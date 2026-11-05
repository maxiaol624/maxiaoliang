package com.maxiaol624.wordtrail.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
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
)

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: WordBook): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWord(word: Word): Long
}

@Database(entities = [WordBook::class, Word::class], version = 1)
abstract class WordDatabase : RoomDatabase() {
    abstract fun words(): WordDao

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
