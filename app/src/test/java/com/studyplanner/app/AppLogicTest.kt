package com.studyplanner.app

import com.studyplanner.app.data.ChapterEntity
import com.studyplanner.app.data.FocusSessionEntity
import com.studyplanner.app.data.StudyPlanEntity
import com.studyplanner.app.data.UpdateChecker
import com.studyplanner.app.util.Dates
import com.studyplanner.app.util.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AppLogicTest {

    @Test
    fun diffHoursHandlesNormalAndOvernightWindows() {
        assertEquals(90, Dates.diffMinutes("09:00", "10:30"))
        assertEquals(120, Dates.diffMinutes("23:00", "01:00"))
        assertEquals(0, Dates.diffMinutes("not-a-time", "10:00"))
        assertEquals(1.5, Dates.diffHours("09:00", "10:30"), 0.0)
        assertEquals(2.0, Dates.diffHours("23:00", "01:00"), 0.0)
        assertEquals(0.0, Dates.diffHours("not-a-time", "10:00"), 0.0)
    }

    @Test
    fun minuteLabelsUseCompactHoursAndMinutes() {
        assertEquals("0m", Dates.minutesLabel(0))
        assertEquals("1m", Dates.minutesLabel(1))
        assertEquals("59m", Dates.minutesLabel(59))
        assertEquals("1h", Dates.minutesLabel(60))
        assertEquals("1h 46m", Dates.minutesLabel(106))
    }

    @Test
    fun weekStartsOnSunday() {
        val days = Dates.weekDays(LocalDate.of(2026, 11, 2))
        assertEquals(LocalDate.of(2026, 11, 1), days.first())
        assertEquals(LocalDate.of(2026, 11, 7), days.last())
        assertEquals(7, days.size)
    }

    @Test
    fun versionComparisonIgnoresPrefixAndPadsMissingParts() {
        assertTrue(UpdateChecker.isNewer("v2.1.0", "2.0.0"))
        assertTrue(UpdateChecker.isNewer("2.0.1", "2.0"))
        assertFalse(UpdateChecker.isNewer("1.3.0", "2.0.0"))
        assertFalse(UpdateChecker.isNewer("2.0.0", "2.0.0"))
    }

    @Test
    fun streakCountsTodayAndPreviousDays() {
        val today = LocalDate.now()
        val plans = listOf(
            plan(today.toString()),
            plan(today.minusDays(1).toString()),
            plan(today.minusDays(2).toString()),
            plan(today.minusDays(4).toString()),
        )
        assertEquals(3, Stats.streak(plans))
    }

    @Test
    fun streakKeepsYesterdayWhenTodayIsNotDoneYet() {
        val yesterday = LocalDate.now().minusDays(1)
        val plans = listOf(
            plan(yesterday.toString()),
            plan(yesterday.minusDays(1).toString()),
        )
        assertEquals(2, Stats.streak(plans))
    }

    @Test
    fun monthlyStatsOnlyUseConfirmedPlans() {
        val chapter = ChapterEntity(
            id = "chapter-1",
            subjectId = "subject-1",
            name = "Chapter",
            completed = true,
            order = 0,
            createdAt = 0L,
        )
        val date = LocalDate.of(2026, 11, 5)
        val plans = listOf(
            plan(date.toString(), chapterId = chapter.id, confirmed = true),
            plan(date.toString(), chapterId = chapter.id, confirmed = false),
        )

        val day = Stats.monthly(plans, listOf(chapter), emptyList(), 2026, 11)
            .first { it.date == date }

        assertEquals(60, day.minutes)
        assertEquals(1.0, day.hours, 0.0)
        assertEquals(1, day.chapters)
        assertEquals(1, day.sessions)
    }

    @Test
    fun oneMinuteFocusSessionCountsAndIsNotDoubleCounted() {
        val date = LocalDate.of(2026, 11, 6)
        val focusPlan = plan(date.toString()).copy(source = "focus")
        val focusSession = FocusSessionEntity(
            id = "focus-1",
            label = "Focus session",
            date = date.toString(),
            startTime = "18:00",
            endTime = "18:01",
            minutes = 1,
            createdAt = 0L,
        )

        val day = Stats.monthly(listOf(focusPlan), emptyList(), listOf(focusSession), 2026, 11)
            .first { it.date == date }

        assertEquals(1, day.minutes)
        assertEquals(1, day.sessions)
        assertEquals(1, day.focusMinutes)
        assertEquals(0, day.plannedMinutes)
        assertEquals(0, day.chapters)
    }

    @Test
    fun analyticsCombinesPlannedAndFocusMinutes() {
        val date = LocalDate.of(2026, 11, 7)
        val day = Stats.monthly(
            plans = listOf(plan(date.toString())),
            chapters = emptyList(),
            focusSessions = listOf(
                FocusSessionEntity(
                    id = "focus-2",
                    label = "Focus session",
                    date = date.toString(),
                    startTime = "18:00",
                    endTime = "18:25",
                    minutes = 25,
                    createdAt = 0L,
                ),
            ),
            year = 2026,
            month = 11,
        ).first { it.date == date }

        assertEquals(85, day.minutes)
        assertEquals(60, day.plannedMinutes)
        assertEquals(25, day.focusMinutes)
        assertEquals(2, day.sessions)
    }

    private fun plan(
        date: String,
        chapterId: String = "chapter-1",
        confirmed: Boolean = true,
    ) = StudyPlanEntity(
        id = "plan-$date-$chapterId-$confirmed",
        subjectId = "subject-1",
        chapterId = chapterId,
        subjectName = "Subject",
        chapterName = "Chapter",
        date = date,
        startTime = "09:00",
        endTime = "10:00",
        createdAt = 0L,
        confirmed = confirmed,
    )
}
