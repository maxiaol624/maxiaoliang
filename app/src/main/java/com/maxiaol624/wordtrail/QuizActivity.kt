package com.maxiaol624.wordtrail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maxiaol624.wordtrail.databinding.ActivityQuizBinding
import com.maxiaol624.wordtrail.quiz.QuizGenerator
import com.maxiaol624.wordtrail.quiz.QuizPage
import com.maxiaol624.wordtrail.quiz.QuizScreen
import com.maxiaol624.wordtrail.quiz.QuizViewModel
import kotlinx.coroutines.launch

class QuizActivity : AppCompatActivity() {
    private val model by lazy { ViewModelProvider(this)[QuizViewModel::class.java] }
    private lateinit var binding: ActivityQuizBinding
    private var saving = false
    private var lastPageKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookId = intent.getLongExtra("bookId", 0)
        if (bookId <= 0) { finish(); return }
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding(binding.root)
        model.open(bookId, savedInstanceState?.getString("sessionId"), savedInstanceState?.getInt("position") ?: -1)
        binding.back.setOnClickListener { back() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = back()
        })
        binding.retry.setOnClickListener { model.retry() }
        binding.startQuiz.setOnClickListener { model.start() }
        binding.restartQuiz.setOnClickListener { model.start() }
        binding.backLobby.setOnClickListener { model.showLobby() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { model.state.collect(::render) }
        }
    }

    private fun back() {
        if (saving) return
        if (model.route.value.sessionId == null) finish() else model.showLobby()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("sessionId", model.route.value.sessionId)
        outState.putInt("position", model.route.value.position)
        super.onSaveInstanceState(outState)
    }

    private fun render(screen: QuizScreen) {
        saving = screen.busy
        val page = screen.page
        binding.lobby.isVisible = page is QuizPage.Lobby
        binding.questionCard.isVisible = page is QuizPage.Question
        binding.result.isVisible = page is QuizPage.Result
        binding.retry.isVisible = page is QuizPage.Error
        binding.message.isVisible = page is QuizPage.Loading || page is QuizPage.Error || screen.saveFailed || screen.busy
        binding.message.setText(when {
            screen.saveFailed -> R.string.quiz_save_failed
            screen.busy -> R.string.quiz_saving
            page is QuizPage.Error -> R.string.quiz_read_failed
            else -> R.string.catalog_loading
        })
        when (page) {
            is QuizPage.Lobby -> {
                binding.bookName.text = page.data.name ?: getString(R.string.book_missing)
                val canStart = page.data.name != null && (page.data.meaningCount >= 4 || page.data.activeId != null)
                binding.insufficient.isVisible = page.data.name != null && !canStart
                binding.insufficient.text = getString(R.string.quiz_insufficient, page.data.meaningCount)
                binding.startQuiz.isEnabled = canStart && !screen.busy
                binding.startQuiz.setText(if (page.data.activeId == null) R.string.quiz_start else R.string.quiz_resume)
                binding.noHistory.isVisible = page.data.history.isEmpty()
                binding.historyRows.removeAllViews()
                page.data.history.forEach { history ->
                    val button = Button(this).apply {
                        isAllCaps = false
                        text = getString(R.string.quiz_history_row, time(history.completedAt),
                            QuizGenerator.percentage(history.correct, history.total), history.correct, history.total)
                        isEnabled = !screen.busy
                        setOnClickListener { model.showAttempt(history.id) }
                    }
                    binding.historyRows.addView(button, LinearLayout.LayoutParams(-1, -2))
                }
                pageKey("lobby")
            }
            is QuizPage.Question -> {
                val question = page.attempt.questions[page.position]
                binding.questionProgress.text = getString(R.string.quiz_progress, page.position + 1, page.attempt.questions.size)
                binding.quizTerm.text = question.term
                listOf(binding.optionA, binding.optionB, binding.optionC, binding.optionD).forEachIndexed { index, button ->
                    button.text = getString(R.string.quiz_option, ('A' + index).toString(), question.options()[index])
                    button.isEnabled = question.selectedIndex == null && !screen.busy
                    button.setOnClickListener { model.answer(page.position, index) }
                }
                val choice = question.selectedIndex
                binding.answerFeedback.isVisible = choice != null
                binding.answerFeedback.text = when {
                    choice == null -> ""
                    question.isCorrect() -> getString(R.string.quiz_correct, question.options()[question.correctIndex])
                    else -> getString(R.string.quiz_wrong, question.options()[choice], question.options()[question.correctIndex])
                }
                binding.nextQuestion.isVisible = choice != null
                binding.nextQuestion.isEnabled = !screen.busy
                binding.nextQuestion.setText(if (page.position == page.attempt.questions.lastIndex) R.string.quiz_finish else R.string.quiz_next)
                binding.nextQuestion.setOnClickListener { model.next(page.position, page.attempt.questions.size) }
                pageKey("${question.sessionId}:${question.position}")
            }
            is QuizPage.Result -> {
                val questions = page.attempt.questions
                val correct = questions.count { it.isCorrect() }
                binding.score.text = getString(R.string.quiz_score, QuizGenerator.percentage(correct, questions.size), correct, questions.size)
                binding.completedAt.text = page.attempt.session.completedAt?.let(::time).orEmpty()
                binding.restartQuiz.isEnabled = !screen.busy
                binding.backLobby.isEnabled = !screen.busy
                val wrong = questions.filterNot { it.isCorrect() }
                binding.allCorrect.isVisible = wrong.isEmpty()
                binding.wrongRows.removeAllViews()
                wrong.forEach { question ->
                    val row = TextView(this).apply {
                        setTextAppearance(R.style.StatisticsText)
                        setTextColor(getColor(R.color.ink))
                        val padding = resources.getDimensionPixelSize(R.dimen.catalog_padding)
                        setPadding(padding, padding, padding, padding)
                        setBackgroundResource(R.drawable.card_background)
                        text = getString(R.string.quiz_wrong_row, question.term,
                            question.options()[requireNotNull(question.selectedIndex)], question.options()[question.correctIndex])
                    }
                    binding.wrongRows.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                        topMargin = resources.getDimensionPixelSize(R.dimen.catalog_padding) / 2
                    })
                }
                pageKey("result:${page.attempt.session.id}")
            }
            else -> Unit
        }
    }

    private fun pageKey(key: String) {
        if (lastPageKey != null && lastPageKey != key) binding.quizScroll.scrollTo(0, 0)
        lastPageKey = key
    }

    private fun time(value: Long) = DateUtils.formatDateTime(this, value,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR)

    companion object {
        fun intent(context: Context, bookId: Long) = Intent(context, QuizActivity::class.java).putExtra("bookId", bookId)
    }
}
