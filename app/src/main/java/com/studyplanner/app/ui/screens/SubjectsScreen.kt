package com.studyplanner.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.studyplanner.app.data.ChapterEntity
import com.studyplanner.app.data.SubjectEntity
import com.studyplanner.app.ui.AppViewModel
import com.studyplanner.app.ui.components.ChapterDialog
import com.studyplanner.app.ui.components.ColorDot
import com.studyplanner.app.ui.components.ConfirmDialog
import com.studyplanner.app.ui.components.EmptyState
import com.studyplanner.app.ui.components.StudyCard
import com.studyplanner.app.ui.components.SubjectDialog
import com.studyplanner.app.ui.theme.parseHexColor

@Composable
fun SubjectsScreen(viewModel: AppViewModel) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SubjectEntity?>(null) }
    var deleting by remember { mutableStateOf<SubjectEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (subjects.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = "No subjects yet",
                        subtitle = "Add your first subject to start tracking chapters.",
                    )
                }
            }
            items(subjects, key = { it.id }) { subject ->
                val subjectChapters = chapters.filter { it.subjectId == subject.id }
                SubjectCard(
                    subject = subject,
                    chapters = subjectChapters,
                    onEdit = { editing = subject },
                    onDelete = { deleting = subject },
                    onToggleChapter = viewModel::toggleChapter,
                    onDeleteChapter = viewModel::deleteChapter,
                    onMoveChapter = { chapterId, direction ->
                        viewModel.moveChapter(chapterId, subject.id, direction)
                    },
                    onAddChapter = { name -> viewModel.addChapter(subject.id, name) },
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("Subject") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }

    if (showAdd) {
        SubjectDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { name, color ->
                viewModel.addSubject(name, color)
                showAdd = false
            },
        )
    }

    editing?.let { subject ->
        SubjectDialog(
            initial = subject,
            onDismiss = { editing = null },
            onSave = { name, color ->
                viewModel.editSubject(subject.id, name, color)
                editing = null
            },
        )
    }

    deleting?.let { subject ->
        ConfirmDialog(
            title = "Delete subject?",
            message = "Deleting \"${subject.name}\" also removes its chapters and study plans. This cannot be undone.",
            onConfirm = {
                viewModel.deleteSubject(subject.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectEntity,
    chapters: List<ChapterEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleChapter: (String) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onMoveChapter: (chapterId: String, direction: Int) -> Unit,
    onAddChapter: (name: String) -> Unit,
) {
    var collapsed by rememberSaveable(subject.id) { mutableStateOf(true) }
    var showChapterDialog by remember { mutableStateOf(false) }
    var chapterToDelete by remember { mutableStateOf<ChapterEntity?>(null) }
    val sorted = remember(chapters) { chapters.sortedBy { it.order } }
    val percent = if (sorted.isEmpty()) 0 else (sorted.count { it.completed } * 100 / sorted.size)
    val subjectColor = parseHexColor(subject.color)

    StudyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(subject.color, size = 14.dp)
            Spacer(Modifier.size(10.dp))
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { collapsed = !collapsed }) {
                Icon(
                    imageVector = if (collapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                    contentDescription = if (collapsed) "Expand chapters" else "Collapse chapters",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit subject",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete subject",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$percent% · ${sorted.count { it.completed }}/${sorted.size} chapters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = subjectColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        if (!collapsed) {
            Spacer(Modifier.height(12.dp))
            if (sorted.isEmpty()) {
                Text(
                    "No chapters yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            sorted.forEachIndexed { index, chapter ->
                ChapterRow(
                    chapter = chapter,
                    isFirst = index == 0,
                    isLast = index == sorted.lastIndex,
                    onToggle = { onToggleChapter(chapter.id) },
                    onDelete = { chapterToDelete = chapter },
                    onMoveUp = { onMoveChapter(chapter.id, -1) },
                    onMoveDown = { onMoveChapter(chapter.id, 1) },
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showChapterDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                Text("Add Chapter")
            }
        }
    }

    if (showChapterDialog) {
        ChapterDialog(
            onDismiss = { showChapterDialog = false },
            onSave = {
                onAddChapter(it)
                showChapterDialog = false
            },
        )
    }

    chapterToDelete?.let { chapter ->
        ConfirmDialog(
            title = "Delete chapter?",
            message = "Delete \"${chapter.name}\" and any study plans for it?",
            onConfirm = {
                onDeleteChapter(chapter.id)
                chapterToDelete = null
            },
            onDismiss = { chapterToDelete = null },
        )
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Outlined.KeyboardArrowUp,
                    contentDescription = "Move chapter up",
                    tint = if (isFirst) MaterialTheme.colorScheme.outlineVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = !isLast,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Move chapter down",
                    tint = if (isLast) MaterialTheme.colorScheme.outlineVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        IconButton(onClick = onToggle, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = if (chapter.completed) Icons.Outlined.CheckCircle
                else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (chapter.completed) "Mark as not completed" else "Mark as completed",
                tint = if (chapter.completed) Color(0xFF22C55E)
                else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = chapter.name,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (chapter.completed) TextDecoration.LineThrough else TextDecoration.None,
            color = if (chapter.completed) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete chapter",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}
