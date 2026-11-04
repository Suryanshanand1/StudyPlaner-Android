package com.studyplanner.app.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.data.StudyPlanEntity
import com.studyplanner.app.data.UpdateInfo
import com.studyplanner.app.ui.AppViewModel
import com.studyplanner.app.ui.components.ColorDot
import com.studyplanner.app.ui.components.SectionTitle
import com.studyplanner.app.ui.components.StatCard
import com.studyplanner.app.ui.components.StudyCard
import com.studyplanner.app.util.Dates
import com.studyplanner.app.util.Stats

@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onOpenPlanner: () -> Unit,
    onStartFocus: () -> Unit,
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val focusSessions by viewModel.focusSessions.collectAsStateWithLifecycle()
    val update by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val today = Dates.today()
    val todayPlans = remember(plans, today) {
        plans.filter { it.date == today }.sortedBy { it.startTime }
    }
    val upcoming = remember(plans, today) {
        plans.filter { it.date >= today && !it.confirmed }
            .sortedWith(compareBy({ it.date }, { it.startTime }))
    }
    val todayMinutes = remember(plans, focusSessions, today) {
        Stats.minutesOn(plans, focusSessions, today)
    }
    val progress = Stats.overallProgress(chapters)
    val completedChapters = chapters.count { it.completed }
    val streak = remember(plans) { Stats.streak(plans) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "${Dates.greeting()}!",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Dates.formatDateLong(today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        update?.let { available ->
            item {
                UpdateCard(
                    update = available,
                    onUpdate = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, available.apkUrl.toUri()),
                            )
                        }
                    },
                    onDismiss = viewModel::dismissUpdate,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        label = "Subjects",
                        value = subjects.size.toString(),
                        caption = "active subjects",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        icon = Icons.Outlined.TrackChanges,
                        label = "Progress",
                        value = "$progress%",
                        caption = "$completedChapters/${chapters.size} chapters",
                        modifier = Modifier.weight(1f),
                        progress = progress / 100f,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        icon = Icons.Outlined.Schedule,
                        label = "Today",
                        value = Dates.minutesLabel(todayMinutes),
                        caption = "${todayPlans.size} session${if (todayPlans.size == 1) "" else "s"}",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        icon = Icons.Outlined.LocalFireDepartment,
                        label = "Streak",
                        value = streak.toString(),
                        caption = "consecutive days",
                        modifier = Modifier.weight(1f),
                        tint = if (streak > 0) androidx.compose.ui.graphics.Color(0xFFF97316)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            StudyCard {
                SectionTitle(
                    text = "Today's Schedule",
                    trailing = {
                        TextButton(onClick = onOpenPlanner) { Text("Planner") }
                    },
                )
                Spacer(Modifier.height(8.dp))
                if (todayPlans.isEmpty()) {
                    Text(
                        text = "Nothing scheduled for today. Plan a session to get started.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    todayPlans.take(5).forEach { plan ->
                        val subject = subjects.firstOrNull { it.id == plan.subjectId }
                        PlanRow(
                            plan = plan,
                            colorHex = subject?.color ?: "#3B82F6",
                            onToggle = { viewModel.togglePlanConfirmed(plan.id) },
                        )
                    }
                    if (todayPlans.size > 5) {
                        Text(
                            text = "+${todayPlans.size - 5} more in Planner",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        item {
            StudyCard {
                SectionTitle(text = "Upcoming")
                Spacer(Modifier.height(6.dp))
                if (upcoming.isEmpty()) {
                    Text(
                        text = "No upcoming sessions. Your future self will thank you for planning one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "${upcoming.size} study session${if (upcoming.size == 1) "" else "s"} planned ahead",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    upcoming.take(3).forEach { plan ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "${plan.subjectName} — ${plan.chapterName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${Dates.formatDate(plan.date)} · ${Dates.formatTime(plan.startTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            StudyCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Focus with the stopwatch",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Time any study session. Saved sessions count toward your streak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onStartFocus, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Start a focus session")
                }
            }
        }
    }
}

@Composable
private fun UpdateCard(
    update: UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    StudyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Version ${update.version} available",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "A newer Study Planner is ready to install.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (update.notes.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = update.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onUpdate) { Text("Update") }
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    }
}

@Composable
private fun PlanRow(
    plan: StudyPlanEntity,
    colorHex: String,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Dates.formatTime(plan.startTime),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 10.dp),
        )
        ColorDot(colorHex, size = 9.dp)
        Spacer(Modifier.size(8.dp))
        Text(
            text = "${plan.subjectName} — ${plan.chapterName}",
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (plan.confirmed) TextDecoration.LineThrough else TextDecoration.None,
            color = if (plan.confirmed) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (plan.confirmed) Icons.Outlined.CheckCircle
                else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (plan.confirmed) "Mark as not studied" else "Mark as studied",
                tint = if (plan.confirmed) androidx.compose.ui.graphics.Color(0xFF22C55E)
                else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
