package com.maxiaol624.wordtrail.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ReminderClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIME_CHANGED && intent.action != Intent.ACTION_TIMEZONE_CHANGED) return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000) { ReminderCoordinator.refresh(app, update = true) }
            } catch (error: Exception) {
                Log.w("ReviewReminder", "Unable to reschedule after clock change", error)
            } finally {
                pending.finish()
            }
        }
    }
}
