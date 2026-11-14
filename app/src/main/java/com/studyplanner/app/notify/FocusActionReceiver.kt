package com.studyplanner.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.studyplanner.app.R
import com.studyplanner.app.data.FocusSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles start/pause/resume/stop actions from the notification and the home-screen widget. */
class FocusActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FocusSessionManager.initialize(context)
        when (intent.action) {
            ACTION_START -> FocusSessionManager.start(
                subjectId = "",
                chapterId = "",
                subjectName = "",
                chapterName = "",
                customLabel = "",
            )
            ACTION_PAUSE -> FocusSessionManager.pause()
            ACTION_RESUME -> FocusSessionManager.resume()
            ACTION_STOP -> {
                val hadSession = FocusSessionManager.current != null
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    val summary = runCatching { FocusSessionManager.stopAndSave() }.getOrNull()
                    val message = when {
                        summary != null -> context.getString(R.string.focus_saved_toast, summary)
                        hadSession -> context.getString(R.string.focus_discarded_toast)
                        else -> null
                    }
                    if (message != null) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_START = "com.studyplanner.app.action.FOCUS_START"
        const val ACTION_PAUSE = "com.studyplanner.app.action.FOCUS_PAUSE"
        const val ACTION_RESUME = "com.studyplanner.app.action.FOCUS_RESUME"
        const val ACTION_STOP = "com.studyplanner.app.action.FOCUS_STOP"
    }
}
