package com.maxiaol624.wordtrail.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.maxiaol624.wordtrail.stats.StatisticsDao
import com.maxiaol624.wordtrail.quiz.QuizDao
import com.maxiaol624.wordtrail.quiz.QuizSession
import com.maxiaol624.wordtrail.quiz.QuizQuestion
import com.maxiaol624.wordtrail.widget.WidgetDao
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "word_books", indices = [Index(value = ["name"], unique = true)])
data class WordBook(
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

@Entity(
    tableName = "words",
    foreignKeys = [ForeignKey(
        entity = WordBook::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["bookId", "term"], unique = true)],
)
data class Word(
    val bookId: Long,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val term: String,
    val meaning: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(defaultValue = "''") val phonetic: String = "",
    @ColumnInfo(defaultValue = "''") val example: String = "",
    @ColumnInfo(defaultValue = "''") val exampleTranslation: String = "",
)

@Entity(
    tableName = "first_reviews",
    foreignKeys = [ForeignKey(
        entity = Word::class,
        parentColumns = ["id"],
        childColumns = ["wordId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class FirstReview(
    @PrimaryKey val wordId: Long,
    val rating: String,
    val reviewedAt: Long,
)

@Entity(
    tableName = "review_schedules",
    foreignKeys = [ForeignKey(
        entity = Word::class, parentColumns = ["id"], childColumns = ["wordId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class ReviewSchedule(@PrimaryKey val wordId: Long, val stage: Int, val dueAt: Long)

@Entity(
    tableName = "review_events",
    foreignKeys = [ForeignKey(
        entity = Word::class, parentColumns = ["id"], childColumns = ["wordId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("wordId")],
)
data class ReviewEvent(
    val wordId: Long,
    val rating: String,
    val reviewedAt: Long,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

data class ScheduledWord(@Embedded val word: Word, val dueAt: Long, val lastReviewedAt: Long)
data class DueReviewBook(val id: Long, val name: String, val dueCount: Int)

@Dao
interface WordDao {
    @Query("SELECT * FROM word_books ORDER BY name, id")
    fun books(): Flow<List<WordBook>>

    @Query("SELECT * FROM word_books WHERE id = :id")
    fun book(id: Long): Flow<WordBook?>

    @Query("SELECT * FROM words WHERE bookId = :bookId ORDER BY term, id")
    fun words(bookId: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE id = :id")
    fun word(id: Long): Flow<Word?>

    @Query("""
        SELECT words.* FROM words
        WHERE bookId = :bookId
          AND NOT EXISTS (SELECT 1 FROM first_reviews WHERE wordId = words.id)
        ORDER BY id
        LIMIT 1
    """)
    fun nextNewWord(bookId: Long): Flow<Word?>

    @Query("""
        SELECT words.* FROM words
        INNER JOIN first_reviews ON first_reviews.wordId = words.id
        WHERE words.bookId = :bookId AND words.id > :afterId
        ORDER BY words.id
        LIMIT 1
    """)
    fun nextPracticeWord(bookId: Long, afterId: Long): Flow<Word?>

    @Query("""
        SELECT words.*, review_schedules.dueAt,
            COALESCE(
                (SELECT reviewedAt FROM review_events WHERE wordId = words.id ORDER BY id DESC LIMIT 1),
                first_reviews.reviewedAt
            ) AS lastReviewedAt
        FROM words JOIN review_schedules ON review_schedules.wordId = words.id
        JOIN first_reviews ON first_reviews.wordId = words.id
        WHERE words.bookId = :bookId
        ORDER BY review_schedules.dueAt, words.id
        LIMIT 1
    """)
    fun nextScheduledWord(bookId: Long): Flow<ScheduledWord?>

    @Query("""
        SELECT b.id, b.name, COUNT(*) AS dueCount
        FROM review_schedules s JOIN words w ON w.id = s.wordId
        JOIN word_books b ON b.id = w.bookId
        WHERE s.dueAt <= :now
        GROUP BY b.id ORDER BY MIN(s.dueAt), b.id LIMIT 1
    """)
    suspend fun firstDueReviewBook(now: Long): DueReviewBook?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFirstReview(review: FirstReview): Long

    @Insert
    suspend fun insertSchedule(schedule: ReviewSchedule)

    @Insert
    suspend fun insertReviewEvent(event: ReviewEvent)

    @Query("""
        UPDATE review_schedules SET stage = :stage, dueAt = :dueAt
        WHERE wordId = :wordId AND dueAt = :expectedDueAt
    """)
    suspend fun updateSchedule(wordId: Long, expectedDueAt: Long, stage: Int, dueAt: Long): Int

    @Transaction
    suspend fun recordFirstReview(wordId: Long, rating: LearningRating, now: Long) {
        if (insertFirstReview(FirstReview(wordId, rating.name, now)) == -1L) return
        val delay = EbbinghausScheduler.firstDelay(rating)
        insertSchedule(ReviewSchedule(wordId, EbbinghausScheduler.stageFor(delay), now + delay))
    }

    @Transaction
    suspend fun recordScheduledReview(current: ScheduledWord, rating: LearningRating, now: Long) {
        if (current.dueAt > now) return
        val delay = EbbinghausScheduler.nextDelay(
            previousDelay = current.dueAt - current.lastReviewedAt,
            elapsedMillis = now - current.lastReviewedAt,
            rating = rating,
        )
        if (updateSchedule(current.word.id, current.dueAt, EbbinghausScheduler.stageFor(delay), now + delay) == 1) {
            insertReviewEvent(ReviewEvent(current.word.id, rating.name, now))
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: WordBook): Long

    @Query("UPDATE word_books SET name = :name WHERE id = :id")
    suspend fun renameBook(id: Long, name: String): Int

    @Query("DELETE FROM word_books WHERE id = :id")
    suspend fun deleteBook(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWord(word: Word): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<Word>): List<Long>

    @Update
    suspend fun updateWord(word: Word): Int

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteWord(id: Long): Int
}

@Database(
    entities = [WordBook::class, Word::class, FirstReview::class, ReviewSchedule::class, ReviewEvent::class,
        QuizSession::class, QuizQuestion::class],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = SeedReviewSchedules::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class WordDatabase : RoomDatabase() {
    abstract fun words(): WordDao
    abstract fun statistics(): StatisticsDao
    abstract fun quizzes(): QuizDao
    abstract fun widget(): WidgetDao

    companion object {
        @Volatile
        private var instance: WordDatabase? = null

        fun get(context: Context): WordDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WordDatabase::class.java,
                "wordtrail.db",
            ).build().also { instance = it }
        }
    }
}
