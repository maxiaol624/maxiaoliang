package com.maxiaol624.wordtrail.widget

import androidx.room.Dao
import androidx.room.Query

data class WidgetSummary(
    val learnedToday: Int,
    val dueCount: Int,
    val hasWords: Boolean,
    val hasNewWords: Boolean,
)

@Dao
interface WidgetDao {
    @Query("""
        SELECT
            (SELECT COUNT(*) FROM first_reviews WHERE reviewedAt BETWEEN :startOfDay AND :now) AS learnedToday,
            (SELECT COUNT(*) FROM review_schedules WHERE dueAt <= :now) AS dueCount,
            EXISTS(SELECT 1 FROM words) AS hasWords,
            EXISTS(
                SELECT 1 FROM words WHERE NOT EXISTS (
                    SELECT 1 FROM first_reviews WHERE wordId = words.id
                )
            ) AS hasNewWords
    """)
    suspend fun summary(startOfDay: Long, now: Long): WidgetSummary

    @Query("""
        SELECT bookId FROM words WHERE NOT EXISTS (
            SELECT 1 FROM first_reviews WHERE wordId = words.id
        ) ORDER BY id LIMIT 1
    """)
    suspend fun firstBookWithNewWords(): Long?
}
