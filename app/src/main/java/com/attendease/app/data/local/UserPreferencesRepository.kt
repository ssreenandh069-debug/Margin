package com.attendease.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private val IS_GUEST_KEY = booleanPreferencesKey("is_guest")

    val isGuestFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_GUEST_KEY] ?: false
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_GUEST_KEY] = isGuest
        }
    }
}
