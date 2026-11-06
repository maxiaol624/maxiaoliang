package com.maxiaol624.wordtrail.data

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

enum class LearningRating { AGAIN, HARD, GOOD }

internal object ReviewIntervals {
    private const val DAY = 86_400_000L
    private val intervals = longArrayOf(600_000L, DAY, 3 * DAY, 7 * DAY, 14 * DAY, 30 * DAY)

    fun firstStage(rating: LearningRating) = when (rating) {
        LearningRating.AGAIN -> 0
        LearningRating.HARD -> 1
        LearningRating.GOOD -> 2
    }

    fun nextStage(previous: Int, rating: LearningRating) = when (rating) {
        LearningRating.AGAIN -> 0
        LearningRating.HARD -> (previous - 1).coerceAtLeast(1)
        LearningRating.GOOD -> (previous + 1).coerceAtMost(intervals.lastIndex)
    }

    fun delayFor(stage: Int) = intervals[stage]
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
            arrayOf(ReviewIntervals.delayFor(1), ReviewIntervals.delayFor(2), ReviewIntervals.delayFor(0)),
        )
    }
}
