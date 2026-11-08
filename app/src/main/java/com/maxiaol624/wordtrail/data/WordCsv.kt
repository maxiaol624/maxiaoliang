package com.maxiaol624.wordtrail.data

import java.io.Reader
import java.io.Writer
import java.util.Locale

internal object WordCsv {
    private val columns = listOf("word", "meaning", "phonetic", "example", "exampleTranslation")
    private const val MaxCharacters = 4 * 1024 * 1024
    private const val MaxRecords = 20_000

    data class Import(val words: List<Word>, val invalid: Int)
    class BadHeader : Exception()
    class BadRecord(val number: Int) : Exception()
    class TooLarge : Exception()

    fun read(reader: Reader, bookId: Long): Import {
        val source = StringBuilder()
        val buffer = CharArray(8192)
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            if (source.length + count > MaxCharacters) throw TooLarge()
            source.append(buffer, 0, count)
        }

        val records = records(source).filterNot { it.size == 1 && it[0].isBlank() }.iterator()
        if (!records.hasNext()) throw BadHeader()
        val header = records.next().map { it.trim().lowercase(Locale.ROOT) }
        val names = columns.map { it.lowercase(Locale.ROOT) }
        if (header.any { it !in names } || header.distinct().size != header.size ||
            "word" !in header || "meaning" !in header
        ) throw BadHeader()
        val indices = names.map(header::indexOf)
        val words = mutableListOf<Word>()
        var invalid = 0
        while (records.hasNext()) {
            if (words.size + invalid >= MaxRecords) throw TooLarge()
            val fields = records.next()
            val (term, meaning, phonetic, example, translation) = indices.map { index ->
                fields.getOrNull(index)?.trim().orEmpty()
            }
            if (fields.size != header.size || term.isEmpty() || meaning.isEmpty() ||
                term.length > 80 || meaning.length > 500 || phonetic.length > 120 ||
                example.length > 1000 || translation.length > 1000 ||
                term.any { it.isISOControl() } || fields.any { '\u0000' in it }
            ) {
                invalid++
            } else {
                words += Word(bookId, term, meaning, phonetic = phonetic,
                    example = example, exampleTranslation = translation)
            }
        }
        return Import(words, invalid)
    }

    fun write(writer: Writer, words: List<Word>) {
        writer.write("\uFEFF")
        writeRecord(writer, columns)
        for (word in words) {
            writeRecord(writer, listOf(word.term, word.meaning, word.phonetic, word.example, word.exampleTranslation))
        }
    }

    private fun writeRecord(writer: Writer, fields: List<String>) {
        writer.write(fields.joinToString(",") { field ->
            if (field.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
                "\"" + field.replace("\"", "\"\"") + "\""
            } else field
        })
        writer.write("\r\n")
    }

    private fun records(source: CharSequence) = sequence {
        var index = if (source.firstOrNull() == '\uFEFF') 1 else 0
        var recordNumber = 1
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var quoteClosed = false
        while (index < source.length) {
            val char = source[index++]
            if (quoted) {
                if (char != '"') {
                    field.append(char)
                } else if (source.getOrNull(index) == '"') {
                    field.append('"')
                    index++
                } else {
                    quoted = false
                    quoteClosed = true
                }
                continue
            }
            when (char) {
                ',' -> {
                    fields.add(field.toString())
                    field.clear()
                    quoteClosed = false
                }
                '\r', '\n' -> {
                    if (char == '\r' && source.getOrNull(index) == '\n') index++
                    yield(fields + field.toString())
                    recordNumber++
                    fields.clear()
                    field.clear()
                    quoteClosed = false
                }
                '"' -> {
                    if (field.isNotEmpty() || quoteClosed) throw BadRecord(recordNumber)
                    quoted = true
                }
                else -> {
                    if (quoteClosed) throw BadRecord(recordNumber)
                    field.append(char)
                }
            }
        }
        if (quoted) throw BadRecord(recordNumber)
        if (fields.isNotEmpty() || field.isNotEmpty() || quoteClosed) yield(fields + field.toString())
    }
}
