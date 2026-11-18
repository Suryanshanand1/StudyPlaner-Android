package com.studyplanner.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.data.ActiveSession
import com.studyplanner.app.focus.DndController
import com.studyplanner.app.ui.AppViewModel
import com.studyplanner.app.ui.components.ColorDot
import com.studyplanner.app.ui.components.DropdownField
import com.studyplanner.app.ui.components.QuickSubjectDialog
import com.studyplanner.app.ui.components.SectionTitle
import com.studyplanner.app.ui.components.StudyCard
import com.studyplanner.app.ui.theme.TimerFontFamily
import com.studyplanner.app.util.Dates
import java.util.Locale

@Composable
fun FocusScreen(viewModel: AppViewModel) {
    val session by viewModel.activeSession.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val history by viewModel.focusSessions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var subjectId by rememberSaveable { mutableStateOf("") }
    var chapterId by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var autoLabel by rememberSaveable { mutableStateOf("") }
    var quickAdd by remember { mutableStateOf(false) }
    var savedLabel by remember { mutableStateOf<String?>(null) }

    var showAppPicker by remember { mutableStateOf(false) }
    var soundImportFailed by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var dndGranted by remember { mutableStateOf(DndController.isGranted(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessibilityEnabled = isAccessibilityServiceEnabled(context)
        dndGranted = DndController.isGranted(context)
    }

    val soundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importFocusSound(uri) { ok -> soundImportFailed = !ok }
        }
    }

    val selectedSubject = subjects.firstOrNull { it.id == subjectId }
    val selectedChapter = chapters.firstOrNull { it.id == chapterId }
    val chapterOptions = chapters
        .filter { it.subjectId == subjectId }
        .sortedBy { it.order }
        .map { it.id to it.name }

    LaunchedEffect(selectedSubject?.name, selectedChapter?.name) {
        val auto = listOfNotNull(
            selectedSubject?.name?.takeIf { it.isNotBlank() },
            selectedChapter?.name?.takeIf { it.isNotBlank() },
        ).joinToString(" — ")
        if (auto.isNotBlank() && (label.isBlank() || label == autoLabel)) {
            label = auto
            autoLabel = auto
        } else if (auto.isBlank() && (label == autoLabel || label.isBlank())) {
            label = ""
            autoLabel = ""
        }
    }

    val elapsed = session?.elapsed(now) ?: 0L
    val canSave = session != null && elapsed >= AppViewModel.MIN_SAVE_MS
    val deepFocus = settings.deepFocusMode

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            DeepFocusToggleCard(
                enabled = settings.deepFocusMode,
                sessionActive = session != null,
                onChange = viewModel::setDeepFocusMode,
            )
        }

        if (session == null) {
            item {
                StudyCard {
                    SectionTitle(text = "What are you studying?")
                    Spacer(Modifier.height(12.dp))
                    DropdownField(
                        label = "Subject",
                        selectedLabel = selectedSubject?.name.orEmpty(),
                        options = buildList {
                            add("" to "No subject (use label only)")
                            subjects.forEach { add(it.id to it.name) }
                        },
                        onSelected = {
                            subjectId = it
                            chapterId = ""
                        },
                        placeholder = "No subject",
                    )
                    if (subjectId.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        DropdownField(
                            label = "Chapter",
                            selectedLabel = selectedChapter?.name.orEmpty(),
                            options = chapterOptions,
                            onSelected = { chapterId = it },
                            placeholder = if (chapterOptions.isEmpty()) "No chapters yet" else "Select chapter",
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { quickAdd = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(5.dp))
                        Text("Quick add subject & chapter")
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Session label") },
                        placeholder = { Text("e.g. Physics — Thermodynamics") },
                        singleLine = true,
                    )
                }
            }
        }

        item {
            StudyCard {
                session?.let { active ->
                    ActiveSessionHeader(session = active, deepFocus = deepFocus)
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (deepFocus) Icons.Outlined.Shield else Icons.Outlined.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        if (deepFocus) "Deep focus stopwatch" else "Stopwatch",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = Dates.clock(elapsed),
                    fontFamily = TimerFontFamily,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        session == null -> "Start to time any study session"
                        session?.isRunning == true -> "${Dates.durationLabel(elapsed)} elapsed"
                        else -> "Paused"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(18.dp))

                if (session == null) {
                    Button(
                        onClick = {
                            viewModel.startFocusSession(
                                subjectId = subjectId,
                                chapterId = chapterId,
                                subjectName = selectedSubject?.name.orEmpty(),
                                chapterName = selectedChapter?.name.orEmpty(),
                                customLabel = label,
                            )
                            savedLabel = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            if (deepFocus) Icons.Outlined.Lock else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(7.dp))
                        Text(
                            if (deepFocus) "Start deep focus" else "Start stopwatch",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (session?.isRunning == true) {
                            OutlinedButton(
                                onClick = viewModel::pauseFocusSession,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Outlined.Pause, contentDescription = null, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Pause")
                            }
                        } else {
                            Button(
                                onClick = viewModel::resumeFocusSession,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Resume")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.discardFocusSession()
                                savedLabel = null
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Discard session", modifier = Modifier.size(19.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveFocusSession { summary -> savedLabel = summary }
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text(
                            text = if (canSave) {
                                "Save Session (${Dates.durationLabel(elapsed)})"
                            } else {
                                "Save Session — min 1 min"
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (elapsed < AppViewModel.MIN_SAVE_MS) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Study for at least 1 minute before saving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (deepFocus) {
            item {
                StrictModeCard(
                    settings = settings,
                    dndGranted = dndGranted,
                    onStrictChange = viewModel::setDeepFocusStrict,
                    onKeepScreenOnChange = viewModel::setKeepScreenOn,
                    onDndChange = viewModel::setDnd,
                    onConfirmStopChange = viewModel::setConfirmStop,
                    onOpenDndSettings = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        }
                    },
                )
            }

            item {
                FocusSoundCard(
                    settings = settings,
                    onSoundEnabledChange = viewModel::setSoundEnabled,
                    onSelectBuiltin = viewModel::selectBuiltinSound,
                    onImport = { soundLauncher.launch(arrayOf("audio/*")) },
                    onRemove = viewModel::removeFocusSound,
                    onVolumeChange = viewModel::setSoundVolume,
                )
            }

            item {
                BlockingCard(
                    settings = settings,
                    accessibilityEnabled = accessibilityEnabled,
                    onBlockingChange = viewModel::setBlockingEnabled,
                    onOpenAccessibility = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    },
                    onPickApps = { showAppPicker = true },
                    onRemovePackage = viewModel::toggleBlockedPackage,
                    onClearPackages = viewModel::clearBlockedPackages,
                )
            }

            item {
                YouTubeCard(
                    settings = settings,
                    onModeChange = viewModel::setYoutubeMode,
                    onAddChannel = viewModel::addYoutubeChannel,
                    onRemoveChannel = viewModel::removeYoutubeChannel,
                )
            }
        }

        savedLabel?.let { summary ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF22C55E).copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(
                                "Session saved to schedule",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF15803D),
                            )
                            Text(
                                summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF16A34A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                StudyCard {
                    SectionTitle(text = "Recent focus sessions")
                    Spacer(Modifier.height(10.dp))
                    history.take(5).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ColorDot("#F59E0B", size = 8.dp)
                            Spacer(Modifier.size(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${Dates.formatDate(entry.date)} · ${Dates.formatTime(entry.startTime)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = if (entry.minutes >= 60) {
                                    String.format(
                                        Locale.US,
                                        "%dh %dm",
                                        entry.minutes / 60,
                                        entry.minutes % 60,
                                    )
                                } else {
                                    "${entry.minutes}m"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }

    if (quickAdd) {
        QuickSubjectDialog(
            viewModel = viewModel,
            onDismiss = { quickAdd = false },
            onCreated = { sid, cid ->
                subjectId = sid
                chapterId = cid
                quickAdd = false
            },
        )
    }

    if (showAppPicker) {
        AppPickerDialog(
            selected = settings.blockedPackages,
            onToggle = viewModel::toggleBlockedPackage,
            onDismiss = { showAppPicker = false },
        )
    }

    if (soundImportFailed) {
        AlertDialog(
            onDismissRequest = { soundImportFailed = false },
            title = { Text("Could not import audio") },
            text = { Text("That file could not be read. Try a different audio file.") },
            confirmButton = {
                TextButton(onClick = { soundImportFailed = false }) { Text("OK") }
            },
        )
    }
}

/**
 * The single switch that turns the ordinary stopwatch into Deep Focus. Its sub-settings only
 * appear while it is on, and toggling it during a session applies or removes the effects.
 */
@Composable
private fun DeepFocusToggleCard(
    enabled: Boolean,
    sessionActive: Boolean,
    onChange: (Boolean) -> Unit,
) {
    StudyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.SelfImprovement,
                        contentDescription = null,
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Deep Focus",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when {
                        enabled && sessionActive -> "Active — strict timer, sounds and blocking follow this session"
                        enabled -> "Strict timer, focus sounds and app blocking"
                        else -> "Switch on to make this session harder to leave"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(8.dp))
            Switch(checked = enabled, onCheckedChange = onChange)
        }
        if (enabled) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DeepFeatureHint("Strict timer")
                DeepFeatureHint("Sounds")
                DeepFeatureHint("Blocking")
            }
        }
    }
}

@Composable
private fun DeepFeatureHint(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.size(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActiveSessionHeader(session: ActiveSession, deepFocus: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (deepFocus) {
            Icon(
                Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.size(6.dp))
        } else {
            ColorDot("#F59E0B", size = 9.dp)
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text = session.label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (session.isRunning) "Studying" else "Paused",
            style = MaterialTheme.typography.labelSmall,
            color = if (session.isRunning) Color(0xFF16A34A)
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
