package com.studyplanner.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.data.IncomingSession
import com.studyplanner.app.ui.components.IncomingSessionDialog
import com.studyplanner.app.ui.screens.AnalyticsScreen
import com.studyplanner.app.ui.screens.DashboardScreen
import com.studyplanner.app.ui.screens.FocusScreen
import com.studyplanner.app.ui.screens.PlannerScreen
import com.studyplanner.app.ui.screens.SettingsScreen
import com.studyplanner.app.ui.screens.SubjectsScreen
import kotlinx.coroutines.launch

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    SUBJECTS("Subjects", Icons.AutoMirrored.Outlined.MenuBook),
    PLANNER("Planner", Icons.Outlined.CalendarMonth),
    FOCUS("Focus", Icons.Outlined.Timer),
    ANALYTICS("Analytics", Icons.Outlined.BarChart),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerRoot(
    viewModel: AppViewModel,
    incoming: IncomingSession?,
    onDismissIncoming: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    BackHandler(enabled = settingsOpen) { settingsOpen = false }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (settingsOpen) "Settings" else tab.label,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (settingsOpen) {
                        IconButton(onClick = { settingsOpen = false }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!settingsOpen) {
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (!settingsOpen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    AppTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                settingsOpen -> SettingsScreen(viewModel = viewModel, onMessage = ::showMessage)
                tab == AppTab.HOME -> DashboardScreen(
                    viewModel = viewModel,
                    onOpenPlanner = { tab = AppTab.PLANNER },
                    onStartFocus = { tab = AppTab.FOCUS },
                )
                tab == AppTab.SUBJECTS -> SubjectsScreen(viewModel)
                tab == AppTab.PLANNER -> PlannerScreen(viewModel)
                tab == AppTab.FOCUS -> FocusScreen(viewModel)
                else -> AnalyticsScreen(viewModel)
            }
        }
    }

    if (incoming != null) {
        IncomingSessionDialog(
            viewModel = viewModel,
            incoming = incoming,
            subjects = subjects,
            chapters = chapters,
            onDismiss = onDismissIncoming,
        )
    }
}
