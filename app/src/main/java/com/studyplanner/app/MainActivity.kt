package com.studyplanner.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.ui.AppViewModel
import com.studyplanner.app.ui.EXTRA_OPEN_TAB
import com.studyplanner.app.ui.StudyPlannerRoot
import com.studyplanner.app.ui.theme.StudyPlannerTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels { AppViewModel.Factory(application) }
    private val requestedTab = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val tab by requestedTab.collectAsStateWithLifecycle()
            StudyPlannerTheme(
                themeMode = settings.themeMode,
                accentHex = settings.accentColor,
            ) {
                StudyPlannerRoot(
                    viewModel = viewModel,
                    requestedTab = tab,
                    onTabConsumed = { requestedTab.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val tab = intent?.getStringExtra(EXTRA_OPEN_TAB) ?: return
        requestedTab.value = tab
    }
}
