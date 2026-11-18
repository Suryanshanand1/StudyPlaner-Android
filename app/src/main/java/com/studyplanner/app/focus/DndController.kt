package com.studyplanner.app.focus

import android.app.NotificationManager
import android.content.Context
import com.studyplanner.app.data.SettingsRepository

/**
 * Turns Do Not Disturb on while a focus session is active and restores the previous
 * interruption filter when the session ends. Requires the user to grant notification
 * policy access in system settings.
 */
object DndController {
    fun isGranted(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.isNotificationPolicyAccessGranted
    }

    fun enable(context: Context) {
        if (!isGranted(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val repository = SettingsRepository.get(context)
        if (repository.savedInterruptionFilter == -1) {
            repository.savedInterruptionFilter = manager.currentInterruptionFilter
        }
        runCatching {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    fun disable(context: Context) {
        val repository = SettingsRepository.get(context)
        val saved = repository.savedInterruptionFilter
        if (saved == -1) return
        if (isGranted(context)) {
            val manager = context.getSystemService(NotificationManager::class.java)
            runCatching { manager?.setInterruptionFilter(saved) }
        }
        repository.savedInterruptionFilter = -1
    }
}
