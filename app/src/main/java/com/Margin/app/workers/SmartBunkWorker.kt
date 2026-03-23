package com.Margin.app.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Margin.app.data.local.AppDatabase
import kotlin.math.floor

class SmartBunkWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(context)

            // Get active session
            val activeSession = db.sessionDao().getActiveSession() ?: return Result.success()
            val subjects = db.subjectDao().getSubjectsBySessionOnce(activeSession.id)

            if (subjects.isEmpty()) return Result.success()

            val subjectIds = subjects.map { it.id }
            val allRecords = db.attendanceRecordDao().getRecordsForSubjectsOnce(subjectIds)

            // Aggregate across all subjects
            val attended = allRecords.count { it.status == "PRESENT" || it.status == "PROXY" }
            val total = allRecords.count { it.status == "PRESENT" || it.status == "ABSENT" || it.status == "PROXY" }

            if (total == 0) return Result.success()

            // The Margin Math™ — how many can you safely bunk?
            val safeBunks = floor((attended - (0.75 * total)) / 0.25).toInt()

            val notifTitle = "📊 Your Bunk Budget"
            val notifBody = when {
                safeBunks >= 2 -> "You can safely bunk $safeBunks classes right now. Use them wisely! 🥷"
                safeBunks == 1 -> "You have exactly 1 safe bunk left. Don't waste it! ⚠️"
                else           -> "Thinking of bunking? Sike. Your attendance is too low. Get to class! 💀"
            }

            sendNotification(notifTitle, notifBody)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(title: String, body: String) {
        val channelId = "SMART_BUNK"
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifManager.createNotificationChannel(
                NotificationChannel(channelId, "Smart Bunk Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Daily attendance bunk budget analysis."
                }
            )
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notifManager.notify(NOTIF_ID, notification)
    }

    companion object {
        const val WORK_NAME = "SmartBunkDailyWorker"
        private const val NOTIF_ID = 9999
    }
}
