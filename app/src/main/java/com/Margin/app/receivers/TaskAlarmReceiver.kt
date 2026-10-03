package com.Margin.app.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.Margin.app.reminders.TaskReminderManager

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra(TaskReminderManager.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val bodyText = intent.getStringExtra(TaskReminderManager.EXTRA_NOTIF_BODY) ?: "You have an upcoming task due soon!"
        val notifId = intent.getIntExtra(TaskReminderManager.EXTRA_NOTIF_ID, System.currentTimeMillis().toInt())

        val channelId = "TASK_REMINDERS"
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifManager.createNotificationChannel(
                NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Deadlines and reminders for upcoming tasks."
                    enableVibration(true)
                }
            )
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("📋 $taskTitle")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            notifManager.notify(notifId, notification)
        } catch (_: SecurityException) {}
    }
}
