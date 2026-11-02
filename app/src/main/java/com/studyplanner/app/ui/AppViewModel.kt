package com.studyplanner.app.ui

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studyplanner.app.BuildConfig
import com.studyplanner.app.data.ActiveSession
import com.studyplanner.app.data.ActiveSessionStore
import com.studyplanner.app.data.AppSettings
import com.studyplanner.app.data.FocusMode
import com.studyplanner.app.data.IncomingSession
import com.studyplanner.app.data.SettingsRepository
import com.studyplanner.app.data.StudyDatabase
import com.studyplanner.app.data.StudyRepository
import com.studyplanner.app.data.ThemeMode
import com.studyplanner.app.data.TimerState
import com.studyplanner.app.data.UpdateChecker
import com.studyplanner.app.data.UpdateInfo
import com.studyplanner.app.notify.NotificationScheduler
import com.studyplanner.app.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository = StudyRepository(StudyDatabase.get(application), application)
    private val settingsRepository = SettingsRepository(application)
    private val activeSessionStore = ActiveSessionStore(application)
    private val updatePrefs = application.getSharedPreferences(
        "study_planner_updates",
        Context.MODE_PRIVATE,
    )

    val subjects = repository.subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val chapters = repository.chapters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val plans = repository.plans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val focusSessions = repository.focusSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    private val _activeSession = MutableStateFlow(activeSessionStore.load())
    val activeSession: StateFlow<ActiveSession?> = _activeSession.asStateFlow()

    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    val savedMessage = MutableStateFlow<String?>(null)

    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate: StateFlow<UpdateInfo?> = _availableUpdate.asStateFlow()

    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            repository.plans.collect { currentPlans ->
                withContext(Dispatchers.Default) {
                    NotificationScheduler.syncPlans(app, currentPlans)
                }
            }
        }
        viewModelScope.launch {
            _activeSession.collect { session ->
                activeSessionStore.save(session)
                updateTicker()
            }
        }
        checkForUpdates()
    }

    private fun updateTicker() {
        if (_activeSession.value?.isRunning == true) {
            if (ticker?.isActive != true) {
                ticker = viewModelScope.launch {
                    while (isActive) {
                        _now.value = System.currentTimeMillis()
                        checkTimerFinished()
                        delay(250)
                    }
                }
            }
        } else {
            ticker?.cancel()
            ticker = null
            _now.value = System.currentTimeMillis()
        }
    }

    private fun checkTimerFinished() {
        val session = _activeSession.value ?: return
        if (session.mode != FocusMode.TIMER || !session.isRunning || session.finished) return
        if (session.elapsed(_now.value) >= session.durationMs) {
            _activeSession.value = session.copy(
                accumulatedMs = session.durationMs,
                segmentStartedAt = _now.value,
                state = TimerState.PAUSED,
                finished = true,
            )
        }
    }

    fun consumeSavedMessage() {
        savedMessage.value = null
    }

    // ----- Settings -----

    fun setTheme(mode: ThemeMode) = settingsRepository.setTheme(mode)

    fun setAccent(color: String) = settingsRepository.setAccent(color)

    // ----- Subjects & chapters -----

    fun addSubject(name: String, color: String, onCreated: (String) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.addSubject(name, color)
            onCreated(id)
        }
    }

    fun editSubject(id: String, name: String, color: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.editSubject(id, name, color) }
    }

    fun deleteSubject(id: String) {
        viewModelScope.launch { repository.deleteSubject(id) }
    }

    fun addChapter(subjectId: String, name: String, onCreated: (String) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.addChapter(subjectId, name)
            onCreated(id)
        }
    }

    fun toggleChapter(id: String) {
        viewModelScope.launch { repository.toggleChapter(id) }
    }

    fun deleteChapter(id: String) {
        viewModelScope.launch { repository.deleteChapter(id) }
    }

    fun moveChapter(chapterId: String, subjectId: String, direction: Int) {
        viewModelScope.launch { repository.moveChapter(chapterId, subjectId, direction) }
    }

    fun createSubjectWithChapter(
        name: String,
        color: String,
        chapterName: String,
        onCreated: (subjectId: String, chapterId: String) -> Unit,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val subjectId = repository.addSubject(name, color)
            val chapterId = repository.addChapter(subjectId, chapterName.ifBlank { "Revision" })
            onCreated(subjectId, chapterId)
        }
    }

    // ----- Plans -----

    fun addPlan(
        subjectId: String,
        chapterId: String,
        subjectName: String,
        chapterName: String,
        date: String,
        startTime: String,
        endTime: String,
        confirmed: Boolean = false,
        source: String = "planner",
    ) {
        viewModelScope.launch {
            repository.addPlan(
                subjectId = subjectId,
                chapterId = chapterId,
                subjectName = subjectName,
                chapterName = chapterName,
                date = date,
                startTime = startTime,
                endTime = endTime,
                confirmed = confirmed,
                source = source,
            )
        }
    }

    fun deletePlan(id: String) {
        NotificationScheduler.cancelPlanAlarm(app, id)
        viewModelScope.launch { repository.deletePlan(id) }
    }

    fun togglePlanConfirmed(id: String) {
        viewModelScope.launch { repository.togglePlanConfirmed(id) }
    }

    fun saveIncomingSession(
        incoming: IncomingSession,
        subjectId: String,
        chapterId: String,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            // Read once so the names match the ids even if the dialog was opened a while ago.
            val dao = StudyDatabase.get(app).dao()
            val subjects = withContext(Dispatchers.IO) { dao.getSubjects() }
            val chapters = withContext(Dispatchers.IO) { dao.getChapters() }
            val subject = subjects.firstOrNull { it.id == subjectId }
            val chapter = chapters.firstOrNull { it.id == chapterId }
            repository.addPlan(
                subjectId = subjectId,
                chapterId = chapterId,
                subjectName = subject?.name ?: "Allies",
                chapterName = chapter?.name ?: incoming.label,
                date = incoming.date,
                startTime = incoming.startTime,
                endTime = incoming.endTime,
                confirmed = true,
                source = "allies",
            )
            onDone()
        }
    }

    // ----- Focus session -----

    fun startFocusSession(
        mode: FocusMode,
        durationMs: Long,
        subjectId: String,
        chapterId: String,
        subjectName: String,
        chapterName: String,
        customLabel: String,
    ) {
        val timestamp = System.currentTimeMillis()
        val chapterLabel = when {
            chapterName.isNotBlank() -> chapterName
            customLabel.isNotBlank() -> customLabel
            subjectName.isNotBlank() -> "Revision"
            else -> "Focus session"
        }
        val session = ActiveSession(
            mode = mode,
            subjectId = subjectId,
            chapterId = chapterId,
            subjectName = subjectName,
            chapterName = chapterLabel,
            durationMs = if (mode == FocusMode.TIMER) durationMs else 0L,
            startedAt = timestamp,
            segmentStartedAt = timestamp,
            accumulatedMs = 0L,
            state = TimerState.RUNNING,
            finished = false,
        )
        _now.value = timestamp
        _activeSession.value = session
        savedMessage.value = null
        NotificationScheduler.cancelFocusNotification(app)
        if (mode == FocusMode.TIMER && durationMs > 0) {
            NotificationScheduler.scheduleFocusFinish(app, durationMs, session.label)
        }
    }

    fun pauseFocusSession() {
        val session = _activeSession.value ?: return
        if (!session.isRunning) return
        val timestamp = System.currentTimeMillis()
        _activeSession.value = session.copy(
            accumulatedMs = session.elapsed(timestamp),
            segmentStartedAt = timestamp,
            state = TimerState.PAUSED,
        )
        _now.value = timestamp
        NotificationScheduler.cancelFocusFinish(app)
    }

    fun resumeFocusSession() {
        val session = _activeSession.value ?: return
        if (session.isRunning || session.finished) return
        val timestamp = System.currentTimeMillis()
        _activeSession.value = session.copy(
            segmentStartedAt = timestamp,
            state = TimerState.RUNNING,
        )
        _now.value = timestamp
        if (session.mode == FocusMode.TIMER) {
            NotificationScheduler.scheduleFocusFinish(app, session.remaining(timestamp), session.label)
        }
    }

    fun discardFocusSession() {
        NotificationScheduler.cancelFocusFinish(app)
        NotificationScheduler.cancelFocusNotification(app)
        _activeSession.value = null
    }

    /**
     * Saves the running/finished session as a confirmed plan and records it in Allies history.
     * Minimum duration is one minute, matching the web app.
     */
    fun saveFocusSession(onSaved: (String) -> Unit) {
        val session = _activeSession.value ?: return
        val timestamp = System.currentTimeMillis()
        val elapsed = session.elapsed(timestamp)
        if (elapsed < MIN_SAVE_MS) return

        val start = Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault())
        val end = Instant.ofEpochMilli(session.startedAt + elapsed).atZone(ZoneId.systemDefault())
        val date = start.toLocalDate().toString()
        val startTime = Dates.toTimeStr(start.toLocalDateTime())
        val endTime = Dates.toTimeStr(end.toLocalDateTime())
        val subjectName = session.subjectName.ifBlank { "Focus" }
        val chapterName = session.chapterName.ifBlank {
            session.subjectName.ifBlank { "Focus session" }
        }
        val minutes = (elapsed / 60_000.0).roundToInt().coerceAtLeast(1)

        viewModelScope.launch {
            repository.addPlan(
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
            repository.addFocusSession(
                label = session.label,
                date = date,
                startTime = startTime,
                endTime = endTime,
                minutes = minutes,
            )
            NotificationScheduler.cancelFocusFinish(app)
            NotificationScheduler.cancelFocusNotification(app)
            _activeSession.value = null
            val summary = "${session.label} (${Dates.durationLabel(elapsed)})"
            savedMessage.value = summary
            onSaved(summary)
        }
    }

    // ----- Backup & maintenance -----

    fun checkForUpdates(force: Boolean = false, onResult: (String) -> Unit = {}) {
        val now = System.currentTimeMillis()
        val lastCheck = updatePrefs.getLong(KEY_UPDATE_CHECK, 0L)
        if (!force && now - lastCheck < UPDATE_CHECK_INTERVAL_MS) return

        viewModelScope.launch {
            val latest = UpdateChecker.fetchLatest()
            if (latest == null) {
                if (force) onResult("Could not check for updates")
                return@launch
            }
            updatePrefs.edit { putLong(KEY_UPDATE_CHECK, System.currentTimeMillis()) }
            if (UpdateChecker.isNewer(latest.version, BuildConfig.VERSION_NAME)) {
                _availableUpdate.value = latest
                onResult("Version ${latest.version} is available")
            } else {
                _availableUpdate.value = null
                onResult("You're up to date")
            }
        }
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    fun exportData(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportJson()
            onReady(json)
        }
    }

    fun importData(text: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.importJson(text)
            onResult(ok)
        }
    }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearAll()
            NotificationScheduler.syncPlans(app, emptyList())
            onDone()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(application) as T
        }
    }

    companion object {
        const val MIN_SAVE_MS = 60_000L
        private const val KEY_UPDATE_CHECK = "last_check"
        private const val UPDATE_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
