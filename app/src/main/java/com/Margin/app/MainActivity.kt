package com.Margin.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.Margin.app.navigation.MainScaffold
import com.Margin.app.ui.theme.MarginTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    // ── Android 13+ POST_NOTIFICATIONS permission launcher ────────────────────
    //
    // The system shows a one-time dialog the first time this fires.
    // If the user denies, we don't crash — notifications simply won't appear.
    // We don't ask again on every launch (system enforces this anyway after
    // two denials, but it's also good UX practice).
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled silently — the app works without notifications */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
        requestNotificationPermissionIfNeeded()

        val userPrefs = (application as MarginApplication).userPreferencesRepository

        // Read DataStore synchronously BEFORE setContent renders any frame.
        // runBlocking here blocks only the ~1ms DataStore cold read — acceptable on main thread
        // before the UI pipeline starts. Eliminates the "flash to Session screen" on cold boot.
        val initialSessionId: String? = runBlocking {
            userPrefs.activeSessionIdFlow.first()
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            MarginTheme(isDarkTheme = isDarkTheme) {
                MainScaffold(
                    isDarkTheme      = isDarkTheme,
                    onToggleTheme    = { isDarkTheme = !isDarkTheme },
                    initialSessionId = initialSessionId
                )
            }
        }
    }

    /**
     * Checks and requests POST_NOTIFICATIONS permission (Android 13+, API 33+).
     *
     * On Android 12 and below this permission doesn't exist — the OS grants
     * it automatically, so we skip the check entirely.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
