package com.maxiaol624.wordtrail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxiaol624.wordtrail.data.LearningRating
import com.maxiaol624.wordtrail.data.ScheduledWord
import com.maxiaol624.wordtrail.data.Word
import com.maxiaol624.wordtrail.data.WordBook
import com.maxiaol624.wordtrail.data.WordDatabase
import com.maxiaol624.wordtrail.data.WordCsv
import java.io.IOException
import java.io.InputStreamReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CatalogViewModel(private val application: Application) : AndroidViewModel(application) {
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

    private val transfer = MutableStateFlow<CsvTransfer>(CsvTransfer.Idle)
    val csvTransfer = transfer.asStateFlow()

    fun importCsv(bookId: Long, uri: Uri) = transferCsv(R.string.csv_import_error) {
        val stream = application.contentResolver.openInputStream(uri) ?: throw IOException()
        val parsed = InputStreamReader(stream, Charsets.UTF_8.newDecoder()).buffered().use {
            WordCsv.read(it, bookId)
        }
        val inserted = if (parsed.words.isEmpty()) 0 else dao.insertWords(parsed.words).count { it != -1L }
        application.getString(R.string.csv_import_result, inserted, parsed.words.size - inserted, parsed.invalid)
    }

    fun exportCsv(bookId: Long, uri: Uri) = transferCsv(R.string.csv_export_error) {
        val words = dao.words(bookId).first()
        val stream = application.contentResolver.openOutputStream(uri, "wt") ?: throw IOException()
        stream.bufferedWriter(Charsets.UTF_8).use { WordCsv.write(it, words) }
        application.getString(R.string.csv_export_result, words.size)
    }

    fun acknowledgeCsvResult() {
        if (transfer.value is CsvTransfer.Result) transfer.value = CsvTransfer.Idle
    }

    private fun transferCsv(errorMessage: Int, action: suspend () -> String) {
        if (transfer.value != CsvTransfer.Idle) return
        transfer.value = CsvTransfer.Working
        viewModelScope.launch {
            val message = try {
                withContext(Dispatchers.IO) { action() }
            } catch (error: CancellationException) {
                throw error
            } catch (_: WordCsv.BadHeader) {
                application.getString(R.string.csv_bad_header)
            } catch (error: WordCsv.BadRecord) {
                application.getString(R.string.csv_bad_record, error.number)
            } catch (_: WordCsv.TooLarge) {
                application.getString(R.string.csv_too_large)
            } catch (_: Exception) {
                application.getString(errorMessage)
            }
            transfer.value = CsvTransfer.Result(message)
        }
    }

    sealed interface CsvTransfer {
        object Idle : CsvTransfer
        object Working : CsvTransfer
        data class Result(val message: String) : CsvTransfer
    }

    data class BookContents(val book: WordBook, val words: List<Word>)
}
