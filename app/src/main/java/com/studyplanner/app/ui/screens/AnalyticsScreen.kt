package com.studyplanner.app.ui.screens

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
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.ui.AppViewModel
import com.studyplanner.app.ui.components.BarChart
import com.studyplanner.app.ui.components.ColorDot
import com.studyplanner.app.ui.components.SectionTitle
import com.studyplanner.app.ui.components.StudyCard
import com.studyplanner.app.ui.theme.parseHexColor
import com.studyplanner.app.util.Dates
import com.studyplanner.app.util.DayStat
import com.studyplanner.app.util.Stats
import java.time.LocalDate
import java.util.Locale

@Composable
fun AnalyticsScreen(viewModel: AppViewModel) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val focusSessions by viewModel.focusSessions.collectAsStateWithLifecycle()

    val now = LocalDate.now()
    var year by rememberSaveable { mutableIntStateOf(now.year) }
    var month by rememberSaveable { mutableIntStateOf(now.monthValue - 1) }

    val days = remember(plans, chapters, focusSessions, year, month) {
        Stats.monthly(plans, chapters, focusSessions, year, month + 1)
    }
    val totalMinutes = remember(days) { days.sumOf { it.minutes } }
    val totalSessions = remember(days) { days.sumOf { it.sessions } }
    val totalChapters = remember(days) { days.sumOf { it.chapters } }
    val studyDays = remember(days) { days.count { it.minutes > 0 || it.chapters > 0 } }
    val activeDays = remember(days) {
        days.filter { it.minutes > 0 || it.chapters > 0 || it.sessions > 0 }
    }
    val bestDay = remember(activeDays) { activeDays.maxByOrNull { it.minutes } }
    val averageMinutes = if (studyDays == 0) 0 else totalMinutes / studyDays
    val labels = remember(days) { days.map { it.date.dayOfMonth.toString() } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (month == 0) {
                    month = 11
                    year -= 1
                } else {
                    month -= 1
                }
            }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = Dates.formatMonthYear(year, month + 1),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = {
                if (month == 11) {
                    month = 0
                    year += 1
                } else {
                    month += 1
                }
            }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month")
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard(
                        icon = Icons.Outlined.Schedule,
                        value = Dates.minutesLabel(totalMinutes),
                        label = "Total time",
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCard(
                        icon = Icons.Outlined.Timer,
                        value = totalSessions.toString(),
                        label = "Sessions",
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard(
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        value = studyDays.toString(),
                        label = "Study days",
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCard(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        value = totalChapters.toString(),
                        label = "Chapters",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                StudyCard {
                    Text(
                        "Daily Study Time",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Planned sessions and saved focus time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    BarChart(
                        values = days.map { it.minutes.toFloat() },
                        labels = labels,
                        color = MaterialTheme.colorScheme.primary,
                        height = 210.dp,
                        formatValue = ::minutesAxisLabel,
                    )
                }
            }

            item {
                StudyCard {
                    SectionTitle(text = "Daily breakdown")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BreakdownMetric(
                            label = "Best day",
                            value = bestDay?.let { Dates.minutesLabel(it.minutes) } ?: "—",
                            caption = bestDay?.let { Dates.formatDate(it.date.toString()) } ?: "No data",
                            modifier = Modifier.weight(1f),
                        )
                        BreakdownMetric(
                            label = "Average / study day",
                            value = Dates.minutesLabel(averageMinutes),
                            caption = "$studyDays day${if (studyDays == 1) "" else "s"}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (activeDays.isEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "No study time recorded this month yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        activeDays.sortedByDescending { it.date }.forEach { day ->
                            DayRow(day)
                        }
                    }
                }
            }

            if (subjects.isNotEmpty()) {
                item {
                    StudyCard {
                        SectionTitle(text = "Subject progress")
                        Spacer(Modifier.height(12.dp))
                        subjects.forEach { subject ->
                            val subjectChapters = chapters.filter { it.subjectId == subject.id }
                            val percent = if (subjectChapters.isEmpty()) 0
                            else subjectChapters.count { it.completed } * 100 / subjectChapters.size
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ColorDot(subject.color, size = 10.dp)
                                Spacer(Modifier.size(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Row {
                                        Text(
                                            subject.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "$percent%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    LinearProgressIndicator(
                                        progress = { percent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = parseHexColor(subject.color),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: DayStat) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    Dates.formatDate(day.date.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    daySummary(day),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(10.dp))
            Text(
                Dates.minutesLabel(day.minutes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun daySummary(day: DayStat): String {
    val parts = mutableListOf<String>()
    parts += "${day.sessions} session${if (day.sessions == 1) "" else "s"}"
    if (day.chapters > 0) {
        parts += "${day.chapters} chapter${if (day.chapters == 1) "" else "s"}"
    }
    if (day.focusMinutes > 0) parts += "${Dates.minutesLabel(day.focusMinutes)} focus"
    if (day.plannedMinutes > 0) parts += "${Dates.minutesLabel(day.plannedMinutes)} planned"
    return parts.joinToString(" · ")
}

private fun minutesAxisLabel(value: Float): String = when {
    value <= 0f -> "0"
    value >= 60f -> String.format(Locale.US, "%.0fh", value / 60f)
    value % 1f == 0f -> "${value.toInt()}m"
    else -> String.format(Locale.US, "%.1fm", value)
}

@Composable
private fun BreakdownMetric(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    StudyCard(modifier = modifier, contentPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(19.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
