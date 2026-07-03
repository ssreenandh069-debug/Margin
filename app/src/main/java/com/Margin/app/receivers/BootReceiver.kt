package com.Margin.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.Margin.app.data.local.AppDatabase
import com.Margin.app.utils.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restores all pending task alarms after a device reboot.
 *
 * Problem: AlarmManager alarms are cleared when the device powers off.
 * Fix: On BOOT_COMPLETED, query every non-completed task from the DB
 * and reschedule its alarm chain via NotificationScheduler.
 *
 * Registered in AndroidManifest.xml with:
 *   <action android:name="android.intent.action.BOOT_COMPLETED" />
 *   <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
 *
 * LOCKED_BOOT_COMPLETED fires in Direct Boot mode (before the user unlocks)
 * on API 24+. Including it means reminders are restored even if the user
 * never unlocks the device (e.g. a powered-on kiosk). For simplicity this
 * receiver does not use credential-encrypted storage, so it is safe.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val triggeringActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED  // Re-arm alarms after app update too
        )
        if (intent.action !in triggeringActions) return

        // goAsync() lets us do coroutine work without the system killing the
        // BroadcastReceiver before the DB query finishes.
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db           = AppDatabase.getInstance(context)
                val pendingTasks = db.taskDao().getPendingTasks()
                val now          = System.currentTimeMillis()

                pendingTasks
                    .filter { it.dueDate > now }   // Skip already-overdue tasks
                    .forEach { task ->
                        NotificationScheduler.scheduleTaskAlarms(context, task)
                    }
            } finally {
                // Must always call finish() to release the wake lock held by goAsync()
                pendingResult.finish()
            }
        }
    }
}
