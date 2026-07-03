package com.Margin.app.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.Margin.app.data.TaskType
import com.Margin.app.ui.screens.*
import com.Margin.app.ui.theme.NeonTeal
import com.Margin.app.ui.theme.NeonTealAlpha20
import com.Margin.app.ui.theme.TextSecondary
import com.Margin.app.ui.viewmodel.SessionViewModel
import com.Margin.app.utils.getAppViewModel

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Track    : Screen("track",    "Track",    Icons.Filled.TrackChanges,  Icons.Outlined.TrackChanges)
    object Schedule : Screen("schedule", "Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Remind   : Screen("remind",   "Remind",   Icons.Filled.Dashboard,     Icons.Outlined.Dashboard)
}

object Routes {
    const val SESSIONS  = "sessions"
    const val TASKS     = "tasks/{type}"          // single parameterised route
    const val TIMETABLE = "timetable"

    /** Build a concrete tasks route from a raw type string (e.g. "ASSIGNMENT"). */
    fun tasks(type: String) = "tasks/$type"
}

val bottomNavItems = listOf(Screen.Track, Screen.Schedule, Screen.Remind)

@Composable
fun MainScaffold(
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    initialSessionId: String? = null,
    sessionViewModel: SessionViewModel = getAppViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomNavVisible = currentRoute in listOf(
        Screen.Track.route, Screen.Schedule.route, Screen.Remind.route
    )

    val sessions by sessionViewModel.sessionsList.collectAsState()

    // Since initialSessionId is read synchronously in MainActivity, we can determine
    // our startDestination instantly without a flash/delay.
    val startDestination = remember(initialSessionId) {
        if (initialSessionId != null) Screen.Track.route else Routes.SESSIONS
    }

    // Still need to handle runtime session deletion (e.g. user deletes their only session)
    LaunchedEffect(sessions) {
        val loaded = sessions.isNotEmpty()
        if (loaded) {
            val hasActive = sessions.any { it.isActive }
            if (!hasActive && currentRoute != Routes.SESSIONS) {
                navController.navigate(Routes.SESSIONS) { popUpTo(0) }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (isBottomNavVisible) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF12121A),
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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn() + slideInHorizontally { it / 6 } },
            exitTransition   = { fadeOut() + slideOutHorizontally { -it / 6 } },
            popEnterTransition  = { fadeIn() + slideInHorizontally { -it / 6 } },
            popExitTransition   = { fadeOut() + slideOutHorizontally { it / 6 } }
        ) {
            composable(Screen.Track.route)    { TrackScreen() }
            composable(Screen.Schedule.route) { ScheduleScreen() }
            composable(Screen.Remind.route) {
                DashboardScreen(
                    isDarkTheme         = isDarkTheme,
                    onToggleTheme       = onToggleTheme,
                    onOpenSessions      = { navController.navigate(Routes.SESSIONS) },
                    onOpenTaskType      = { type -> navController.navigate(Routes.tasks(type)) },
                    onOpenTimetable     = { navController.navigate(Routes.TIMETABLE) }
                )
            }
            composable(Routes.SESSIONS) {
                SessionManagementScreen(
                    onBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Track.route) { popUpTo(0) }
                        }
                    }
                )
            }
            // ── Single dynamic task route ────────────────────────────────────
            composable(
                route = Routes.TASKS,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStackEntry ->
                val rawType = backStackEntry.arguments?.getString("type") ?: "OTHER"
                val taskType = TaskType.fromString(rawType)   // safe — unknown → OTHER
                TaskListScreen(
                    taskType = taskType,
                    onBack   = { navController.popBackStack() }
                )
            }
            composable(Routes.TIMETABLE) { TimetableScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
