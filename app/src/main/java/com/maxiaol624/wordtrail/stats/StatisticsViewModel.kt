package com.maxiaol624.wordtrail.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.maxiaol624.wordtrail.data.WordDatabase
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WordDatabase.get(application)
    private val reload = MutableStateFlow(0)

    // 冷流随页面 STARTED 状态收集，离开页面后停止计时与数据库监听。
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<StatisticsState> = reload.flatMapLatest { observe() }

    fun retry() {
        reload.value++
    }

    private fun observe(): Flow<StatisticsState> {
        val clock = flow {
            while (true) {
                emit(System.currentTimeMillis() to ZoneId.systemDefault())
                delay(1_000)
            }
        }
        return database.invalidationTracker.createFlow(
            "word_books", "words", "first_reviews", "review_schedules", "review_events",
            "quiz_sessions", "quiz_questions",
            emitInitialState = true,
        ).map { database.statistics().snapshot() }
            .combine(clock) { source, (now, zone) -> StatisticsCalculator.calculate(source, now, zone) }
            .distinctUntilChanged()
            .map<StatisticsReport, StatisticsState> { StatisticsState.Ready(it) }
            .onStart { emit(StatisticsState.Loading) }
            .catch { emit(StatisticsState.Error) }
            .flowOn(Dispatchers.Default)
    }
}
