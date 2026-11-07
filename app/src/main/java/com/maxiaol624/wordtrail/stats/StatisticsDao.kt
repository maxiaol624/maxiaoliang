package com.maxiaol624.wordtrail.stats

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface StatisticsDao {
    @Query("SELECT id, name FROM word_books ORDER BY name, id")
    suspend fun books(): List<StatisticsBook>

    @Query("""
        SELECT words.id AS wordId, words.bookId,
            first_reviews.reviewedAt AS firstReviewedAt,
            review_schedules.stage, review_schedules.dueAt
        FROM words
        LEFT JOIN first_reviews ON first_reviews.wordId = words.id
        LEFT JOIN review_schedules ON review_schedules.wordId = words.id
    """)
    suspend fun words(): List<StatisticsWord>

    @Query("SELECT reviewedAt FROM review_events")
    suspend fun reviewTimes(): List<Long>

    // 一次读取同一份数据库快照，避免学习保存时三个查询读到不同状态。
    @Transaction
    suspend fun snapshot() = StatisticsSource(books(), words(), reviewTimes())
}
