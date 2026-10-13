package com.Margin.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.Margin.app.data.local.AppDatabase
import com.Margin.app.reminders.TaskReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val triggeringActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in triggeringActions) return
        if (!androidx.core.os.UserManagerCompat.isUserUnlocked(context)) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val tasks = db.taskDao().getPendingTasksWithReminders()
                tasks.forEach { task ->
                    TaskReminderManager.scheduleTaskAlarms(context, task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
