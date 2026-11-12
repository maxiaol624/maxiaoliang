package com.maxiaol624.wordtrail.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxiaol624.wordtrail.data.WordDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

sealed interface QuizPage {
    object Loading : QuizPage
    object Error : QuizPage
    data class Lobby(val data: QuizLobby) : QuizPage
    data class Question(val attempt: QuizAttempt, val position: Int) : QuizPage
    data class Result(val attempt: QuizAttempt) : QuizPage
}

data class QuizRoute(val bookId: Long = 0, val sessionId: String? = null, val position: Int = -1)
data class QuizScreen(val page: QuizPage, val busy: Boolean, val saveFailed: Boolean)

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WordDatabase.get(application)
    private val dao = database.quizzes()
    val route = MutableStateFlow(QuizRoute())
    private val reload = MutableStateFlow(0)
    private val busy = MutableStateFlow(false)
    private val saveFailed = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pages = combine(route, reload) { current, _ -> current }.flatMapLatest { current ->
        if (current.bookId <= 0) flowOf<QuizPage>(QuizPage.Loading)
        else database.invalidationTracker.createFlow("word_books", "words", "quiz_sessions", "quiz_questions")
            .map {
                val id = current.sessionId
                if (id == null) QuizPage.Lobby(dao.lobby(current.bookId))
                else {
                    val attempt = dao.attempt(id, current.bookId)
                    if (attempt == null || attempt.questions.isEmpty()) QuizPage.Error
                    else if (current.position < 0 && attempt.session.completedAt != null) QuizPage.Result(attempt)
                    else {
                        val index = current.position.takeIf { it in attempt.questions.indices }
                            ?: attempt.questions.indexOfFirst { it.selectedIndex == null }.coerceAtLeast(0)
                        QuizPage.Question(attempt, index)
                    }
                }
            }.onStart { emit(QuizPage.Loading) }.catch { emit(QuizPage.Error) }
    }.flowOn(Dispatchers.Default)

    val state = combine(pages, busy, saveFailed) { page, saving, failed -> QuizScreen(page, saving, failed) }

    fun open(bookId: Long, sessionId: String?, position: Int) {
        if (route.value.bookId == 0L) route.value = QuizRoute(bookId, sessionId, position)
    }

    fun showLobby() { if (!busy.value) navigate(route.value.copy(sessionId = null, position = -1)) }
    fun showAttempt(id: String) = navigate(route.value.copy(sessionId = id, position = -1))
    fun next(position: Int, total: Int) = navigate(route.value.copy(position = if (position + 1 < total) position + 1 else -1))
    fun retry() { saveFailed.value = false; reload.value++ }

    fun start(replaceUnfinishedId: String? = null) = operation {
        val id = dao.startOrResume(route.value.bookId, System.currentTimeMillis(), replaceUnfinishedId)
        if (id != null) navigate(route.value.copy(sessionId = id, position = -1))
        else reload.value++
    }

    fun answer(position: Int, choice: Int) = operation {
        val current = route.value
        route.value = current.copy(position = position)
        current.sessionId?.let { dao.answer(it, current.bookId, position, choice, System.currentTimeMillis()) }
    }

    private fun navigate(target: QuizRoute) { saveFailed.value = false; route.value = target }

    private fun operation(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        saveFailed.value = false
        viewModelScope.launch {
            try { action() }
            catch (error: android.database.sqlite.SQLiteException) { saveFailed.value = true }
            finally { busy.value = false }
        }
    }
}
