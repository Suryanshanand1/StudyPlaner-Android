package com.studyplanner.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val createdAt: Long,
)

@Entity(tableName = "chapters", indices = [Index("subjectId")])
data class ChapterEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val name: String,
    val completed: Boolean,
    @ColumnInfo(name = "chapter_order") val order: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "study_plans",
    indices = [Index("date"), Index("subjectId"), Index("chapterId")],
)
data class StudyPlanEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val chapterId: String,
    val subjectName: String,
    val chapterName: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val createdAt: Long,
    val confirmed: Boolean,
    val source: String = "planner",
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val label: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val minutes: Int,
    val createdAt: Long,
)

enum class SessionState { RUNNING, PAUSED }

data class ActiveSession(
    val subjectId: String,
    val chapterId: String,
    val subjectName: String,
    val chapterName: String,
    val startedAt: Long,
    val segmentStartedAt: Long,
    val accumulatedMs: Long,
    val state: SessionState,
) {
    val isRunning: Boolean get() = state == SessionState.RUNNING
    val label: String get() = when {
        subjectName.isNotBlank() && chapterName.isNotBlank() -> "$subjectName — $chapterName"
        subjectName.isNotBlank() -> subjectName
        chapterName.isNotBlank() -> chapterName
        else -> "Focus session"
    }

    fun elapsed(now: Long): Long =
        accumulatedMs + if (state == SessionState.RUNNING) (now - segmentStartedAt).coerceAtLeast(0) else 0
}
