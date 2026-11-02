# Study Planner

A native Android rewrite of the Study Planner PWA with the Allies focus timer built in.
The app is written in Kotlin with Jetpack Compose and Material 3, and keeps all data on
the device.

## Features

- **Subjects and chapters** — create, edit, delete, reorder and complete chapters, with
  per-subject progress and colors.
- **Planner** — day and week views, quick subject/chapter creation, date and time pickers.
- **Study streaks** — mark a scheduled session as studied; streaks are based on days with
  at least one confirmed session.
- **Analytics** — monthly totals for study time and sessions, daily study-time and
  chapter charts, a day-by-day breakdown (focus vs. planned minutes), best day,
  daily average, and per-subject progress. Saved focus sessions count toward the day
  they were recorded without being double-counted.
- **Focus timer and stopwatch** — the Allies experience is built into the app:
  - countdown timer with pause/resume,
  - open-ended stopwatch,
  - session labels and optional subject/chapter association,
  - saved sessions are added to the planner and focus history (minimum one minute).
- **Reminders** — local notifications when a planned session starts and when a focus timer
  finishes. Alarms are restored after reboot.
- **Appearance** — light, dark or system theme plus preset or custom accent colors.
- **Backups** — JSON export/import using the same schema as the original web app
  (`version`, `subjects`, `chapters`, `studyPlans`).
- **Allies compatibility** — still accepts the
  `studyplanner://session?label=...&date=...&start=...&end=...&minutes=...` deep link used
  by the standalone Allies app.

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
