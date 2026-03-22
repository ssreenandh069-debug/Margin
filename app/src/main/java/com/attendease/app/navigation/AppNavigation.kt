package com.attendease.app.navigation

import androidx.compose.animation.*
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
import androidx.navigation.compose.*
import com.attendease.app.data.TaskType
import com.attendease.app.ui.screens.*
import com.attendease.app.ui.theme.NeonTeal
import com.attendease.app.ui.theme.NeonTealAlpha20
import com.attendease.app.ui.theme.TextSecondary
import com.attendease.app.ui.viewmodel.SessionViewModel

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

// ── Secondary routes (no bottom nav) ──────────────────────────────────
object Routes {
    const val SESSIONS   = "sessions"
    const val TASKS_ASSIGNMENTS   = "tasks/assignment"
    const val TASKS_PRESENTATIONS = "tasks/presentation"
    const val TASKS_PRACTICALS    = "tasks/practical"
    const val TIMETABLE  = "timetable"
}

val bottomNavItems = listOf(Screen.Track, Screen.Schedule, Screen.Remind)

@Composable
fun MainScaffold(
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    sessionViewModel: SessionViewModel = com.attendease.app.utils.getAppViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomNavVisible = currentRoute in listOf(
        Screen.Track.route, Screen.Schedule.route, Screen.Remind.route
    )

    val sessions by sessionViewModel.sessionsList.collectAsState()
    
    LaunchedEffect(sessions.isEmpty()) {
        if (sessions.isEmpty() && currentRoute != Routes.SESSIONS) {
            navController.navigate(Routes.SESSIONS) {
                popUpTo(0)
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
            startDestination = Screen.Track.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn() + slideInHorizontally { it / 6 } },
            exitTransition   = { fadeOut() + slideOutHorizontally { -it / 6 } },
            popEnterTransition  = { fadeIn() + slideInHorizontally { -it / 6 } },
            popExitTransition   = { fadeOut() + slideOutHorizontally { it / 6 } }
        ) {
            composable(Screen.Track.route)    { TrackScreen() }
            composable(Screen.Schedule.route) { ScheduleScreen() }
            composable(Screen.Remind.route)   {
                DashboardScreen(
                    isDarkTheme          = isDarkTheme,
                    onToggleTheme        = onToggleTheme,
                    onOpenSessions       = { navController.navigate(Routes.SESSIONS) },
                    onOpenAssignments    = { navController.navigate(Routes.TASKS_ASSIGNMENTS) },
                    onOpenPresentations  = { navController.navigate(Routes.TASKS_PRESENTATIONS) },
                    onOpenPracticals     = { navController.navigate(Routes.TASKS_PRACTICALS) },
                    onOpenTimetable      = { navController.navigate(Routes.TIMETABLE) }
                )
            }
            composable(Routes.SESSIONS) { 
                SessionManagementScreen(
                    onBack = { 
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Track.route) {
                                popUpTo(0)
                            }
                        }
                    }
                ) 
            }
            composable(Routes.TASKS_ASSIGNMENTS)   { TaskListScreen(TaskType.ASSIGNMENT,   onBack = { navController.popBackStack() }) }
            composable(Routes.TASKS_PRESENTATIONS) { TaskListScreen(TaskType.PRESENTATION, onBack = { navController.popBackStack() }) }
            composable(Routes.TASKS_PRACTICALS)    { TaskListScreen(TaskType.PRACTICAL,    onBack = { navController.popBackStack() }) }
            composable(Routes.TIMETABLE)  { TimetableScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
