package com.studyplanner.app.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/**
 * Persists the live stopwatch so it survives process death and app restarts.
 * Time is derived from wall-clock timestamps, so the clock stays accurate while
 * the app is closed.
 */
class ActiveSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): ActiveSession? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            // Older builds also persisted countdown timers, which no longer exist.
            if (json.optString("mode") == "timer") return null
            ActiveSession(
                subjectId = json.optString("subjectId"),
                chapterId = json.optString("chapterId"),
                subjectName = json.optString("subjectName"),
                chapterName = json.optString("chapterName"),
                startedAt = json.optLong("startedAt", System.currentTimeMillis()),
                segmentStartedAt = json.optLong("segmentStartedAt", System.currentTimeMillis()),
                accumulatedMs = json.optLong("accumulatedMs", 0L),
                state = if (json.optString("state") == "paused") SessionState.PAUSED else SessionState.RUNNING,
            )
        }.getOrNull()
    }

    fun save(session: ActiveSession?) {
        if (session == null) {
            prefs.edit { remove(KEY) }
            return
        }
        val json = JSONObject().apply {
            put("subjectId", session.subjectId)
            put("chapterId", session.chapterId)
            put("subjectName", session.subjectName)
            put("chapterName", session.chapterName)
            put("startedAt", session.startedAt)
            put("segmentStartedAt", session.segmentStartedAt)
            put("accumulatedMs", session.accumulatedMs)
            put("state", if (session.state == SessionState.PAUSED) "paused" else "running")
        }
        prefs.edit { putString(KEY, json.toString()) }
    }

    private companion object {
        const val PREFS = "study_planner_active_session"
        const val KEY = "session"
    }
}
