package com.maxiaol624.wordtrail.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.maxiaol624.wordtrail.CatalogActivity
import com.maxiaol624.wordtrail.MainActivity
import com.maxiaol624.wordtrail.R
import com.maxiaol624.wordtrail.data.WordDatabase
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class StudyWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            RefreshAction, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> refresh(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context)
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshAlarm(context))
    }

    private fun refresh(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val provider = component(app)
        if (manager.getAppWidgetIds(provider).isEmpty()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000) {
                    updateLock.withLock {
                        val now = ZonedDateTime.now()
                        val dayStart = now.toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli()
                        val summary = WordDatabase.get(app).widget().summary(dayStart, now.toInstant().toEpochMilli())
                        manager.updateAppWidget(provider, views(app, summary))
                        val alarms = app.getSystemService(AlarmManager::class.java)
                        val alarm = refreshAlarm(app)
                        // Refresh near the next change while the device is awake.
                        alarms.setWindow(AlarmManager.RTC, summary.nextRefreshAt(now), 10 * 60_000L, alarm)
                        if (manager.getAppWidgetIds(provider).isEmpty()) alarms.cancel(alarm)
                    }
                }
            } catch (error: Exception) {
                Log.w("StudyWidget", "Unable to update study widget", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val LearnAction = "com.maxiaol624.wordtrail.widget.LEARN"
        const val ReviewAction = "com.maxiaol624.wordtrail.widget.REVIEW"
        private const val RefreshAction = "com.maxiaol624.wordtrail.widget.REFRESH"
        private val updateLock = Mutex()

        fun component(context: Context) = ComponentName(context, StudyWidgetProvider::class.java)

        private fun refreshIntent(context: Context) = Intent(RefreshAction).setComponent(component(context))

        private fun refreshAlarm(context: Context) = PendingIntent.getBroadcast(
            context, 0, refreshIntent(context), PendingIntent.FLAG_IMMUTABLE,
        )

        fun requestRefresh(context: Context) {
            val provider = component(context)
            if (AppWidgetManager.getInstance(context).getAppWidgetIds(provider).isNotEmpty()) {
                context.sendBroadcast(refreshIntent(context))
            }
        }

        private fun views(context: Context, summary: WidgetSummary) =
            RemoteViews(context.packageName, R.layout.widget_study).apply {
                setTextViewText(R.id.widget_title, context.getString(
                    if (summary.hasWords) R.string.widget_title else R.string.widget_empty,
                ))
                setTextViewText(R.id.widget_learned, context.getString(R.string.widget_count, summary.learnedToday))
                setTextViewText(R.id.widget_due, context.getString(R.string.widget_count, summary.dueCount))
                setTextViewText(R.id.widget_learn, context.getString(
                    if (summary.hasNewWords) R.string.widget_learn else R.string.open_books,
                ))
                setTextViewText(R.id.widget_review, context.getString(
                    if (summary.dueCount > 0) R.string.widget_review else R.string.open_statistics,
                ))
                val learn = if (summary.hasNewWords) Intent(context, MainActivity::class.java).setAction(LearnAction)
                else Intent(context, CatalogActivity::class.java)
                val review = Intent(context, MainActivity::class.java).setAction(ReviewAction)
                setOnClickPendingIntent(R.id.widget_learn, shortcut(context, learn))
                setOnClickPendingIntent(R.id.widget_review, shortcut(context, review))
            }

        private fun shortcut(context: Context, intent: Intent): PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
