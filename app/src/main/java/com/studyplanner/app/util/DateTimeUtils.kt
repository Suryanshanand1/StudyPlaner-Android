package com.studyplanner.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.round

object Dates {
    private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val prettyDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    private val prettyDateLong: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)
    private val prettyTime: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val monthTitle: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    private val shortDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)

    fun today(): String = LocalDate.now().toString()

    fun nowTime(): String = LocalTime.now().format(time)

    fun formatDate(date: String): String = runCatching {
        LocalDate.parse(date).format(prettyDate)
    }.getOrDefault(date)

    fun formatDateLong(date: String): String = runCatching {
        LocalDate.parse(date).format(prettyDateLong)
    }.getOrDefault(date)

    fun formatMonthYear(year: Int, month: Int): String =
        YearMonth.of(year, month).format(monthTitle)

    fun formatMonth(month: Int): String =
        YearMonth.of(2000, month).format(DateTimeFormatter.ofPattern("MMMM", Locale.US))

    fun formatShortDay(date: LocalDate): String = date.format(shortDay)

    fun formatTime(value: String): String = runCatching {
        LocalTime.parse(value).format(prettyTime)
    }.getOrDefault(value)

    fun toTimeStr(dateTime: LocalDateTime): String = dateTime.format(time)

    fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

    fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

    fun toMillis(date: String, startTime: String): Long? {
        val d = parseDate(date) ?: return null
        val t = parseTime(startTime) ?: return null
        return d.atTime(t).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** Difference in whole minutes; a window that ends earlier than it starts crosses midnight. */
    fun diffMinutes(startTime: String, endTime: String): Int {
        val start = parseTime(startTime) ?: return 0
        val end = parseTime(endTime) ?: return 0
        var minutes = (end.toSecondOfDay() - start.toSecondOfDay()) / 60
        if (minutes < 0) minutes += 24 * 60
        return minutes
    }

    /** Difference in hours; a window that ends earlier than it starts crosses midnight. */
    fun diffHours(startTime: String, endTime: String): Double {
        return round((diffMinutes(startTime, endTime) / 60.0) * 10.0) / 10.0
    }

    fun minutesLabel(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val h = safe / 60
        val m = safe % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun weekDays(date: LocalDate): List<LocalDate> {
        val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return (0L..6L).map { start.plusDays(it) }
    }

    fun monthDays(year: Int, month: Int): List<LocalDate> {
        val ym = YearMonth.of(year, month)
        return (1..ym.lengthOfMonth()).map { ym.atDay(it) }
    }

    fun greeting(): String = when (LocalTime.now().hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    fun durationLabel(ms: Long): String {
        val totalMinutes = (ms / 60_000).toInt().coerceAtLeast(0)
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun clock(ms: Long): String {
        val total = (ms.coerceAtLeast(0) / 1000).toInt()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return "%02d:%02d:%02d".format(Locale.US, h, m, s)
    }
}

data class DayStat(
    val date: LocalDate,
    val minutes: Int,
    val chapters: Int,
    val sessions: Int,
    val focusMinutes: Int,
    val plannedMinutes: Int,
) {
    val hours: Double get() = minutes / 60.0
}

object Stats {
    fun overallProgress(chapters: List<com.studyplanner.app.data.ChapterEntity>): Int {
        if (chapters.isEmpty()) return 0
        return ((chapters.count { it.completed }.toFloat() / chapters.size) * 100f).toInt()
    }

    fun streak(plans: List<com.studyplanner.app.data.StudyPlanEntity>): Int {
        val confirmed = plans.filter { it.confirmed }.map { it.date }.toSet()
        var day = LocalDate.now()
        if (day.toString() !in confirmed) day = day.minusDays(1)
        var count = 0
        while (day.toString() in confirmed) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /**
     * One day of study activity. Focus time comes from the saved focus sessions (exact minutes),
     * and focus plans are skipped so a saved session is never counted twice.
     */
    fun dayStat(
        plans: List<com.studyplanner.app.data.StudyPlanEntity>,
        chapters: List<com.studyplanner.app.data.ChapterEntity>,
        focusSessions: List<com.studyplanner.app.data.FocusSessionEntity>,
        day: LocalDate,
    ): DayStat {
        val date = day.toString()
        val completedChapters = chapters.filter { it.completed }.map { it.id }.toSet()
        val dayPlans = plans.filter { it.date == date && it.confirmed }
        val plannedPlans = dayPlans.filter { it.source != FOCUS_SOURCE }
        val plannedMinutes = plannedPlans.sumOf {
            Dates.diffMinutes(it.startTime, it.endTime)
        }
        val dayFocus = focusSessions.filter { it.date == date }
        val focusMinutes = dayFocus.sumOf { it.minutes.coerceAtLeast(0) }
        val chaptersStudied = dayPlans
            .mapNotNull { plan -> plan.chapterId.takeIf { it in completedChapters } }
            .distinct()
            .size
        return DayStat(
            date = day,
            minutes = plannedMinutes + focusMinutes,
            chapters = chaptersStudied,
            sessions = plannedPlans.size + dayFocus.size,
            focusMinutes = focusMinutes,
            plannedMinutes = plannedMinutes,
        )
    }

    /** Minutes recorded on a date, for dashboard-style summaries. */
    fun minutesOn(
        plans: List<com.studyplanner.app.data.StudyPlanEntity>,
        focusSessions: List<com.studyplanner.app.data.FocusSessionEntity>,
        date: String,
    ): Int {
        val planned = plans
            .filter { it.date == date && it.source != FOCUS_SOURCE }
            .sumOf { Dates.diffMinutes(it.startTime, it.endTime) }
        val focus = focusSessions.filter { it.date == date }.sumOf { it.minutes.coerceAtLeast(0) }
        return planned + focus
    }

    fun monthly(
        plans: List<com.studyplanner.app.data.StudyPlanEntity>,
        chapters: List<com.studyplanner.app.data.ChapterEntity>,
        focusSessions: List<com.studyplanner.app.data.FocusSessionEntity>,
        year: Int,
        month: Int,
    ): List<DayStat> {
        return Dates.monthDays(year, month).map { day ->
            dayStat(plans, chapters, focusSessions, day)
        }
    }

    private const val FOCUS_SOURCE = "focus"
}
