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
import com.studyplanner.app.data.AppSettings
import com.studyplanner.app.data.FocusSessionManager
import com.studyplanner.app.data.SettingsRepository
import com.studyplanner.app.data.StudyDatabase
import com.studyplanner.app.data.StudyRepository
import com.studyplanner.app.data.ThemeMode
import com.studyplanner.app.data.UpdateChecker
import com.studyplanner.app.data.UpdateInfo
import com.studyplanner.app.notify.NotificationScheduler
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

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository = StudyRepository(StudyDatabase.get(application), application)
    private val settingsRepository = SettingsRepository(application)
    private val updatePrefs = application.getSharedPreferences(
        "study_planner_updates",
        Context.MODE_PRIVATE,
    )

    val subjects = repository.subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val chapters = repository.chapters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val plans = repository.plans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val focusSessions = repository.focusSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    val activeSession: StateFlow<ActiveSession?> = FocusSessionManager.session

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
            FocusSessionManager.session.collect { updateTicker() }
        }
        checkForUpdates()
    }

    private fun updateTicker() {
        if (FocusSessionManager.session.value?.isRunning == true) {
            if (ticker?.isActive != true) {
                ticker = viewModelScope.launch {
                    while (isActive) {
                        _now.value = System.currentTimeMillis()
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

    // ----- Focus session (stopwatch) -----

    fun startFocusSession(
        subjectId: String,
        chapterId: String,
        subjectName: String,
        chapterName: String,
        customLabel: String,
    ) {
        FocusSessionManager.start(
            subjectId = subjectId,
            chapterId = chapterId,
            subjectName = subjectName,
            chapterName = chapterName,
            customLabel = customLabel,
        )
        _now.value = System.currentTimeMillis()
        savedMessage.value = null
    }

    fun pauseFocusSession() {
        FocusSessionManager.pause()
        _now.value = System.currentTimeMillis()
    }

    fun resumeFocusSession() {
        FocusSessionManager.resume()
        _now.value = System.currentTimeMillis()
    }

    fun discardFocusSession() {
        FocusSessionManager.discard()
    }

    /**
     * Saves the running/paused stopwatch as a confirmed plan and records it in focus history.
     * Minimum duration is one minute, matching the web app.
     */
    fun saveFocusSession(onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val summary = FocusSessionManager.stopAndSave() ?: return@launch
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
        const val MIN_SAVE_MS = FocusSessionManager.MIN_SAVE_MS
        private const val KEY_UPDATE_CHECK = "last_check"
        private const val UPDATE_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
