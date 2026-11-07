package com.maxiaol624.wordtrail.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

class ReviewReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        ReminderCoordinator.check(applicationContext)
        Result.success()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Log.w("ReviewReminder", "Unable to check for due words", error)
        Result.retry()
    }
}
