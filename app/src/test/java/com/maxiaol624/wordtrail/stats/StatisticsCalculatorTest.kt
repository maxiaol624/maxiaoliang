package com.maxiaol624.wordtrail.stats

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsCalculatorTest {
    private val shanghai = ZoneId.of("Asia/Shanghai")
    private val now = local("2026-11-07T12:00:00")
    private val book = StatisticsBook(1, "日常词汇")

    @Test
    fun emptyLibraryStillShowsSevenCalendarDaysAndZeroTotals() {
        val report = calculate()
        assertEquals(0, report.totalWords)
        assertEquals(0, report.totalLearned)
        assertEquals(0, report.todayNew)
        assertEquals(0, report.todayReviews)
        assertEquals(0, report.dueNow)
        assertEquals(0, report.streakDays)
        assertEquals(7, report.week.size)
        assertEquals(LocalDate.of(2026, 11, 1), report.week.first().date)
        assertEquals(LocalDate.of(2026, 11, 7), report.week.last().date)
        assertTrue(report.week.all { it.newWords == 0 && it.reviews == 0 })
        assertTrue(report.books.isEmpty())
    }

    @Test
    fun emptyBooksRemainVisibleAndProgressUsesOnlyTheirOwnWords() {
        val source = StatisticsSource(
            listOf(book, StatisticsBook(2, "空词本"), StatisticsBook(3, "其他词汇")),
            listOf(
                word(1, now - 100, stage = 0, dueAt = now),
                word(2, null),
                word(3, now - 100, bookId = 3, stage = 4, dueAt = now + 1),
            ),
            emptyList(),
        )
        val report = StatisticsCalculator.calculate(source, now, shanghai)
        assertEquals(3, report.totalWords)
        assertEquals(2, report.totalLearned)
        assertEquals(1, report.dueNow)
        assertEquals(BookProgress(1, "日常词汇", 2, 1, 1), report.books[0])
        assertEquals(BookProgress(2, "空词本", 0, 0, 0), report.books[1])
        assertEquals(BookProgress(3, "其他词汇", 1, 1, 0), report.books[2])
    }

    @Test
    fun midnightMovesActivityToPreviousDayWithoutLosingProgress() {
        val learnedAt = local("2026-11-06T23:59:59")
        val source = StatisticsSource(listOf(book), listOf(word(1, learnedAt)), listOf(learnedAt))
        val before = StatisticsCalculator.calculate(source, learnedAt, shanghai)
        val after = StatisticsCalculator.calculate(source, local("2026-11-07T00:00:00"), shanghai)
        assertEquals(1, before.todayNew)
        assertEquals(1, before.todayReviews)
        assertEquals(0, after.todayNew)
        assertEquals(0, after.todayReviews)
        assertEquals(1, after.totalLearned)
        assertEquals(1, after.week[5].newWords)
        assertEquals(1, after.week[5].reviews)
        assertEquals(1, after.streakDays)
    }

    @Test
    fun calendarDaysUseTheSelectedTimeZone() {
        val time = Instant.parse("2026-11-06T18:00:00Z").toEpochMilli()
        val source = StatisticsSource(
            listOf(book),
            listOf(
                word(1, Instant.parse("2026-11-06T12:00:00Z").toEpochMilli()),
                word(2, Instant.parse("2026-11-06T16:30:00Z").toEpochMilli()),
            ),
            emptyList(),
        )
        val localReport = StatisticsCalculator.calculate(source, time, shanghai)
        val utcReport = StatisticsCalculator.calculate(source, time, ZoneId.of("UTC"))
        assertEquals(LocalDate.of(2026, 11, 7), localReport.today)
        assertEquals(1, localReport.todayNew)
        assertEquals(LocalDate.of(2026, 11, 6), utcReport.today)
        assertEquals(2, utcReport.todayNew)
        assertEquals(2, localReport.totalLearned)
        assertEquals(utcReport.totalLearned, localReport.totalLearned)
    }

    @Test
    fun daylightSavingFallbackKeepsBothRepeatedHoursInTheSameDay() {
        val zone = ZoneId.of("America/New_York")
        val source = StatisticsSource(
            emptyList(), emptyList(),
            listOf(
                Instant.parse("2026-11-01T04:10:00Z").toEpochMilli(),
                Instant.parse("2026-11-01T05:30:00Z").toEpochMilli(),
                Instant.parse("2026-11-01T06:30:00Z").toEpochMilli(),
            ),
        )
        val sameDay = StatisticsCalculator.calculate(
            source, Instant.parse("2026-11-02T04:30:00Z").toEpochMilli(), zone,
        )
        val nextDay = StatisticsCalculator.calculate(
            source, Instant.parse("2026-11-02T05:00:00Z").toEpochMilli(), zone,
        )
        assertEquals(LocalDate.of(2026, 11, 1), sameDay.today)
        assertEquals(3, sameDay.todayReviews)
        assertEquals(0, nextDay.todayReviews)
        assertEquals(3, nextDay.week[5].reviews)
    }

    @Test
    fun streakMayEndYesterdayButStopsAtAMissingDay() {
        val recent = listOf(
            local("2026-11-06T12:00:00"),
            local("2026-11-05T12:00:00"),
            local("2026-11-04T12:00:00"),
            local("2026-11-02T12:00:00"),
        )
        assertEquals(3, calculate(reviews = recent).streakDays)
        assertEquals(4, calculate(reviews = recent + now).streakDays)
        assertEquals(1, calculate(reviews = listOf(now, recent[1])).streakDays)
        assertEquals(0, calculate(reviews = listOf(recent[1])).streakDays)
    }

    @Test
    fun repeatedReviewsCountAttemptsWhileNewLearningCountsWords() {
        val report = calculate(
            words = listOf(word(1, now - 1_000)),
            reviews = listOf(now - 500, now - 100),
        )
        assertEquals(1, report.todayNew)
        assertEquals(1, report.totalLearned)
        assertEquals(2, report.todayReviews)
        assertEquals(DailyStudy(report.today, 1, 2), report.week.last())
        assertEquals(1, report.streakDays)
    }

    @Test
    fun futureActivityDoesNotInflateDailyCountsStreakOrProgress() {
        val future = now + 1
        val report = calculate(
            words = listOf(word(1, future, stage = 4, dueAt = now - 100)),
            reviews = listOf(future, local("2026-11-08T12:00:00")),
        )
        assertEquals(1, report.totalWords)
        assertEquals(0, report.totalLearned)
        assertEquals(0, report.todayNew)
        assertEquals(0, report.todayReviews)
        assertEquals(0, report.streakDays)
        assertEquals(0, report.dueNow)
        assertEquals(0, report.books.single().learnedWords)
        assertEquals(0, report.books.single().dueWords)
        assertEquals(StageDistribution(0, 0, 0), report.stages)
    }

    @Test
    fun stageDistributionAndDueBoundaryReflectCurrentSchedules() {
        val learned = (0..5).map { stage ->
            word(stage + 1L, now - 1, stage = stage, dueAt = now + stage - 2)
        }
        val report = calculate(words = learned + word(7, null))
        assertEquals(7, report.totalWords)
        assertEquals(6, report.totalLearned)
        assertEquals(3, report.dueNow)
        assertEquals(StageDistribution(2, 2, 2), report.stages)
    }

    @Test
    fun completedTestsContributeToStreakAndAccuracyWithoutChangingLearningCounts() {
        val source = StatisticsSource(emptyList(), emptyList(), emptyList(), listOf(
            TestScore(now - 1, 4, 4),
            TestScore(local("2026-11-06T12:00:00"), 6, 3),
            TestScore(now + 1, 10, 10),
        ))
        val report = StatisticsCalculator.calculate(source, now, shanghai)
        assertEquals(1, report.todayTests)
        assertEquals(2, report.testSessions)
        assertEquals(10, report.testQuestions)
        assertEquals(7, report.testCorrect)
        assertEquals(2, report.streakDays)
        assertEquals(0, report.totalLearned)
        assertEquals(0, report.todayNew)
        assertEquals(0, report.todayReviews)
        assertEquals(0, report.dueNow)
    }

    private fun calculate(
        words: List<StatisticsWord> = emptyList(),
        reviews: List<Long> = emptyList(),
    ) = StatisticsCalculator.calculate(
        StatisticsSource(if (words.isEmpty()) emptyList() else listOf(book), words, reviews),
        now,
        shanghai,
    )

    private fun word(
        id: Long,
        learnedAt: Long?,
        bookId: Long = 1,
        stage: Int? = learnedAt?.let { 0 },
        dueAt: Long? = learnedAt?.let { now + 600_000 },
    ) = StatisticsWord(id, bookId, learnedAt, stage, dueAt)

    private fun local(text: String) = LocalDateTime.parse(text).atZone(shanghai).toInstant().toEpochMilli()
}
