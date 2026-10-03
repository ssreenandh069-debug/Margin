package com.Margin.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.Margin.app.data.local.entity.TaskEntity
import com.Margin.app.receivers.TaskAlarmReceiver
import java.util.Calendar

object TaskReminderManager {

    const val EXTRA_TASK_TITLE = "TASK_TITLE"
    const val EXTRA_NOTIF_BODY = "NOTIF_BODY"
    const val EXTRA_NOTIF_ID   = "NOTIF_ID"

    fun scheduleTaskAlarms(context: Context, task: TaskEntity, subjectName: String? = null) {
        if (!task.hasReminder || task.isCompleted) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        if (task.dueDate <= now) return

        val baseCode = task.id.hashCode()
        val subjectPrefix = if (!subjectName.isNullOrBlank()) "[$subjectName] " else ""

        val dayBeforeCal = Calendar.getInstance().apply {
            timeInMillis = task.dueDate
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dayOfCal = Calendar.getInstance().apply {
            timeInMillis = task.dueDate
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val alarms = listOf(
            Triple(dayBeforeCal.timeInMillis, baseCode + 1, "${subjectPrefix}\"${task.title}\" is due tomorrow!"),
            Triple(dayOfCal.timeInMillis, baseCode + 2, "${subjectPrefix}\"${task.title}\" is due today!")
        )

        for ((triggerTime, requestCode, body) in alarms) {
            if (triggerTime > now) {
                scheduleAlarm(context, alarmManager, requestCode, task.title, body, triggerTime)
            }
        }
    }

    fun cancelTaskAlarms(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val baseCode = taskId.hashCode()
        for (offset in 1..2) {
            val intent = Intent(context, TaskAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                baseCode + offset,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: continue
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        title: String,
        body: String,
        triggerAtMillis: Long
    ) {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_NOTIF_BODY, body)
            putExtra(EXTRA_NOTIF_ID, requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
