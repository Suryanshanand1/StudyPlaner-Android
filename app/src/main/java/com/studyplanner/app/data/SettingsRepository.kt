package com.studyplanner.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: String = "#3B82F6",
)

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings {
        val mode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        val accent = prefs.getString(KEY_ACCENT, "#3B82F6") ?: "#3B82F6"
        return AppSettings(mode, accent)
    }

    fun setTheme(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.name) }
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setAccent(color: String) {
        prefs.edit { putString(KEY_ACCENT, color) }
        _settings.value = _settings.value.copy(accentColor = color)
    }

    private companion object {
        const val PREFS = "study_planner_settings"
        const val KEY_THEME = "theme_mode"
        const val KEY_ACCENT = "accent_color"
    }
}
