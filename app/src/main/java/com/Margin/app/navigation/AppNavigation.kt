package com.Margin.app.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.Margin.app.ui.screens.InsightsScreen
import com.Margin.app.ui.screens.LandingScreen
import com.Margin.app.ui.screens.TaskListScreen
import com.Margin.app.ui.screens.TimetableScreen
import com.Margin.app.ui.screens.TrackScreen
import com.Margin.app.ui.theme.CardDark
import com.Margin.app.ui.theme.DarkBg
import com.Margin.app.ui.theme.NeonTeal
import com.Margin.app.ui.theme.NeonTealAlpha20
import com.Margin.app.ui.theme.TextMuted
import com.Margin.app.ui.theme.TextSecondary
import com.Margin.app.ui.viewmodel.AttendanceViewModel
import com.Margin.app.utils.getAppViewModel

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Track : Screen("track", "Track", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday)
    object Timetable : Screen("timetable", "Timetable", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Tasks : Screen("tasks", "Tasks", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment)
    object Insights : Screen("insights", "Insights", Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp)
}

val bottomNavItems = listOf(Screen.Track, Screen.Timetable, Screen.Tasks, Screen.Insights)

@Composable
fun MainScaffold(
    attendanceViewModel: AttendanceViewModel = getAppViewModel()
) {
    val activeSession by attendanceViewModel.activeSession.collectAsState()

    if (activeSession == null) {
        LandingScreen(viewModel = attendanceViewModel)
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomNavVisible = currentRoute in listOf(
        Screen.Track.route, Screen.Timetable.route, Screen.Tasks.route, Screen.Insights.route
    )

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            if (isBottomNavVisible) {
                NavigationBar(
                    containerColor = Color(0xFF101924),
                    tonalElevation = 0.dp
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
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonTeal,
                                selectedTextColor = NeonTeal,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
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
            composable(Screen.Insights.route) { InsightsScreen() }
        }
    }
}

