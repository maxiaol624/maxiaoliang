package com.maxiaol624.wordtrail.reminder

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.map

private val Context.reminderStore by preferencesDataStore(name = "review_reminder")

internal class ReminderStore(context: Context) {
    private val store = context.applicationContext.reminderStore
    val settings = store.data.map { preferences ->
        val minutes = preferences[Time]?.takeIf { it in 0 until 24 * 60 } ?: 20 * 60
        ReminderSettings(
            enabled = preferences[Enabled] ?: false,
            time = LocalTime.of(minutes / 60, minutes % 60),
            lastSentDate = preferences[LastSent]?.let(LocalDate::ofEpochDay),
            permissionAsked = preferences[Asked] ?: false,
        )
    }

    suspend fun setEnabled(value: Boolean) { store.edit { it[Enabled] = value } }
    suspend fun setTime(value: LocalTime) { store.edit { it[Time] = value.hour * 60 + value.minute } }
    suspend fun setLastSent(date: LocalDate) { store.edit { it[LastSent] = date.toEpochDay() } }
    suspend fun markPermissionAsked() { store.edit { it[Asked] = true } }

    private companion object {
        val Enabled = booleanPreferencesKey("enabled")
        val Time = intPreferencesKey("minutes_of_day")
        val LastSent = longPreferencesKey("last_sent_day")
        val Asked = booleanPreferencesKey("notification_permission_asked")
    }
}
