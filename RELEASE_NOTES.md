# Release Notes — Study Planner

## Latest Features

- **Allies is now built-in** — the standalone Allies focus timer/stopwatch is part of this app (no separate install needed).
- **Focus timer** — countdown timer with pause/resume and optional end notification.
- **Stopwatch** — open-ended stopwatch to time any study session.
- **Session labels** — associate a session with a subject/chapter or custom label; saved to focus history and the planner (minimum 1 minute).
- **Detailed daily analytics** — monthly totals, daily study-time chart, day-by-day breakdown (focus vs. planned minutes), best day, daily average, per-subject progress. Saved focus sessions count without being double-counted.
- **Reminders** — local notifications for planned sessions and finished focus timers; alarms restored after reboot.
- **Appearance** — light, dark or system theme plus preset or custom accent colors.
- **Backups** — JSON export/import using the original web app schema (`version`, `subjects`, `chapters`, `studyPlans`) plus native `focusSessions` and `exportedAt`.
- **Allies compatibility** — still accepts the `studyplanner://session` deep link.
- **App updates** — checks GitHub releases once a day and shows an in-app update card (plus manual "Check for updates" in Settings).

## All Features

- Subjects and chapters — create, edit, delete, reorder, complete; per-subject progress and colors.
- Day/week planner — schedule sessions with quick subject/chapter creation and date/time pickers.
- Study streaks — mark a session as studied; streaks based on days with at least one confirmed session.
- Focus timer and stopwatch (Allies built-in).
- Session labels and optional subject/chapter association.
- Analytics — monthly totals, daily study-time chart, day-by-day breakdown, best day, daily average, subject progress.
- Reminders — local notifications for planned sessions and finished focus timers; alarms restored after reboot.
- Appearance — light, dark or system theme plus preset or custom accent colors.
- Backups — JSON export/import compatible with the original web app backup schema.
- Allies compatibility — accepts the `studyplanner://session` deep link used by the standalone Allies app.
- App updates — daily GitHub release check with in-app update card and manual check in Settings.

## Build

- Kotlin + Jetpack Compose, Material 3.
- Room persistence; minSdk 26 / targetSdk 36.
- Debug: `.\gradlew.bat :app:assembleDebug`
- Release: `.\gradlew.bat :app:assembleRelease` (signed with local debug key for testing).