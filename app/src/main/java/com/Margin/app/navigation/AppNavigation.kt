package com.Margin.app.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.Margin.app.ui.screens.TaskListScreen
import com.Margin.app.ui.screens.TimetableScreen
import com.Margin.app.ui.screens.TrackScreen
import com.Margin.app.ui.theme.NeonTeal
import com.Margin.app.ui.theme.NeonTealAlpha20
import com.Margin.app.ui.theme.TextSecondary

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Track : Screen("track", "Track", Icons.Filled.TrackChanges, Icons.Outlined.TrackChanges)
    object Timetable : Screen("timetable", "Timetable", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Tasks : Screen("tasks", "Remind", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment)
}

val bottomNavItems = listOf(Screen.Track, Screen.Timetable, Screen.Tasks)

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomNavVisible = currentRoute in listOf(
        Screen.Track.route, Screen.Timetable.route, Screen.Tasks.route
    )

    Scaffold(
        bottomBar = {
            if (isBottomNavVisible) {
                NavigationBar(
                    containerColor = Color(0xFF12121A),
                    tonalElevation = androidx.compose.ui.unit.Dp(0f)
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonTeal,
                                selectedTextColor = NeonTeal,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = NeonTealAlpha20
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Track.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() + slideInHorizontally { it / 6 } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it / 6 } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it / 6 } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 6 } }
        ) {
            composable(Screen.Track.route) { TrackScreen() }
            composable(Screen.Timetable.route) { TimetableScreen() }
            composable(Screen.Tasks.route) { TaskListScreen() }
        }
    }
}
