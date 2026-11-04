package com.studyplanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.ui.AppViewModel
import com.studyplanner.app.ui.StudyPlannerRoot
import com.studyplanner.app.ui.theme.StudyPlannerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels { AppViewModel.Factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            StudyPlannerTheme(
                themeMode = settings.themeMode,
                accentHex = settings.accentColor,
            ) {
                StudyPlannerRoot(viewModel = viewModel)
            }
        }
    }
}
