package com.Margin.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.Margin.app.data.local.entity.TaskEntity
import com.Margin.app.receivers.TaskAlarmReceiver
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    /**
     * Schedules aggressive alarms for a task:
     *  - 5 alarms on the day BEFORE due date: 9 AM, 12 PM, 3 PM, 6 PM, 9 PM
     *  - 2 alarms per day for all earlier days: 10 AM, 5 PM
     */
    fun scheduleTaskAlarms(context: Context, task: TaskEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        val dueCalendar = Calendar.getInstance().apply {
            timeInMillis = task.dueDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysUntilDue = TimeUnit.MILLISECONDS.toDays(
            dueCalendar.timeInMillis - todayCalendar.timeInMillis
        ).toInt()

        // Nothing to schedule if already due or past
        if (daysUntilDue <= 0) return

        val baseCode = task.id.hashCode()

        // Day before: 5 alarms at 9 AM, 12 PM, 3 PM, 6 PM, 9 PM
        val dayBeforeTimes = listOf(9 to 0, 12 to 0, 15 to 0, 18 to 0, 21 to 0)
        dayBeforeTimes.forEachIndexed { idx, (hour, min) ->
            val alarmTime = buildAlarmTime(dueCalendar, offsetDays = -1, hour = hour, minute = min)
            if (alarmTime > now) {
                val bodyText = "⚠️ Due TOMORROW: \"${task.title}\" — Last chance to get it done!"
                scheduleExactAlarm(
                    context, alarmManager, task, alarmTime,
                    requestCode = baseCode + (1000 + idx),
                    bodyText = bodyText
                )
            }
        }

        // Earlier days: 2 alarms per day — 10 AM and 5 PM
        val earlyDayTimes = listOf(10 to 0, 17 to 0)
        for (dayOffset in 2..minOf(daysUntilDue, 7)) { // cap to 7 days lookback
            earlyDayTimes.forEachIndexed { idx, (hour, min) ->
                val alarmTime = buildAlarmTime(dueCalendar, offsetDays = -dayOffset, hour = hour, minute = min)
                if (alarmTime > now) {
                    val daysLabel = if (dayOffset == 2) "tomorrow" else "in $dayOffset days"
                    val bodyText = "📌 \"${task.title}\" is due $daysLabel. Don't leave it for the last minute!"
                    scheduleExactAlarm(
                        context, alarmManager, task, alarmTime,
                        requestCode = baseCode + (dayOffset * 100 + idx),
                        bodyText = bodyText
                    )
                }
            }
        }
    }

    /** Cancel all alarms for a given task */
    fun cancelTaskAlarms(context: Context, task: TaskEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val baseCode = task.id.hashCode()
        // Cancel day-before alarms (indices 1000–1004)
        for (i in 0..4) {
            cancelAlarm(context, alarmManager, baseCode + (1000 + i))
        }
        // Cancel earlier-day alarms (days 2–7, indices 0–1)
        for (day in 2..7) {
            for (i in 0..1) {
                cancelAlarm(context, alarmManager, baseCode + (day * 100 + i))
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun buildAlarmTime(dueCalendar: Calendar, offsetDays: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = dueCalendar.timeInMillis
            add(Calendar.DAY_OF_YEAR, offsetDays)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        task: TaskEntity,
        triggerAtMillis: Long,
        requestCode: Int,
        bodyText: String
    ) {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("TASK_TITLE", task.title)
            putExtra("NOTIF_BODY", bodyText)
            putExtra("NOTIF_ID", requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
    }
}
