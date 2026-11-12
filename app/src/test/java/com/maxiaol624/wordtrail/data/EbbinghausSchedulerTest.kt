package com.maxiaol624.wordtrail.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EbbinghausSchedulerTest {
    private val hour = 3_600_000L
    private val day = 24 * hour

    @Test
    fun curveDecreasesWithTimeAndReachesTargetAtScheduledInterval() {
        val scale = 12.0 * hour
        val interval = EbbinghausScheduler.firstDelay(LearningRating.GOOD)
        assertEquals(1.0, EbbinghausScheduler.retention(0L, scale), 1e-9)
        assertEquals(0.70, EbbinghausScheduler.retention(interval, scale), 1e-8)
        assertTrue(EbbinghausScheduler.retention(interval * 2, scale) < 0.70)
    }

    @Test
    fun firstReviewIntervalsReflectFeedback() {
        val again = EbbinghausScheduler.firstDelay(LearningRating.AGAIN)
        val hard = EbbinghausScheduler.firstDelay(LearningRating.HARD)
        val good = EbbinghausScheduler.firstDelay(LearningRating.GOOD)
        assertTrue(again in 11 * 60_000L..12 * 60_000L)
        assertTrue(hard in 22 * hour..23 * hour)
        assertTrue(good in 68 * hour..69 * hour)
    }

    @Test
    fun rememberingLaterLengthensTheNextInterval() {
        val due = EbbinghausScheduler.nextDelay(day, day, LearningRating.GOOD)
        val late = EbbinghausScheduler.nextDelay(day, 3 * day, LearningRating.GOOD)
        assertEquals(18 * day / 10, due)
        assertTrue(late > due)
    }

    @Test
    fun hardShortensIntervalAndAgainResetsIt() {
        val again = EbbinghausScheduler.firstDelay(LearningRating.AGAIN)
        assertEquals(again, EbbinghausScheduler.nextDelay(day, day, LearningRating.AGAIN))
        assertEquals(7 * day / 10, EbbinghausScheduler.nextDelay(day, day, LearningRating.HARD))
        assertEquals(again, EbbinghausScheduler.nextDelay(again, again, LearningRating.HARD))
    }

    @Test
    fun continuousIntervalsRespectMaximumAndStatisticsBoundaries() {
        assertEquals(30 * day, EbbinghausScheduler.nextDelay(29 * day, 29 * day, LearningRating.GOOD))
        assertEquals(1, EbbinghausScheduler.stageFor(3 * day - 1))
        assertEquals(2, EbbinghausScheduler.stageFor(3 * day))
        assertEquals(3, EbbinghausScheduler.stageFor(14 * day - 1))
        assertEquals(4, EbbinghausScheduler.stageFor(14 * day))
        assertEquals(5, EbbinghausScheduler.stageFor(30 * day))
    }
}
