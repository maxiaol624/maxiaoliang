package com.maxiaol624.wordtrail.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.maxiaol624.wordtrail.CatalogActivity
import com.maxiaol624.wordtrail.MainActivity
import com.maxiaol624.wordtrail.R
import com.maxiaol624.wordtrail.StudyActivity
import com.maxiaol624.wordtrail.data.DueReviewBook

internal object ReviewNotifications {
    private const val Channel = "review_reminders"
    private const val NotificationId = 1001

    fun hasPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun allowed(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return hasPermission(context) && manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(Channel)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            Channel, context.getString(R.string.reminder_title), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) })
    }

    fun settingsIntent(context: Context): Intent {
        createChannel(context)
        val appAllowed = hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()
        return Intent(if (appAllowed) Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS else Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .apply { if (appAllowed) putExtra(Settings.EXTRA_CHANNEL_ID, Channel) }
    }

    fun post(context: Context, book: DueReviewBook): Boolean {
        createChannel(context)
        if (!allowed(context)) return false
        val pending = TaskStackBuilder.create(context)
            .addNextIntent(Intent(context, MainActivity::class.java))
            .addNextIntent(CatalogActivity.bookIntent(context, book.id))
            .addNextIntent(StudyActivity.intent(context, book.id, review = true))
            .getPendingIntent(NotificationId, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val message = context.getString(R.string.reminder_notification_body, book.name, book.dueCount)
        val notification = NotificationCompat.Builder(context, Channel)
            .setSmallIcon(R.drawable.ic_review_notification)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        return try {
            // Permission can change between the check above and posting.
            if (!hasPermission(context)) return false
            NotificationManagerCompat.from(context).notify(NotificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(context: Context) { NotificationManagerCompat.from(context).cancel(NotificationId) }
}
