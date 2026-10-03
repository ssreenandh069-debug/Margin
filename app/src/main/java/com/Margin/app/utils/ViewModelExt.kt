package com.Margin.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Margin.app.MarginApplication
import com.Margin.app.ui.viewmodel.AttendanceViewModel
import com.Margin.app.ui.viewmodel.TaskViewModel

@Composable
inline fun <reified T : ViewModel> getAppViewModel(): T {
    val context = LocalContext.current
    val app = context.applicationContext as MarginApplication
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V {
            return when {
                modelClass.isAssignableFrom(AttendanceViewModel::class.java) ->
                    AttendanceViewModel(app.repository, app.userPreferencesRepository) as V
                modelClass.isAssignableFrom(TaskViewModel::class.java) ->
                    TaskViewModel(app.repository) as V
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
    return viewModel(factory = factory)
}
