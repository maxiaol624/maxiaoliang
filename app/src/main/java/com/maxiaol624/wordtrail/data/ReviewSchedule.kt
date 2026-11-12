package com.maxiaol624.wordtrail.data

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

enum class LearningRating { AGAIN, HARD, GOOD }

internal object EbbinghausScheduler {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR
    private const val CURVE_K = 1.84
    private const val CURVE_EXPONENT = 1.25
    private const val TARGET_RETENTION = 0.70
    private const val RELEARN_SCALE = 2.0 * MINUTE
    private const val MAX_DELAY = 30 * DAY

    // Solve retention(t, scale) = TARGET_RETENTION for t / scale.
    private val targetTimeRatio =
        10.0.pow((CURVE_K * (1.0 / TARGET_RETENTION - 1.0)).pow(1.0 / CURVE_EXPONENT)) - 1.0

    fun retention(elapsedMillis: Long, scaleMillis: Double): Double {
        val logTime = log10(1.0 + elapsedMillis.coerceAtLeast(0L) / scaleMillis)
        return CURVE_K / (logTime.pow(CURVE_EXPONENT) + CURVE_K)
    }

    fun firstDelay(rating: LearningRating): Long = delayForScale(when (rating) {
        LearningRating.AGAIN -> RELEARN_SCALE
        LearningRating.HARD -> 4.0 * HOUR
        LearningRating.GOOD -> 12.0 * HOUR
    })

    fun nextDelay(previousDelay: Long, elapsedMillis: Long, rating: LearningRating): Long {
        val previousScale = previousDelay.coerceIn(1L, MAX_DELAY) / targetTimeRatio
        val newScale = when (rating) {
            LearningRating.AGAIN -> RELEARN_SCALE
            LearningRating.HARD -> previousScale * 0.70
            LearningRating.GOOD -> previousScale * (2.5 - retention(elapsedMillis, previousScale))
        }
        return delayForScale(newScale)
    }

    // Keep the existing statistics buckets for old and new schedules.
    fun stageFor(delay: Long): Int = when {
        delay < DAY -> 0
        delay < 3 * DAY -> 1
        delay < 7 * DAY -> 2
        delay < 14 * DAY -> 3
        delay < MAX_DELAY -> 4
        else -> 5
    }

    private fun delayForScale(scale: Double): Long =
        (scale.coerceAtLeast(RELEARN_SCALE) * targetTimeRatio).roundToLong().coerceAtMost(MAX_DELAY)
}

class SeedReviewSchedules : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO review_schedules (wordId, stage, dueAt)
            SELECT wordId, stage, reviewedAt + CASE stage WHEN 1 THEN ? WHEN 2 THEN ? ELSE ? END
            FROM (
                SELECT wordId, reviewedAt,
                       CASE rating WHEN 'HARD' THEN 1 WHEN 'GOOD' THEN 2 ELSE 0 END AS stage
                FROM first_reviews
            )
            """.trimIndent(),
            // Keep the original intervals when migrating existing learning records.
            arrayOf(86_400_000L, 259_200_000L, 600_000L),
        )
    }
}
