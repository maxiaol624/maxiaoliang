package com.maxiaol624.wordtrail.quiz

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class QuizGeneratorTest {
    @Test fun insufficientDistinctMeaningsCannotProduceAmbiguousOptions() {
        val words = listOf(QuizWord(1, "one", " 一 "), QuizWord(2, "two", "一"),
            QuizWord(3, "three", "二"), QuizWord(4, "four", "三"), QuizWord(5, "blank", "  "))
        assertTrue(QuizGenerator.generate("id", words).isEmpty())
        assertTrue(QuizGenerator.generate("id", emptyList()).isEmpty())
    }

    @Test fun everyQuestionHasOneCorrectAnswerAndThreeDistinctDistractors() {
        val words = (1L..12L).map { QuizWord(it, "word$it", "释义$it") }
        repeat(20) { seed ->
            val questions = QuizGenerator.generate("id", words, Random(seed))
            assertEquals(10, questions.size)
            assertEquals(10, questions.map { it.wordId }.distinct().size)
            assertEquals((0..9).toList(), questions.map { it.position })
            questions.forEach { question ->
                assertEquals("id", question.sessionId)
                assertNull(question.selectedIndex)
                assertEquals(4, question.options().distinct().size)
                assertEquals("释义${question.wordId}", question.options()[question.correctIndex])
            }
        }
    }

    @Test fun repeatedMeaningsDoNotRepeatOptionsOrDropQuestionWords() {
        val words = (1L..6L).map { QuizWord(it, "word$it", " 意思${it % 4} ") }
        val questions = QuizGenerator.generate("id", words, Random(3))
        assertEquals(6, questions.size)
        assertTrue(questions.all { it.options().distinct().size == 4 && it.options().all { s -> s == s.trim() } })
        assertEquals(words.map { it.id }.toSet(), questions.map { it.wordId }.toSet())
    }

    @Test fun scoringUsesSelectedAnswerAndHandlesEmptyTotal() {
        val q = QuizQuestion("id", 0, 1, "one", "一", "二", "三", "四", 2)
        assertFalse(q.isCorrect())
        assertTrue(q.copy(selectedIndex = 2).isCorrect())
        assertFalse(q.copy(selectedIndex = 1).isCorrect())
        assertEquals(0, QuizGenerator.percentage(0, 0))
        assertEquals(66, QuizGenerator.percentage(2, 3))
        assertEquals(100, QuizGenerator.percentage(10, 10))
    }
}
