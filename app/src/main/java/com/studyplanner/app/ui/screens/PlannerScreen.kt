package com.studyplanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.data.StudyPlanEntity
import com.studyplanner.app.data.SubjectEntity
import com.studyplanner.app.ui.AppViewModel
import com.studyplanner.app.ui.components.ColorDot
import com.studyplanner.app.ui.components.PlanDialog
import com.studyplanner.app.ui.components.StudyCard
import com.studyplanner.app.util.Dates
import java.time.LocalDate

private enum class PlannerView { DAY, WEEK }

@Composable
fun PlannerScreen(viewModel: AppViewModel) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    var view by rememberSaveable { mutableStateOf(PlannerView.DAY) }
    var currentDate by rememberSaveable { mutableStateOf(Dates.today()) }
    var showPlanDialog by remember { mutableStateOf(false) }
    var deletingPlan by remember { mutableStateOf<StudyPlanEntity?>(null) }

    val date = Dates.parseDate(currentDate) ?: LocalDate.now()
    val today = Dates.today()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp),
                    )
                    .padding(3.dp),
            ) {
                ViewToggle(
                    label = "Day",
                    selected = view == PlannerView.DAY,
                    onClick = { view = PlannerView.DAY },
                )
                ViewToggle(
                    label = "Week",
                    selected = view == PlannerView.WEEK,
                    onClick = { view = PlannerView.WEEK },
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { showPlanDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                Text("New Plan")
            }
        }

        if (view == PlannerView.DAY) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { currentDate = date.minusDays(1).toString() }) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous day")
                        }
                        Text(
                            text = Dates.formatDate(currentDate),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        IconButton(onClick = { currentDate = date.plusDays(1).toString() }) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Next day")
                        }
                    }
                    if (currentDate != today) {
                        TextButton(
                            onClick = { currentDate = today },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Jump to Today")
                        }
                    }
                }

                val dayPlans = plans.filter { it.date == currentDate }.sortedBy { it.startTime }
                if (dayPlans.isEmpty()) {
                    item {
                        StudyCard {
                            Text(
                                "No study plans for this day.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { showPlanDialog = true }) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Add a plan for this day")
                            }
                        }
                    }
                }
                items(dayPlans, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        colorHex = subjects.firstOrNull { it.id == plan.subjectId }?.color ?: "#3B82F6",
                        onToggle = { viewModel.togglePlanConfirmed(plan.id) },
                        onDelete = { deletingPlan = plan },
                    )
                }
            }
        } else {
            val weekDays = Dates.weekDays(date)
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { currentDate = date.minusWeeks(1).toString() }) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous week")
                        }
                        Text(
                            text = "${Dates.formatDate(weekDays.first().toString())} – " +
                                Dates.formatDate(weekDays.last().toString()),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(onClick = { currentDate = date.plusWeeks(1).toString() }) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Next week")
                        }
                    }
                    if (today !in weekDays.map { it.toString() }) {
                        TextButton(
                            onClick = { currentDate = today },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Jump to This Week") }
                    }
                }

                items(weekDays, key = { it.toString() }) { day ->
                    val dayPlans = plans.filter { it.date == day.toString() }
                        .sortedBy { it.startTime }
                    WeekDayCard(
                        day = day,
                        isToday = day.toString() == today,
                        plans = dayPlans,
                        subjects = subjects,
                        onToggle = viewModel::togglePlanConfirmed,
                        onDelete = { deletingPlan = it },
                    )
                }
            }
        }
    }

    if (showPlanDialog) {
        PlanDialog(
            viewModel = viewModel,
            subjects = subjects,
            chapters = chapters,
            initialDate = currentDate,
            onDismiss = { showPlanDialog = false },
        )
    }

    deletingPlan?.let { plan ->
        com.studyplanner.app.ui.components.ConfirmDialog(
            title = "Delete plan?",
            message = "Remove ${plan.subjectName} — ${plan.chapterName} from your schedule?",
            onConfirm = {
                viewModel.deletePlan(plan.id)
                deletingPlan = null
            },
            onDismiss = { deletingPlan = null },
        )
    }
}

@Composable
private fun ViewToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun PlanCard(
    plan: StudyPlanEntity,
    colorHex: String,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    StudyCard(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(colorHex, size = 12.dp)
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${plan.subjectName} — ${plan.chapterName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (plan.confirmed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (plan.confirmed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${Dates.formatTime(plan.startTime)} – ${Dates.formatTime(plan.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = if (plan.confirmed) Icons.Outlined.CheckCircle
                    else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (plan.confirmed) "Mark as not studied" else "Mark as studied",
                    tint = if (plan.confirmed) Color(0xFF22C55E)
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(21.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete plan",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        if (plan.confirmed) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Confirmed study time",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF16A34A),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun WeekDayCard(
    day: LocalDate,
    isToday: Boolean,
    plans: List<StudyPlanEntity>,
    subjects: List<SubjectEntity>,
    onToggle: (String) -> Unit,
    onDelete: (StudyPlanEntity) -> Unit,
) {
    StudyCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Dates.formatShortDay(day),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isToday) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (plans.isEmpty()) {
                Text(
                    "No plans",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        plans.forEach { plan ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorDot(
                    subjects.firstOrNull { it.id == plan.subjectId }?.color ?: "#3B82F6",
                    size = 8.dp,
                )
                Spacer(Modifier.size(7.dp))
                Text(
                    text = "${plan.subjectName} — ${plan.chapterName}",
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = if (plan.confirmed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (plan.confirmed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Dates.formatTime(plan.startTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                IconButton(onClick = { onToggle(plan.id) }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = if (plan.confirmed) Icons.Outlined.CheckCircle
                        else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (plan.confirmed) Color(0xFF22C55E)
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = { onDelete(plan) }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete plan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
        if (plans.isEmpty()) {
            Spacer(Modifier.height(2.dp))
        }
    }
}
