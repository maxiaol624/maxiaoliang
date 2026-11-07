package com.maxiaol624.wordtrail.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.maxiaol624.wordtrail.data.WordDatabase
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object ReminderCoordinator {
    const val WorkName = "daily_review_reminder"
    private val lock = Mutex()

    suspend fun setEnabled(context: Context, enabled: Boolean) = lock.withLock {
        val store = ReminderStore(context)
        store.setEnabled(enabled)
        if (enabled) schedule(context, store.settings.first(), ZonedDateTime.now(), false, ExistingPeriodicWorkPolicy.UPDATE)
        else {
            WorkManager.getInstance(context).cancelUniqueWork(WorkName).await()
            ReviewNotifications.cancel(context)
        }
    }

    suspend fun setTime(context: Context, time: LocalTime) = lock.withLock {
        val store = ReminderStore(context)
        store.setTime(time)
        val settings = store.settings.first()
        if (settings.enabled) schedule(context, settings, ZonedDateTime.now(), false, ExistingPeriodicWorkPolicy.UPDATE)
    }

    suspend fun refresh(context: Context, update: Boolean = false) = lock.withLock {
        val settings = ReminderStore(context).settings.first()
        if (settings.enabled) schedule(context, settings, ZonedDateTime.now(), false,
            if (update) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP)
        else if (update) {
            WorkManager.getInstance(context).cancelUniqueWork(WorkName).await()
            ReviewNotifications.cancel(context)
        }
    }

    suspend fun check(context: Context) = lock.withLock {
        val store = ReminderStore(context)
        var settings = store.settings.first()
        if (!settings.enabled) return@withLock
        val now = ZonedDateTime.now()
        val checkedToday = ReminderSchedule.shouldCheck(now, settings)
        if (checkedToday && ReviewNotifications.allowed(context)) {
            val book = WordDatabase.get(context).words().firstDueReviewBook(now.toInstant().toEpochMilli())
            if (book != null && ReviewNotifications.post(context, book)) {
                store.setLastSent(now.toLocalDate())
                settings = settings.copy(lastSentDate = now.toLocalDate())
            }
        }
        schedule(context, settings, now, checkedToday, ExistingPeriodicWorkPolicy.UPDATE)
    }

    private suspend fun schedule(
        context: Context, settings: ReminderSettings, now: ZonedDateTime,
        checkedToday: Boolean, policy: ExistingPeriodicWorkPolicy,
    ) {
        val next = ReminderSchedule.nextCheck(now, settings, checkedToday)
        val request = PeriodicWorkRequestBuilder<ReviewReminderWorker>(1, TimeUnit.DAYS)
            .setNextScheduleTimeOverride(next.toEpochMilli())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WorkName, policy, request).await()
    }
}
