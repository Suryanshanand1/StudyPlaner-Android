package com.studyplanner.app.data

import android.net.Uri

data class IncomingSession(
    val label: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val minutes: Int,
)

private val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
private val TIME_PATTERN = Regex("^\\d{2}:\\d{2}$")

/** Parses the studyplanner://session deep link used by the standalone Allies app. */
fun parseIncomingSession(uri: Uri?): IncomingSession? {
    if (uri == null) return null
    if (uri.scheme != "studyplanner" || uri.host != "session") return null
    val date = uri.getQueryParameter("date") ?: return null
    val start = uri.getQueryParameter("start") ?: return null
    val end = uri.getQueryParameter("end") ?: return null
    if (!DATE_PATTERN.matches(date)) return null
    if (!TIME_PATTERN.matches(start) || !TIME_PATTERN.matches(end)) return null
    val label = (uri.getQueryParameter("label") ?: "Allies session").take(100)
    val minutes = (uri.getQueryParameter("minutes")?.toIntOrNull() ?: 0).coerceAtLeast(0)
    return IncomingSession(label, date, start, end, minutes)
}
