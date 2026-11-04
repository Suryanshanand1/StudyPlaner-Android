package com.studyplanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.data.ActiveSession
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

    var subjectId by rememberSaveable { mutableStateOf("") }
    var chapterId by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var autoLabel by rememberSaveable { mutableStateOf("") }
    var quickAdd by remember { mutableStateOf(false) }
    var savedLabel by remember { mutableStateOf<String?>(null) }

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
                    ActiveSessionHeader(session = active)
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "Stopwatch",
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
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("Start stopwatch", fontWeight = FontWeight.SemiBold)
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
}

@Composable
private fun ActiveSessionHeader(session: ActiveSession) {
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
        ColorDot("#F59E0B", size = 9.dp)
        Spacer(Modifier.size(8.dp))
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
