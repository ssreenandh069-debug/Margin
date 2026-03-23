package com.Margin.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import com.Margin.app.data.local.AppDatabase
import com.Margin.app.data.local.AttendanceRepository
import com.Margin.app.data.local.UserPreferencesRepository
import com.Margin.app.workers.SmartBunkWorker
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "margin_prefs")

class MarginApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: AttendanceRepository by lazy { AttendanceRepository(database) }
    val userPreferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(dataStore) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        enqueueSmartBunkWorker()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // High-priority channel for task deadline alarms
            notifManager.createNotificationChannel(
                NotificationChannel(
                    "TASK_REMINDERS",
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Aggressive reminders for upcoming tasks and assignments."
                    enableVibration(true)
                }
            )

            // Default channel for daily bunk-budget roasts
            notifManager.createNotificationChannel(
                NotificationChannel(
                    "SMART_BUNK",
                    "Daily Roasts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily attendance bunk budget analysis."
                }
            )
        }
    }

    private fun enqueueSmartBunkWorker() {
        val request = PeriodicWorkRequestBuilder<SmartBunkWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SmartBunkWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Don't reset the timer if already enqueued
            request
        )
    }
}
