# Study Planner

A single native Android app that merges the Study Planner PWA with the Allies focus
timer/stopwatch — **Allies is now built-in**. The app is written in Kotlin with Jetpack
Compose and Material 3, and keeps all data on the device.

## Features

- **Allies built-in** — the standalone Allies timer/stopwatch experience is now part of
  this app (no separate install needed). Open the **Focus** tab to use it.
- **Allies deep-link compatibility** — still accepts the
  `studyplanner://session?label=...&date=...&start=...&end=...&minutes=...&source=...`
  deep link used by the standalone Allies app.
- **Subjects and chapters** — create, edit, delete, reorder and complete chapters, with
  per-subject progress and colors.
- **Day/week planner** — schedule study sessions in day or week views, with quick
  subject/chapter creation and date/time pickers.
- **Study streaks** — mark a scheduled session as studied; streaks are based on days with
  at least one confirmed session.
- **Focus timer** — countdown timer with pause/resume and an optional end notification.
- **Stopwatch** — open-ended stopwatch to time any study session.
- **Session labels** — associate a timer/stopwatch session with a subject/chapter or a
  custom label; sessions are saved to focus history and the planner (minimum 1 minute).
- **Analytics** — monthly totals for study time and sessions, a daily study-time chart,
  a day-by-day breakdown (focus vs. planned minutes), best day, daily average, and
  per-subject progress. Saved focus sessions count toward the day they were recorded
  without being double-counted.
- **Reminders** — local notifications when a planned session starts and when a focus
  timer finishes. Alarms are restored after reboot (requires exact-alarm permission).
- **Appearance** — light, dark or system theme plus preset or custom accent colors.
- **Backups** — JSON export/import using the same schema as the original web app
  (`version`, `subjects`, `chapters`, `studyPlans`), plus native `focusSessions` and
  `exportedAt` fields.
- **App updates** — the app checks GitHub releases once a day and shows an in-app update
  card when a newer version exists (plus a manual "Check for updates" in Settings).

## Build

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36
- Android Studio (Ladybug or newer) or the Gradle wrapper

Create `local.properties` if it is missing:

```properties
sdk.dir=C\:\\Path\\To\\Android\\Sdk
```

Then build the debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Install it on a connected device or emulator:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Run the unit tests and Android lint:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

For a release build, run:

```powershell
.\gradlew.bat :app:assembleRelease
```

This produces an installable APK at
`app/build/outputs/apk/release/app-release.apk`, signed with the local debug key so
it can be tested on a device. Before publishing, replace the release signing
configuration in `app/build.gradle.kts` with your own keystore (or use
**Build > Generate Signed App Bundle or APK** in Android Studio).

## Deep link test

```powershell
adb shell am start -a android.intent.action.VIEW `
  -d "studyplanner://session?label=Physics&date=2026-11-02&start=18:00&end=18:45&minutes=45"
```

## Project layout

```
app/src/main/java/com/studyplanner/app/
  MainActivity.kt              Activity, theme and deep-link handling
  StudyPlannerApp.kt           Notification channel setup
  data/                        Room entities/DAO/database, repository, settings
  notify/                      AlarmManager scheduler and broadcast receivers
  ui/                          ViewModel, root scaffold and screens
    components/                Reusable cards, dialogs, chart
    screens/                   Dashboard, Subjects, Planner, Focus, Analytics, Settings
    theme/                     Material 3 color schemes and typography
  util/                        Date, time and statistics helpers
```

## Notes

- Data is stored in a Room database (`study-planner.db`); preferences and the active focus
  session are stored in `SharedPreferences`.
- Deleting a subject removes its chapters and plans. Importing a backup replaces all
  current data.
- Notifications require permission on Android 13+. If reminders are delayed, allow exact
  alarms from **Settings > Notifications** inside the app.
- The app checks the project's GitHub releases once a day and shows an in-app update card
  when a newer version exists. Everything else works offline.
