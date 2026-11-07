package com.maxiaol624.wordtrail

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maxiaol624.wordtrail.databinding.ActivityStatisticsBinding
import com.maxiaol624.wordtrail.databinding.StatisticsBookBinding
import com.maxiaol624.wordtrail.databinding.StatisticsDayBinding
import com.maxiaol624.wordtrail.stats.StatisticsReport
import com.maxiaol624.wordtrail.stats.StatisticsState
import com.maxiaol624.wordtrail.stats.StatisticsViewModel
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class StatisticsActivity : AppCompatActivity() {
    private val model by lazy { ViewModelProvider(this)[StatisticsViewModel::class.java] }
    private lateinit var binding: ActivityStatisticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding(binding.root)
        binding.back.setOnClickListener { finish() }
        binding.retry.setOnClickListener { model.retry() }
        binding.createBooks.setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    binding.message.isVisible = state !is StatisticsState.Ready
                    binding.retry.isVisible = state is StatisticsState.Error
                    binding.statisticsScroll.isVisible = state is StatisticsState.Ready
                    when (state) {
                        StatisticsState.Loading -> binding.message.setText(R.string.catalog_loading)
                        StatisticsState.Error -> binding.message.setText(R.string.statistics_error)
                        is StatisticsState.Ready -> render(state.report)
                    }
                }
            }
        }
    }

    private fun render(report: StatisticsReport) {
        binding.dateLabel.text = getString(
            R.string.statistics_date, report.today.format(DateTimeFormatter.ofPattern("M 月 d 日")),
        )
        binding.todayNew.text = getString(R.string.statistics_today_new, report.todayNew)
        binding.todayReviews.text = getString(R.string.statistics_today_reviews, report.todayReviews)
        binding.streakDays.text = getString(R.string.statistics_streak, report.streakDays)
        binding.dueNow.text = getString(R.string.statistics_due, report.dueNow)
        binding.overallSummary.text = progressText(report.totalLearned, report.totalWords)
        binding.overallProgress.max = report.totalWords.coerceAtLeast(1)
        binding.overallProgress.progress = report.totalLearned
        binding.statisticsEmpty.isVisible = report.totalWords == 0
        binding.createBooks.isVisible = report.totalWords == 0
        binding.noBooks.isVisible = report.books.isEmpty()

        val chartMax = report.week.maxOf { maxOf(it.newWords, it.reviews) }.coerceAtLeast(1)
        binding.weekRows.removeAllViews()
        report.week.forEach { day ->
            val row = StatisticsDayBinding.inflate(layoutInflater, binding.weekRows, false)
            row.dayLabel.text = if (day.date == report.today) getString(R.string.statistics_today)
            else day.date.format(DateTimeFormatter.ofPattern("MM/dd"))
            row.dayCounts.text = getString(R.string.statistics_day_counts, day.newWords, day.reviews)
            row.newProgress.max = chartMax
            row.newProgress.progress = day.newWords
            row.reviewProgress.max = chartMax
            row.reviewProgress.progress = day.reviews
            binding.weekRows.addView(row.root)
        }

        binding.stageSummary.text = getString(
            R.string.statistics_stage_summary,
            report.stages.strengthening, report.stages.consolidating, report.stages.longInterval,
        )
        binding.bookRows.removeAllViews()
        report.books.forEach { book ->
            val row = StatisticsBookBinding.inflate(layoutInflater, binding.bookRows, false)
            row.bookName.text = book.name
            row.progressSummary.text = progressText(book.learnedWords, book.totalWords)
            row.bookProgress.max = book.totalWords.coerceAtLeast(1)
            row.bookProgress.progress = book.learnedWords
            row.bookDue.text = getString(R.string.statistics_book_due, book.dueWords)
            row.openBook.contentDescription = getString(R.string.statistics_open_book) + " " + book.name
            row.openBook.setOnClickListener { startActivity(CatalogActivity.bookIntent(this, book.id)) }
            binding.bookRows.addView(row.root)
        }
    }

    private fun progressText(learned: Int, total: Int): String {
        val percent = if (total == 0) 0 else (learned.toLong() * 100 / total).toInt()
        return getString(R.string.statistics_overall, learned, total, percent)
    }
}
