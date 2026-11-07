package com.maxiaol624.wordtrail.stats

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object StatisticsCalculator {
    fun calculate(source: StatisticsSource, now: Long, zone: ZoneId): StatisticsReport {
        fun day(time: Long): LocalDate = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()

        val today = day(now)
        // 修改系统时间后可能存在未来记录，待实际时间到达再计入统计。
        val learned = source.words.filter { it.firstReviewedAt?.let { time -> time <= now } == true }
        val newByDay = learned.groupingBy { day(requireNotNull(it.firstReviewedAt)) }.eachCount()
        val reviewByDay = source.reviewTimes.asSequence().filter { it <= now }
            .groupingBy(::day).eachCount()
        val activeDays = newByDay.keys + reviewByDay.keys
        var cursor = if (today in activeDays) today else today.minusDays(1)
        var streak = 0
        while (cursor in activeDays) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val due = learned.filter { it.dueAt?.let { time -> time <= now } == true }
        val wordsByBook = source.words.groupingBy { it.bookId }.eachCount()
        val learnedByBook = learned.groupingBy { it.bookId }.eachCount()
        val dueByBook = due.groupingBy { it.bookId }.eachCount()

        return StatisticsReport(
            today = today,
            todayNew = newByDay[today] ?: 0,
            todayReviews = reviewByDay[today] ?: 0,
            totalWords = source.words.size,
            totalLearned = learned.size,
            dueNow = due.size,
            streakDays = streak,
            week = (6L downTo 0L).map { daysAgo ->
                val date = today.minusDays(daysAgo)
                DailyStudy(date, newByDay[date] ?: 0, reviewByDay[date] ?: 0)
            },
            books = source.books.map { book ->
                BookProgress(
                    book.id, book.name,
                    wordsByBook[book.id] ?: 0,
                    learnedByBook[book.id] ?: 0,
                    dueByBook[book.id] ?: 0,
                )
            },
            stages = StageDistribution(
                strengthening = learned.count { it.stage in 0..1 },
                consolidating = learned.count { it.stage in 2..3 },
                longInterval = learned.count { it.stage in 4..5 },
            ),
        )
    }
}
