package com.maxiaol624.wordtrail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

    suspend fun addBook(name: String) = dao.insertBook(WordBook(name)) != -1L

    suspend fun addWord(bookId: Long, term: String, meaning: String) =
        dao.insertWord(Word(bookId, term, meaning)) != -1L

    data class BookContents(val book: WordBook, val words: List<Word>)
}
