package com.maxiaol624.wordtrail

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.widget.Toast
import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maxiaol624.wordtrail.data.Word
import com.maxiaol624.wordtrail.data.LearningRating
import com.maxiaol624.wordtrail.data.ScheduledWord
import com.maxiaol624.wordtrail.databinding.ActivityStudyBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StudyActivity : AppCompatActivity() {
    private val model by lazy { ViewModelProvider(this)[CatalogViewModel::class.java] }
    private lateinit var binding: ActivityStudyBinding
    private var word: Word? = null
    private var revealedId = 0L
    private var savingId = 0L
    private var scheduled: ScheduledWord? = null
    private val isReview by lazy { intent.getBooleanExtra(EXTRA_REVIEW, false) }
    private val practiceAfterId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookId = intent.getLongExtra(EXTRA_BOOK_ID, 0)
        if (bookId <= 0) {
            finish()
            return
        }
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding(binding.root)
        revealedId = savedInstanceState?.getLong(REVEALED_ID) ?: 0L
        if (savedInstanceState?.containsKey(PRACTICE_AFTER_ID) == true) {
            practiceAfterId.value = savedInstanceState.getLong(PRACTICE_AFTER_ID)
        }
        binding.back.setOnClickListener { finish() }
        binding.reveal.setOnClickListener {
            val current = word ?: return@setOnClickListener
            revealedId = current.id
            render(current)
        }
        binding.again.setOnClickListener { rate(LearningRating.AGAIN) }
        binding.hard.setOnClickListener { rate(LearningRating.HARD) }
        binding.good.setOnClickListener { rate(LearningRating.GOOD) }
        binding.practiceAgain.setOnClickListener {
            revealedId = 0L
            practiceAfterId.value = 0L
        }
        binding.practiceNext.setOnClickListener {
            val current = word ?: return@setOnClickListener
            if (current.id != revealedId) return@setOnClickListener
            revealedId = 0L
            practiceAfterId.value = current.id
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (isReview) observeReview(bookId) else observeStudy(bookId)
            }
        }
    }

    private suspend fun observeStudy(bookId: Long) {
        practiceAfterId.collectLatest { afterId ->
            val practice = afterId != null
            binding.pageTitle.setText(if (practice) R.string.practice_title else R.string.study_title)
            val queue = if (afterId == null) model.nextNewWord(bookId)
            else model.nextPracticeWord(bookId, afterId)
            queue.catch { showMessage(R.string.catalog_read_failed) }.collect { next ->
                word = next
                if (savingId != next?.id) savingId = 0L
                when {
                    next != null -> render(next)
                    afterId == null -> showMessage(R.string.study_done, canRepeat = true)
                    afterId == 0L -> showMessage(R.string.practice_empty)
                    else -> showMessage(R.string.practice_done, canRepeat = true)
                }
            }
        }
    }

    private suspend fun observeReview(bookId: Long) {
        binding.pageTitle.setText(R.string.review_title)
        val clock = flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(1_000)
            }
        }
        model.nextScheduledWord(bookId)
            .combine(clock) { next, now -> next to (next != null && next.dueAt <= now) }
            .distinctUntilChanged()
            .catch { showMessage(R.string.catalog_read_failed) }
            .collect { (next, due) ->
                val current = next?.takeIf { due }
                if (scheduled != current) {
                    savingId = 0L
                    if (scheduled != null) revealedId = 0L
                }
                scheduled = current
                word = current?.word
                when {
                    current != null -> render(current.word)
                    next == null -> showMessage(R.string.review_empty)
                    else -> {
                        val time = DateUtils.formatDateTime(
                            this, next.dueAt,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR,
                        )
                        showMessage(getString(R.string.review_waiting, time))
                    }
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(REVEALED_ID, revealedId)
        practiceAfterId.value?.let { outState.putLong(PRACTICE_AFTER_ID, it) }
        super.onSaveInstanceState(outState)
    }

    private fun render(current: Word) {
        val revealed = current.id == revealedId
        val practice = practiceAfterId.value != null
        binding.message.isVisible = false
        binding.practiceAgain.isVisible = false
        binding.card.isVisible = true
        binding.term.text = current.term
        binding.meaning.text = if (revealed) current.meaning else ""
        binding.meaning.isVisible = revealed
        binding.reveal.isVisible = !revealed
        binding.ratings.isVisible = revealed && !practice
        binding.practiceNext.isVisible = revealed && practice
        val enabled = savingId != current.id
        binding.again.isEnabled = enabled
        binding.hard.isEnabled = enabled
        binding.good.isEnabled = enabled
    }

    private fun showMessage(text: Int, canRepeat: Boolean = false) =
        showMessage(getString(text), canRepeat)

    private fun showMessage(text: String, canRepeat: Boolean = false) {
        binding.card.isVisible = false
        binding.message.isVisible = true
        binding.message.text = text
        binding.practiceAgain.isVisible = canRepeat
    }

    private fun rate(rating: LearningRating) {
        val current = word ?: return
        if (current.id != revealedId || savingId == current.id) return
        val currentSchedule = scheduled
        savingId = current.id
        render(current)
        lifecycleScope.launch {
            try {
                if (currentSchedule == null) model.recordFirstReview(current.id, rating)
                else model.recordScheduledReview(currentSchedule, rating)
            } catch (_: SQLiteException) {
                if (savingId == current.id) savingId = 0L
                if (word?.id == current.id) render(current)
                Toast.makeText(this@StudyActivity, R.string.study_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val EXTRA_BOOK_ID = "studyBookId"
        private const val EXTRA_REVIEW = "scheduledReview"
        private const val REVEALED_ID = "revealedWordId"
        private const val PRACTICE_AFTER_ID = "practiceAfterId"

        fun intent(context: Context, bookId: Long, review: Boolean = false) =
            Intent(context, StudyActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, bookId)
                .putExtra(EXTRA_REVIEW, review)
    }
}
