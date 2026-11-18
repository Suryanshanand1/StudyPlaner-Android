package com.studyplanner.app.data

import android.annotation.SuppressLint
import android.content.Context
import com.studyplanner.app.focus.DeepFocusManager
import com.studyplanner.app.notify.FocusNotification
import com.studyplanner.app.util.Dates
import com.studyplanner.app.widget.FocusWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Single source of truth for the live stopwatch. The in-app screen, the ongoing
 * notification actions, and the home-screen widget all read and mutate this state, so
 * they stay in sync even when only one of them is visible.
 */
object FocusSessionManager {
    const val MIN_SAVE_MS = 60_000L

    @Volatile
    private var initialized = false

    private lateinit var appContext: Context
    private lateinit var store: ActiveSessionStore

    // Holds the application context only, so a long-lived reference is safe.
    @SuppressLint("StaticFieldLeak")
    private var repository: StudyRepository? = null

    private val _session = MutableStateFlow<ActiveSession?>(null)
    val session: StateFlow<ActiveSession?> = _session.asStateFlow()

    val current: ActiveSession? get() = if (initialized) _session.value else null

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            store = ActiveSessionStore(appContext)
            repository = StudyRepository(StudyDatabase.get(appContext), appContext)
            _session.value = store.load()
            initialized = true
        }
        publish(_session.value)
        // A running session restored after a process restart should bring its sound and
        // DND state back with it, so treat it as a fresh start for Deep Focus.
        val restored = _session.value
        if (restored != null) {
            DeepFocusManager.onSessionChanged(appContext, null, restored)
        }
    }

    fun start(
        subjectId: String,
        chapterId: String,
        subjectName: String,
        chapterName: String,
        customLabel: String,
    ) {
        ensureInitialized()
        val timestamp = System.currentTimeMillis()
        val chapterLabel = when {
            chapterName.isNotBlank() -> chapterName
            customLabel.isNotBlank() -> customLabel
            subjectName.isNotBlank() -> "Revision"
            else -> "Focus session"
        }
        publish(
            ActiveSession(
                subjectId = subjectId,
                chapterId = chapterId,
                subjectName = subjectName,
                chapterName = chapterLabel,
                startedAt = timestamp,
                segmentStartedAt = timestamp,
                accumulatedMs = 0L,
                state = SessionState.RUNNING,
            ),
        )
    }

    fun pause() {
        ensureInitialized()
        val session = _session.value ?: return
        if (!session.isRunning) return
        val timestamp = System.currentTimeMillis()
        publish(
            session.copy(
                accumulatedMs = session.elapsed(timestamp),
                segmentStartedAt = timestamp,
                state = SessionState.PAUSED,
            ),
        )
    }

    fun resume() {
        ensureInitialized()
        val session = _session.value ?: return
        if (session.isRunning) return
        val timestamp = System.currentTimeMillis()
        publish(
            session.copy(
                segmentStartedAt = timestamp,
                state = SessionState.RUNNING,
            ),
        )
    }

    fun discard() {
        ensureInitialized()
        publish(null)
    }

    /**
     * Ends the session and saves it to the planner and focus history when it lasted at
     * least one minute. Shorter sessions are discarded. Returns the saved summary, or
     * null when nothing was saved.
     */
    suspend fun stopAndSave(): String? {
        ensureInitialized()
        val session = _session.value ?: return null
        val timestamp = System.currentTimeMillis()
        val elapsed = session.elapsed(timestamp)
        val save = elapsed >= MIN_SAVE_MS

        try {
            if (save) {
                val start = Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault())
                val end = Instant.ofEpochMilli(session.startedAt + elapsed)
                    .atZone(ZoneId.systemDefault())
                val date = start.toLocalDate().toString()
                val startTime = Dates.toTimeStr(start.toLocalDateTime())
                val endTime = Dates.toTimeStr(end.toLocalDateTime())
                val subjectName = session.subjectName.ifBlank { "Focus" }
                val chapterName = session.chapterName.ifBlank {
                    session.subjectName.ifBlank { "Focus session" }
                }
                val minutes = (elapsed / 60_000.0).roundToInt().coerceAtLeast(1)
                val repo = repository ?: error("FocusSessionManager is not initialized")

                repo.addPlan(
                    subjectId = session.subjectId,
                    chapterId = session.chapterId,
                    subjectName = subjectName,
                    chapterName = chapterName,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    confirmed = true,
                    source = "focus",
                )
                repo.addFocusSession(
                    label = session.label,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    minutes = minutes,
                )
            }
        } finally {
            publish(null)
        }

        return if (save) "${session.label} (${Dates.durationLabel(elapsed)})" else null
    }

    private fun publish(session: ActiveSession?) {
        val previous = _session.value
        store.save(session)
        _session.value = session
        FocusNotification.update(appContext, session)
        FocusWidgetProvider.updateAll(appContext, session)
        DeepFocusManager.onSessionChanged(appContext, previous, session)
    }

    private fun ensureInitialized() {
        check(initialized) { "Call FocusSessionManager.initialize(context) first" }
    }
}
