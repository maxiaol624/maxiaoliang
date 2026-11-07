package com.maxiaol624.wordtrail.quiz

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.maxiaol624.wordtrail.data.WordBook
import kotlin.random.Random

@Entity(
    tableName = "quiz_sessions",
    foreignKeys = [ForeignKey(entity = WordBook::class, parentColumns = ["id"],
        childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId")],
)
data class QuizSession(
    @PrimaryKey val id: String,
    val bookId: Long,
    val createdAt: Long,
    val completedAt: Long? = null,
)

@Entity(
    tableName = "quiz_questions",
    primaryKeys = ["sessionId", "position"],
    foreignKeys = [ForeignKey(entity = QuizSession::class, parentColumns = ["id"],
        childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
)
data class QuizQuestion(
    val sessionId: String,
    val position: Int,
    val wordId: Long,
    val term: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int,
    val selectedIndex: Int? = null,
) {
    fun options() = listOf(optionA, optionB, optionC, optionD)
    fun isCorrect() = selectedIndex == correctIndex
}

data class QuizWord(val id: Long, val term: String, val meaning: String)
data class QuizAttempt(val session: QuizSession, val questions: List<QuizQuestion>)
data class QuizHistory(val id: String, val completedAt: Long, val total: Int, val correct: Int)
data class QuizLobby(val name: String?, val meaningCount: Int, val activeId: String?, val history: List<QuizHistory>)

object QuizGenerator {
    fun generate(sessionId: String, words: List<QuizWord>, random: Random = Random.Default): List<QuizQuestion> {
        val meanings = words.map { it.meaning.trim() }.filter { it.isNotEmpty() }.distinct()
        if (meanings.size < 4) return emptyList()
        return words.filter { it.meaning.isNotBlank() }.shuffled(random).take(10).mapIndexed { index, word ->
            val correct = word.meaning.trim()
            val options = (meanings.filter { it != correct }.shuffled(random).take(3) + correct).shuffled(random)
            QuizQuestion(sessionId, index, word.id, word.term,
                options[0], options[1], options[2], options[3], options.indexOf(correct))
        }
    }

    fun percentage(correct: Int, total: Int): Int = if (total == 0) 0 else (correct.toLong() * 100 / total).toInt()
}
