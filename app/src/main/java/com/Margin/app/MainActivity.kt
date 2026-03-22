package com.Margin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.Margin.app.navigation.MainScaffold
import com.Margin.app.ui.screens.LoginScreen
import com.Margin.app.ui.theme.MarginTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val userPrefs = (application as MarginApplication).userPreferencesRepository
        
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            val isGuest by userPrefs.isGuestFlow.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            
            MarginTheme(isDarkTheme = isDarkTheme) {
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

