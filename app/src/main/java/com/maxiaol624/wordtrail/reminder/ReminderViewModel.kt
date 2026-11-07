package com.maxiaol624.wordtrail.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

sealed interface ReminderLoad {
    object Loading : ReminderLoad
    object Error : ReminderLoad
    data class Ready(val settings: ReminderSettings) : ReminderLoad
}

data class ReminderScreen(val load: ReminderLoad, val saving: Boolean, val saveFailed: Boolean)

class ReminderViewModel(private val app: Application) : AndroidViewModel(app) {
    private val store = ReminderStore(app)
    private val reload = MutableStateFlow(0)
    private val saving = MutableStateFlow(false)
    private val failed = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val load = reload.flatMapLatest {
        store.settings.map<ReminderSettings, ReminderLoad> { ReminderLoad.Ready(it) }
            .onStart { emit(ReminderLoad.Loading) }.catch { emit(ReminderLoad.Error) }
    }

    val state = combine(load, saving, failed, ::ReminderScreen)

    fun setEnabled(enabled: Boolean) = change { ReminderCoordinator.setEnabled(app, enabled) }
    fun setTime(time: LocalTime) = change { ReminderCoordinator.setTime(app, time) }

    fun permissionResult(granted: Boolean) = change {
        store.markPermissionAsked()
        if (granted) ReminderCoordinator.refresh(app, update = true)
    }

    fun permissionRestored() = change { ReminderCoordinator.refresh(app, update = true) }

    fun retry() = change {
        reload.value++
        ReminderCoordinator.refresh(app, update = true)
    }

    private fun change(action: suspend () -> Unit) {
        if (saving.value) return
        saving.value = true
        failed.value = false
        viewModelScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                failed.value = true
            } finally {
                saving.value = false
            }
        }
    }
}
