package com.maxiaol624.wordtrail.quiz

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import java.util.UUID

@Dao
interface QuizDao {
    @Query("SELECT name FROM word_books WHERE id = :bookId")
    suspend fun bookName(bookId: Long): String?

    @Query("SELECT id, term, meaning FROM words WHERE bookId = :bookId ORDER BY id")
    suspend fun words(bookId: Long): List<QuizWord>

    @Query("SELECT id FROM quiz_sessions WHERE bookId = :bookId AND completedAt IS NULL ORDER BY createdAt DESC LIMIT 1")
    suspend fun activeId(bookId: Long): String?

    @Query("""
        SELECT s.id, s.completedAt, COUNT(*) AS total,
            SUM(CASE WHEN q.selectedIndex = q.correctIndex THEN 1 ELSE 0 END) AS correct
        FROM quiz_sessions s JOIN quiz_questions q ON q.sessionId = s.id
        WHERE s.bookId = :bookId AND s.completedAt IS NOT NULL
        GROUP BY s.id ORDER BY s.completedAt DESC, s.id LIMIT 20
    """)
    suspend fun history(bookId: Long): List<QuizHistory>

    @Transaction
    suspend fun lobby(bookId: Long) = QuizLobby(
        bookName(bookId), words(bookId).map { it.meaning.trim() }.filter { it.isNotEmpty() }.distinct().size,
        activeId(bookId), history(bookId),
    )

    @Query("SELECT * FROM quiz_sessions WHERE id = :id AND bookId = :bookId")
    suspend fun session(id: String, bookId: Long): QuizSession?

    @Query("SELECT * FROM quiz_questions WHERE sessionId = :id ORDER BY position")
    suspend fun questions(id: String): List<QuizQuestion>

    @Transaction
    suspend fun attempt(id: String, bookId: Long): QuizAttempt? =
        session(id, bookId)?.let { QuizAttempt(it, questions(id)) }

    @Insert
    suspend fun insertSession(session: QuizSession)

    @Insert
    suspend fun insertQuestions(questions: List<QuizQuestion>)

    @Transaction
    suspend fun startOrResume(bookId: Long, now: Long): String? {
        activeId(bookId)?.let { return it }
        if (bookName(bookId) == null) return null
        val id = UUID.randomUUID().toString()
        val questions = QuizGenerator.generate(id, words(bookId))
        if (questions.isEmpty()) return null
        insertSession(QuizSession(id, bookId, now))
        insertQuestions(questions)
        return id
    }

    @Query("""
        UPDATE quiz_questions SET selectedIndex = :choice
        WHERE sessionId = :id AND position = :position AND selectedIndex IS NULL
    """)
    suspend fun selectAnswer(id: String, position: Int, choice: Int): Int

    @Query("""
        UPDATE quiz_sessions SET completedAt = :now WHERE id = :id AND completedAt IS NULL
          AND NOT EXISTS (SELECT 1 FROM quiz_questions WHERE sessionId = :id AND selectedIndex IS NULL)
    """)
    suspend fun completeIfAnswered(id: String, now: Long)

    @Transaction
    suspend fun answer(id: String, bookId: Long, position: Int, choice: Int, now: Long) {
        require(choice in 0..3)
        val session = session(id, bookId) ?: return
        if (session.completedAt != null) return
        if (selectAnswer(id, position, choice) == 1) completeIfAnswered(id, now)
    }
}
