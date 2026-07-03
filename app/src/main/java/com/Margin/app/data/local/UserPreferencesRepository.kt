package com.Margin.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private val ACTIVE_SESSION_KEY = stringPreferencesKey("active_session_id")

    /** Emits the persisted active session ID, or null if none saved. */
    val activeSessionIdFlow: Flow<String?> = dataStore.data.map { it[ACTIVE_SESSION_KEY] }

    suspend fun saveActiveSessionId(sessionId: String) {
        dataStore.edit { it[ACTIVE_SESSION_KEY] = sessionId }
    }

    suspend fun clearActiveSessionId() {
        dataStore.edit { it.remove(ACTIVE_SESSION_KEY) }
    }
}
