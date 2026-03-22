package com.attendease.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendease.app.AttendEaseApplication
import com.attendease.app.ui.viewmodel.SessionViewModel
import com.attendease.app.ui.viewmodel.TaskViewModel
import com.attendease.app.ui.viewmodel.TimetableViewModel
import com.attendease.app.ui.viewmodel.TrackViewModel

@Composable
inline fun <reified T : ViewModel> getAppViewModel(): T {
    val context = LocalContext.current
    val repository = (context.applicationContext as AttendEaseApplication).repository
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V {
            return when (modelClass) {
                TrackViewModel::class.java -> TrackViewModel(repository) as V
                TaskViewModel::class.java -> TaskViewModel(repository) as V
                SessionViewModel::class.java -> SessionViewModel(repository) as V
                TimetableViewModel::class.java -> TimetableViewModel(repository) as V
                else -> throw java.lang.IllegalArgumentException("Unknown ViewModel class: \${modelClass.name}")
            }
        }
    }
    return viewModel(factory = factory)
}
