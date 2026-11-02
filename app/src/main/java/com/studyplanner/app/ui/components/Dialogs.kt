package com.studyplanner.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyplanner.app.data.ChapterEntity
import com.studyplanner.app.data.IncomingSession
import com.studyplanner.app.data.SubjectEntity
import com.studyplanner.app.ui.AppViewModel
import com.studyplanner.app.util.Dates
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun SubjectDialog(
    initial: SubjectEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: SUBJECT_COLORS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Subject" else "Edit Subject") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                ColorPicker(selected = color, onSelected = { color = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), color) },
                enabled = name.isNotBlank(),
            ) {
                Text(if (initial == null) "Add Subject" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun ChapterDialog(
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chapter") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Chapter name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("Add Chapter") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun QuickSubjectDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onCreated: (subjectId: String, chapterId: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var chapterName by remember { mutableStateOf("Revision") }
    var color by remember { mutableStateOf(SUBJECT_COLORS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Subject") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = chapterName,
                    onValueChange = { chapterName = it },
                    label = { Text("Starter chapter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
                ColorPicker(selected = color, onSelected = { color = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.createSubjectWithChapter(name.trim(), color, chapterName.trim()) { sid, cid ->
                        onCreated(sid, cid)
                    }
                },
                enabled = name.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun IncomingSessionDialog(
    viewModel: AppViewModel,
    incoming: IncomingSession,
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    onDismiss: () -> Unit,
) {
    var subjectId by remember { mutableStateOf("") }
    var chapterId by remember { mutableStateOf("") }
    var quickAdd by remember { mutableStateOf(false) }

    val subjectOptions = buildList {
        add("" to "No subject (save label only)")
        subjects.forEach { add(it.id to it.name) }
    }
    val chapterOptions = chapters
        .filter { it.subjectId == subjectId }
        .sortedBy { it.order }
        .map { it.id to it.name }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session from Allies") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "${incoming.label} · ${Dates.formatDate(incoming.date)} · " +
                        "${Dates.formatTime(incoming.startTime)}–${Dates.formatTime(incoming.endTime)}" +
                        if (incoming.minutes > 0) " (${incoming.minutes} min)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                DropdownField(
                    label = "Subject",
                    selectedLabel = subjects.firstOrNull { it.id == subjectId }?.name ?: "",
                    options = subjectOptions,
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
                        selectedLabel = chapterOptions.firstOrNull { it.first == chapterId }?.second ?: "",
                        options = chapterOptions,
                        onSelected = { chapterId = it },
                        placeholder = if (chapterOptions.isEmpty()) "No chapters yet" else "Select chapter",
                    )
                    if (chapterOptions.isEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "This subject has no chapters. You can save it with the label only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { quickAdd = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("Quick add subject")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveIncomingSession(incoming, subjectId, chapterId) { onDismiss() }
                },
            ) { Text("Save as confirmed") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Discard") }
        },
    )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDialog(
    viewModel: AppViewModel,
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    initialDate: String = Dates.today(),
    onDismiss: () -> Unit,
) {
    var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id.orEmpty()) }
    var chapterId by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(initialDate) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var quickAdd by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val chapterOptions = chapters
        .filter { it.subjectId == subjectId }
        .sortedBy { it.order }
        .map { it.id to it.name }
    val subjectName = subjects.firstOrNull { it.id == subjectId }?.name.orEmpty()
    val chapterName = chapters.firstOrNull { it.id == chapterId }?.name.orEmpty()
    val invalidTime = startTime >= endTime
    val canSave = subjectId.isNotBlank() && chapterId.isNotBlank() && !invalidTime

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Study Plan") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DropdownField(
                    label = "Subject",
                    selectedLabel = subjectName,
                    options = subjects.map { it.id to it.name },
                    onSelected = {
                        subjectId = it
                        chapterId = ""
                    },
                    placeholder = if (subjects.isEmpty()) "No subjects yet" else "Select subject",
                )
                TextButton(onClick = { quickAdd = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("Quick add subject & chapter")
                }
                DropdownField(
                    label = "Chapter",
                    selectedLabel = chapterName,
                    options = chapterOptions,
                    onSelected = { chapterId = it },
                    placeholder = if (chapterOptions.isEmpty()) "Add a chapter first" else "Select chapter",
                )
                PickerField(
                    label = "Date",
                    value = Dates.formatDate(date),
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = { showDatePicker = true },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PickerField(
                        label = "Start",
                        value = Dates.formatTime(startTime),
                        icon = Icons.Outlined.Schedule,
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                    PickerField(
                        label = "End",
                        value = Dates.formatTime(endTime),
                        icon = Icons.Outlined.Schedule,
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (invalidTime) {
                    Text(
                        "End time must be after start time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.addPlan(
                        subjectId = subjectId,
                        chapterId = chapterId,
                        subjectName = subjectName,
                        chapterName = chapterName,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                    )
                    onDismiss()
                },
                enabled = canSave,
            ) { Text("Add to Schedule") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showDatePicker) {
        val initialMillis = Dates.parseDate(date)
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showStartPicker) {
        TimeFieldDialog(
            initial = startTime,
            onDismiss = { showStartPicker = false },
            onConfirm = {
                startTime = it
                showStartPicker = false
            },
        )
    }
    if (showEndPicker) {
        TimeFieldDialog(
            initial = endTime,
            onDismiss = { showEndPicker = false },
            onConfirm = {
                endTime = it
                showEndPicker = false
            },
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFieldDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val parsed = Dates.parseTime(initial) ?: LocalTime.of(9, 0)
    val state = rememberTimePickerState(
        initialHour = parsed.hour,
        initialMinute = parsed.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm("%02d:%02d".format(state.hour, state.minute))
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
