package com.maxiaol624.wordtrail

import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maxiaol624.wordtrail.databinding.ActivityCatalogBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CatalogActivity : AppCompatActivity() {
    private val model by lazy { ViewModelProvider(this)[CatalogViewModel::class.java] }
    private lateinit var binding: ActivityCatalogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding(binding.root)
        binding.back.setOnClickListener { finish() }

        val wordId = intent.getLongExtra(EXTRA_WORD_ID, 0)
        val bookId = intent.getLongExtra(EXTRA_BOOK_ID, 0)
        when {
            wordId > 0 -> showWord(wordId)
            bookId > 0 -> showBook(bookId)
            else -> showBooks()
        }
    }

    private fun showBooks() {
        binding.pageTitle.setText(R.string.books_title)
        binding.add.setText(R.string.add_book)
        binding.add.setOnClickListener {
            showEditor(
                R.string.add_book, R.string.book_name, R.string.book_exists,
                save = { name, _ -> model.addBook(name) },
            )
        }
        observe(model.books) { books ->
            binding.add.isVisible = true
            renderList(books, R.string.no_books, { it.name }) { book ->
                startActivity(Intent(this, CatalogActivity::class.java).putExtra(EXTRA_BOOK_ID, book.id))
            }
        }
    }

    private fun showBook(bookId: Long) {
        binding.pageTitle.setText(R.string.words_title)
        binding.add.setText(R.string.add_word)
        binding.study.setOnClickListener {
            startActivity(StudyActivity.intent(this, bookId))
        }
        binding.review.setOnClickListener {
            startActivity(StudyActivity.intent(this, bookId, review = true))
        }
        binding.add.setOnClickListener {
            showEditor(
                R.string.add_word, R.string.word_term, R.string.word_exists, hasMeaning = true,
                save = { term, meaning -> model.addWord(bookId, term, meaning) },
            )
        }
        observe(model.bookContents(bookId)) { contents ->
            if (contents == null) {
                showMessage(R.string.book_missing)
            } else {
                binding.pageTitle.text = contents.book.name
                binding.add.isVisible = true
                binding.study.isVisible = contents.words.isNotEmpty()
                binding.review.isVisible = contents.words.isNotEmpty()
                renderList(contents.words, R.string.no_words, { it.term }) { word ->
                    startActivity(Intent(this, CatalogActivity::class.java).putExtra(EXTRA_WORD_ID, word.id))
                }
            }
        }
    }

    private fun showWord(wordId: Long) {
        binding.pageTitle.setText(R.string.word_detail)
        observe(model.word(wordId)) { word ->
            if (word == null) {
                showMessage(R.string.word_missing)
            } else {
                binding.message.isVisible = false
                binding.detail.isVisible = true
                binding.term.text = word.term
                binding.meaning.text = word.meaning
            }
        }
    }

    private fun <T> observe(flow: Flow<T>, render: (T) -> Unit) {
        showMessage(R.string.catalog_loading)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.catch { showMessage(R.string.catalog_read_failed) }.collect { render(it) }
            }
        }
    }

    private fun <T> renderList(items: List<T>, empty: Int, label: (T) -> String, open: (T) -> Unit) {
        binding.list.adapter = ArrayAdapter(this, R.layout.catalog_row, items.map(label))
        binding.list.setOnItemClickListener { _, _, position, _ -> open(items[position]) }
        binding.list.isVisible = items.isNotEmpty()
        binding.message.isVisible = items.isEmpty()
        binding.message.setText(empty)
    }

    private fun showMessage(text: Int) {
        binding.add.isVisible = false
        binding.study.isVisible = false
        binding.review.isVisible = false
        binding.list.isVisible = false
        binding.detail.isVisible = false
        binding.message.isVisible = true
        binding.message.setText(text)
    }

    private fun showEditor(
        title: Int,
        hint: Int,
        duplicate: Int,
        hasMeaning: Boolean = false,
        save: suspend (String, String) -> Boolean,
    ) {
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.catalog_padding)
            setPadding(padding, 0, padding, 0)
        }
        val nameField = EditText(this).apply {
            setHint(hint)
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(80))
        }
        fields.addView(nameField)
        val meaningField = if (hasMeaning) EditText(this).apply {
            setHint(R.string.word_meaning)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            filters = arrayOf(InputFilter.LengthFilter(500))
            fields.addView(this)
        } else null

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(fields)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .show()
        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.setOnClickListener {
            val name = nameField.text.toString().trim()
            val meaning = meaningField?.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                nameField.error = getString(R.string.required_field)
                return@setOnClickListener
            }
            if (meaningField != null && meaning.isEmpty()) {
                meaningField.error = getString(R.string.required_field)
                return@setOnClickListener
            }
            button.isEnabled = false
            dialog.setCancelable(false)
            val cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            cancel.isEnabled = false
            lifecycleScope.launch {
                try {
                    if (save(name, meaning)) dialog.dismiss()
                    else nameField.error = getString(duplicate)
                } catch (_: SQLiteException) {
                    nameField.error = getString(R.string.catalog_save_failed)
                } finally {
                    if (dialog.isShowing) {
                        button.isEnabled = true
                        cancel.isEnabled = true
                        dialog.setCancelable(true)
                    }
                }
            }
        }
    }

    private companion object {
        const val EXTRA_BOOK_ID = "bookId"
        const val EXTRA_WORD_ID = "wordId"
    }
}
