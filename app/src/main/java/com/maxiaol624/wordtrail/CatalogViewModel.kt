package com.maxiaol624.wordtrail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.maxiaol624.wordtrail.data.LearningRating
import com.maxiaol624.wordtrail.data.ScheduledWord
import com.maxiaol624.wordtrail.data.Word
import com.maxiaol624.wordtrail.data.WordBook
import com.maxiaol624.wordtrail.data.WordDatabase
import kotlinx.coroutines.flow.combine

class CatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = WordDatabase.get(application).words()

    val books = dao.books()

    fun bookContents(id: Long) = dao.book(id).combine(dao.words(id)) { book, words ->
        book?.let { BookContents(it, words) }
    }

    fun word(id: Long) = dao.word(id)

    fun nextNewWord(bookId: Long) = dao.nextNewWord(bookId)

    fun nextPracticeWord(bookId: Long, afterId: Long) = dao.nextPracticeWord(bookId, afterId)

    suspend fun recordFirstReview(wordId: Long, rating: LearningRating) {
        dao.recordFirstReview(wordId, rating, System.currentTimeMillis())
    }

    fun nextScheduledWord(bookId: Long) = dao.nextScheduledWord(bookId)

    suspend fun recordScheduledReview(current: ScheduledWord, rating: LearningRating) =
        dao.recordScheduledReview(current, rating, System.currentTimeMillis())

    suspend fun addBook(name: String) = dao.insertBook(WordBook(name)) != -1L

    suspend fun addWord(bookId: Long, term: String, meaning: String) =
        dao.insertWord(Word(bookId, term, meaning)) != -1L

    data class BookContents(val book: WordBook, val words: List<Word>)
}
