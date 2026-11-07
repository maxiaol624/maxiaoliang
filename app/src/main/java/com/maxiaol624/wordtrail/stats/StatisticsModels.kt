package com.maxiaol624.wordtrail.stats

import java.time.LocalDate

data class StatisticsBook(val id: Long, val name: String)

data class StatisticsWord(
    val wordId: Long,
    val bookId: Long,
    val firstReviewedAt: Long?,
    val stage: Int?,
    val dueAt: Long?,
)

data class StatisticsSource(
    val books: List<StatisticsBook>,
    val words: List<StatisticsWord>,
    val reviewTimes: List<Long>,
)

data class DailyStudy(val date: LocalDate, val newWords: Int, val reviews: Int)

data class BookProgress(
    val id: Long,
    val name: String,
    val totalWords: Int,
    val learnedWords: Int,
    val dueWords: Int,
)

data class StageDistribution(
    val strengthening: Int,
    val consolidating: Int,
    val longInterval: Int,
)

data class StatisticsReport(
    val today: LocalDate,
    val todayNew: Int,
    val todayReviews: Int,
    val totalWords: Int,
    val totalLearned: Int,
    val dueNow: Int,
    val streakDays: Int,
    val week: List<DailyStudy>,
    val books: List<BookProgress>,
    val stages: StageDistribution,
)

sealed interface StatisticsState {
    object Loading : StatisticsState
    object Error : StatisticsState
    data class Ready(val report: StatisticsReport) : StatisticsState
}
