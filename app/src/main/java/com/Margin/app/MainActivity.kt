package com.attendease.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.attendease.app.navigation.MainScaffold
import com.attendease.app.ui.screens.LoginScreen
import com.attendease.app.ui.theme.AttendEaseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val userPrefs = (application as AttendEaseApplication).userPreferencesRepository
        
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            val isGuest by userPrefs.isGuestFlow.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            
            AttendEaseTheme(isDarkTheme = isDarkTheme) {
                if (isGuest == true) {
                    MainScaffold(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme }
                    )
                } else if (isGuest == false) {
                    LoginScreen(onEnterApp = {
                        scope.launch { userPrefs.setGuestMode(true) }
                    })
                }
                // When isGuest is null, we are loading, show nothing or splash
            }
        }
    }
}

