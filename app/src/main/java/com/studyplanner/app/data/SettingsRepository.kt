package com.studyplanner.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class YouTubeMode { BLOCK_ALL, STUDY_CHANNELS }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: String = "#3B82F6",
    // ----- Deep Focus -----
    val deepFocusMode: Boolean = false,
    val deepFocusStrict: Boolean = false,
    val deepFocusKeepScreenOn: Boolean = true,
    val deepFocusDnd: Boolean = false,
    val deepFocusConfirmStop: Boolean = true,
    val blockingEnabled: Boolean = false,
    val blockedPackages: Set<String> = emptySet(),
    val youtubeMode: YouTubeMode = YouTubeMode.BLOCK_ALL,
    val youtubeChannels: Set<String> = emptySet(),
    val soundEnabled: Boolean = true,
    val builtinSound: String? = null,
    val soundPath: String? = null,
    val soundName: String? = null,
    val soundVolume: Float = 0.7f,
)

/**
 * Shared settings store. Kept as a process-wide singleton so the accessibility service,
 * the sound service, and the UI all observe the same values.
 */
class SettingsRepository private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings {
        val mode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        val youtubeMode = runCatching {
            YouTubeMode.valueOf(
                prefs.getString(KEY_YOUTUBE_MODE, YouTubeMode.BLOCK_ALL.name) ?: YouTubeMode.BLOCK_ALL.name,
            )
        }.getOrDefault(YouTubeMode.BLOCK_ALL)
        return AppSettings(
            themeMode = mode,
            accentColor = prefs.getString(KEY_ACCENT, "#3B82F6") ?: "#3B82F6",
            deepFocusMode = prefs.getBoolean(KEY_DEEP_MODE, false),
            deepFocusStrict = prefs.getBoolean(KEY_STRICT, false),
            deepFocusKeepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
            deepFocusDnd = prefs.getBoolean(KEY_DND, false),
            deepFocusConfirmStop = prefs.getBoolean(KEY_CONFIRM_STOP, true),
            blockingEnabled = prefs.getBoolean(KEY_BLOCKING, false),
            blockedPackages = loadSet(KEY_BLOCKED_PACKAGES),
            youtubeMode = youtubeMode,
            youtubeChannels = loadSet(KEY_YOUTUBE_CHANNELS),
            soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
            builtinSound = prefs.getString(KEY_SOUND_BUILTIN, null),
            soundPath = prefs.getString(KEY_SOUND_PATH, null),
            soundName = prefs.getString(KEY_SOUND_NAME, null),
            soundVolume = prefs.getFloat(KEY_SOUND_VOLUME, 0.7f).coerceIn(0f, 1f),
        )
    }

    fun setTheme(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.name) }
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setAccent(color: String) {
        prefs.edit { putString(KEY_ACCENT, color) }
        _settings.value = _settings.value.copy(accentColor = color)
    }

    // ----- Deep Focus -----

    fun setDeepFocusMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DEEP_MODE, enabled) }
        _settings.value = _settings.value.copy(deepFocusMode = enabled)
    }

    fun setDeepFocusStrict(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_STRICT, enabled) }
        _settings.value = _settings.value.copy(deepFocusStrict = enabled)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_KEEP_SCREEN_ON, enabled) }
        _settings.value = _settings.value.copy(deepFocusKeepScreenOn = enabled)
    }

    fun setDnd(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DND, enabled) }
        _settings.value = _settings.value.copy(deepFocusDnd = enabled)
    }

    fun setConfirmStop(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONFIRM_STOP, enabled) }
        _settings.value = _settings.value.copy(deepFocusConfirmStop = enabled)
    }

    fun setBlockingEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BLOCKING, enabled) }
        _settings.value = _settings.value.copy(blockingEnabled = enabled)
    }

    fun toggleBlockedPackage(packageName: String) {
        val updated = _settings.value.blockedPackages.toMutableSet()
        if (!updated.add(packageName)) updated.remove(packageName)
        saveSet(KEY_BLOCKED_PACKAGES, updated)
        _settings.value = _settings.value.copy(blockedPackages = updated)
    }

    fun setBlockedPackages(packages: Set<String>) {
        saveSet(KEY_BLOCKED_PACKAGES, packages)
        _settings.value = _settings.value.copy(blockedPackages = packages)
    }

    fun setYoutubeMode(mode: YouTubeMode) {
        prefs.edit { putString(KEY_YOUTUBE_MODE, mode.name) }
        _settings.value = _settings.value.copy(youtubeMode = mode)
    }

    fun addYoutubeChannel(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val updated = _settings.value.youtubeChannels + trimmed
        saveSet(KEY_YOUTUBE_CHANNELS, updated)
        _settings.value = _settings.value.copy(youtubeChannels = updated)
    }

    fun removeYoutubeChannel(name: String) {
        val updated = _settings.value.youtubeChannels - name
        saveSet(KEY_YOUTUBE_CHANNELS, updated)
        _settings.value = _settings.value.copy(youtubeChannels = updated)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SOUND_ENABLED, enabled) }
        _settings.value = _settings.value.copy(soundEnabled = enabled)
    }

    fun setSoundVolume(volume: Float) {
        val safe = volume.coerceIn(0f, 1f)
        prefs.edit { putFloat(KEY_SOUND_VOLUME, safe) }
        _settings.value = _settings.value.copy(soundVolume = safe)
    }

    fun setSound(path: String?, name: String?) {
        prefs.edit {
            remove(KEY_SOUND_BUILTIN)
            if (path == null) {
                remove(KEY_SOUND_PATH)
                remove(KEY_SOUND_NAME)
            } else {
                putString(KEY_SOUND_PATH, path)
                putString(KEY_SOUND_NAME, name)
            }
        }
        _settings.value = _settings.value.copy(
            builtinSound = null,
            soundPath = path,
            soundName = name,
        )
    }

    /** Selects one of the sounds bundled with the app and clears any imported file. */
    fun setBuiltinSound(id: String?) {
        prefs.edit {
            if (id == null) remove(KEY_SOUND_BUILTIN) else putString(KEY_SOUND_BUILTIN, id)
            remove(KEY_SOUND_PATH)
            remove(KEY_SOUND_NAME)
        }
        _settings.value = _settings.value.copy(
            builtinSound = id,
            soundPath = null,
            soundName = null,
        )
    }

    // ----- Do Not Disturb restore state -----

    var savedInterruptionFilter: Int
        get() = prefs.getInt(KEY_SAVED_FILTER, -1)
        set(value) {
            prefs.edit { putInt(KEY_SAVED_FILTER, value) }
        }

    private fun loadSet(key: String): Set<String> {
        val raw = prefs.getString(key, null) ?: return emptySet()
        return runCatching {
            val array = JSONArray(raw)
            buildSet {
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun saveSet(key: String, values: Set<String>) {
        val array = JSONArray()
        values.forEach { array.put(it) }
        prefs.edit { putString(key, array.toString()) }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }

        private const val PREFS = "study_planner_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_ACCENT = "accent_color"
        private const val KEY_DEEP_MODE = "deep_focus_mode"
        private const val KEY_STRICT = "deep_focus_strict"
        private const val KEY_KEEP_SCREEN_ON = "deep_focus_keep_screen_on"
        private const val KEY_DND = "deep_focus_dnd"
        private const val KEY_CONFIRM_STOP = "deep_focus_confirm_stop"
        private const val KEY_BLOCKING = "deep_focus_blocking"
        private const val KEY_BLOCKED_PACKAGES = "deep_focus_blocked_packages"
        private const val KEY_YOUTUBE_MODE = "deep_focus_youtube_mode"
        private const val KEY_YOUTUBE_CHANNELS = "deep_focus_youtube_channels"
        private const val KEY_SOUND_ENABLED = "deep_focus_sound_enabled"
        private const val KEY_SOUND_BUILTIN = "deep_focus_sound_builtin"
        private const val KEY_SOUND_PATH = "deep_focus_sound_path"
        private const val KEY_SOUND_NAME = "deep_focus_sound_name"
        private const val KEY_SOUND_VOLUME = "deep_focus_sound_volume"
        private const val KEY_SAVED_FILTER = "deep_focus_saved_filter"
    }
}
