package com.studyplanner.app.focus

import android.content.Context
import com.studyplanner.app.data.ActiveSession
import com.studyplanner.app.data.SettingsRepository

/**
 * Applies the Deep Focus side effects whenever the live session changes: Do Not Disturb
 * and ambient sound start with the session and stop when it ends.
 */
object DeepFocusManager {
    fun onSessionChanged(context: Context, previous: ActiveSession?, current: ActiveSession?) {
        val app = context.applicationContext
        val settings = SettingsRepository.get(app).settings.value

        // Plain stopwatch sessions keep their hands off DND, sound, and blocking.
        if (!settings.deepFocusMode) return

        when {
            previous == null && current != null -> {
                if (settings.deepFocusDnd) DndController.enable(app)
                val hasSound = settings.builtinSound != null || !settings.soundPath.isNullOrBlank()
                if (current.isRunning && settings.soundEnabled && hasSound) {
                    FocusSoundService.start(app)
                }
            }

            previous != null && current == null -> {
                FocusSoundService.stop(app)
                DndController.disable(app)
            }

            previous != null && current != null -> {
                if (previous.isRunning && !current.isRunning) {
                    FocusSoundService.pause(app)
                } else if (!previous.isRunning && current.isRunning) {
                    FocusSoundService.resume(app)
                }
            }
        }
    }
}
