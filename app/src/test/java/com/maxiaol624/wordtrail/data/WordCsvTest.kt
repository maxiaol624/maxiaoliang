package com.maxiaol624.wordtrail.data

import java.io.StringWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordCsvTest {
    @Test fun exportAndImportPreserveQuotedChineseAndMultilineFields() {
        val word = Word(
            bookId = 1, term = "say", meaning = "说, 讲\"话\"\n表达",
            phonetic = "/seɪ/", example = "She said \"hi\".\r\nHe smiled.",
            exampleTranslation = "她说了“你好”，他笑了。",
        )
        val writer = StringWriter()
        WordCsv.write(writer, listOf(word))
        assertTrue(writer.toString().startsWith("\uFEFFword,meaning,phonetic,example,exampleTranslation\r\n"))
        val imported = WordCsv.read(writer.toString().reader(), 2)
        assertEquals(listOf(word.copy(bookId = 2)), imported.words)
        assertEquals(0, imported.invalid)
    }

    @Test fun columnsCanBeReorderedAndInvalidRowsAreCounted() {
        val source = "\uFEFFMeaning,WORD\n苹果,apple\n,empty\n不完整\n\n香蕉,banana"
        val result = WordCsv.read(source.reader(), 7)
        assertEquals(listOf(Word(7, "apple", "苹果"), Word(7, "banana", "香蕉")), result.words)
        assertEquals(2, result.invalid)
    }

    @Test(expected = WordCsv.BadHeader::class)
    fun duplicateHeaderIsRejected() {
        WordCsv.read("word,meaning,WORD\none,一,two".reader(), 1)
    }

    @Test fun malformedQuoteReportsRecordEvenAfterAValidRow() {
        val source = "word,meaning\none,一\ntwo,\"bad\nthree,三"
        val error = runCatching { WordCsv.read(source.reader(), 1) }.exceptionOrNull()
        assertEquals(3, (error as WordCsv.BadRecord).number)
    }

    @Test fun exportedEmptyBookCanBeReadAsTemplate() {
        val writer = StringWriter()
        WordCsv.write(writer, emptyList())
        assertEquals(WordCsv.Import(emptyList(), 0), WordCsv.read(writer.toString().reader(), 1))
    }
}
