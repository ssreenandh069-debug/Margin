package com.Margin.app

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.Margin.app.data.local.AppDatabase
import com.Margin.app.data.local.AttendanceRepository
import com.Margin.app.data.local.UserPreferencesRepository

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class AttendEaseApplication : Application() {

    // Lazy initialization so the db is created only when needed
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    
    // The single repository instance exposing data down the hierarchy
    val repository: AttendanceRepository by lazy { AttendanceRepository(database) }
    
    val userPreferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(dataStore) }
}
