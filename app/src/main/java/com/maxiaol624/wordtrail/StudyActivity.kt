package com.maxiaol624.wordtrail

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maxiaol624.wordtrail.data.Word
import com.maxiaol624.wordtrail.databinding.ActivityStudyBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class StudyActivity : AppCompatActivity() {
    private val model by lazy { ViewModelProvider(this)[CatalogViewModel::class.java] }
    private lateinit var binding: ActivityStudyBinding
    private var word: Word? = null
    private var revealedId = 0L
    private var savingId = 0L

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
        binding.back.setOnClickListener { finish() }
        binding.reveal.setOnClickListener {
            val current = word ?: return@setOnClickListener
            revealedId = current.id
            render(current)
        }
        binding.again.setOnClickListener { rate(LearningRating.AGAIN) }
        binding.hard.setOnClickListener { rate(LearningRating.HARD) }
        binding.good.setOnClickListener { rate(LearningRating.GOOD) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.nextNewWord(bookId)
                    .catch { showMessage(R.string.catalog_read_failed) }
                    .collect { next ->
                        word = next
                        if (savingId != next?.id) savingId = 0L
                        if (next == null) showMessage(R.string.study_done) else render(next)
                    }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(REVEALED_ID, revealedId)
        super.onSaveInstanceState(outState)
    }

    private fun render(current: Word) {
        val revealed = current.id == revealedId
        binding.message.isVisible = false
        binding.card.isVisible = true
        binding.term.text = current.term
        binding.meaning.text = if (revealed) current.meaning else ""
        binding.meaning.isVisible = revealed
        binding.reveal.isVisible = !revealed
        binding.ratings.isVisible = revealed
        val enabled = savingId != current.id
        binding.again.isEnabled = enabled
        binding.hard.isEnabled = enabled
        binding.good.isEnabled = enabled
    }

    private fun showMessage(text: Int) {
        binding.card.isVisible = false
        binding.message.isVisible = true
        binding.message.setText(text)
    }

    private fun rate(rating: LearningRating) {
        val current = word ?: return
        if (current.id != revealedId || savingId == current.id) return
        savingId = current.id
        render(current)
        lifecycleScope.launch {
            try {
                model.recordFirstReview(current.id, rating)
            } catch (_: SQLiteException) {
                if (savingId == current.id) savingId = 0L
                if (word?.id == current.id) render(current)
                Toast.makeText(this@StudyActivity, R.string.study_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val EXTRA_BOOK_ID = "studyBookId"
        private const val REVEALED_ID = "revealedWordId"

        fun intent(context: Context, bookId: Long) =
            Intent(context, StudyActivity::class.java).putExtra(EXTRA_BOOK_ID, bookId)
    }
}
