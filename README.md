# Study Planner

A native Android study planner with a built-in focus stopwatch. It combines subject and
chapter tracking, day/week planning, streaks, and monthly analytics in one app written in
Kotlin with Jetpack Compose and Material 3. All data stays on the device.

- **Package:** `com.studyplanner.app`
- **Version:** 2.1.0 (versionCode 9)
- **Android:** minSdk 26, compile/targetSdk 36

## Features

- **Focus stopwatch** — an open-ended stopwatch to time any study session, with
  start, pause/resume, discard, and save.
- **Subjects and chapters** — create, edit, delete, reorder, and complete chapters, with
  per-subject progress and colors.
- **Day/week planner** — schedule study sessions in day or week views, with quick
  subject/chapter creation and date/time pickers.
- **Study streaks** — mark a scheduled session as studied; streaks count days with at
  least one confirmed session.
- **Session labels** — associate a stopwatch session with a subject/chapter or a custom
  label. Saved sessions are added to focus history and the planner (minimum 1 minute).
- **Analytics** — monthly totals for study time and sessions, a daily study-time chart, a
  day-by-day breakdown (focus vs. planned minutes), best day, daily average, and
  per-subject progress. Saved focus sessions count toward the day they were recorded
  without being double-counted.
- **Reminders** — local notifications when a planned session starts. Alarms are restored
  after reboot (requires exact-alarm permission).
- **Appearance** — light, dark, or system theme plus preset or custom accent colors.
- **Backups** — JSON export/import using the same schema as the original web app
  (`version`, `subjects`, `chapters`, `studyPlans`), plus native `focusSessions` and
  `exportedAt` fields.
- **App updates** — checks GitHub releases once a day and shows an in-app update card
  when a newer version exists, plus a manual "Check for updates" in Settings.

## Focus stopwatch

1. Open the **Focus** tab.
2. Optionally pick a subject/chapter and enter a session label.
3. Tap **Start stopwatch**, and **Pause**/**Resume** as needed.
4. Tap **Save Session** after at least one minute to add it to your planner, focus
   history, and analytics. **Discard** clears the current session.

The stopwatch keeps accurate time even if the app is closed, because elapsed time is
derived from wall-clock timestamps and persisted in `SharedPreferences`.

> Version 2.1.0 uses the stopwatch only. The countdown timer and the
> `studyplanner://session` Allies deep link were removed.

## Build

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36
- Android Studio (Ladybug or newer) or the Gradle wrapper

Create `local.properties` if it is missing:

```properties
sdk.dir=C\:\\Path\\To\\Android\\Sdk
```

Build the debug APK:

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

For a release build:

```powershell
.\gradlew.bat :app:assembleRelease
```

This produces an installable APK at
`app/build/outputs/apk/release/app-release.apk`, signed with the local debug key so it
can be tested on a device. Before publishing, replace the release signing configuration
in `app/build.gradle.kts` with your own keystore (or use
**Build > Generate Signed App Bundle or APK** in Android Studio).

## Project layout

```
app/src/main/java/com/studyplanner/app/
  MainActivity.kt              Activity and theme setup
  StudyPlannerApp.kt           Notification channel setup
  data/                        Room entities/DAO/database, repository, settings
  notify/                      AlarmManager scheduler and broadcast receivers
  ui/                          ViewModel, root scaffold and screens
    components/                Reusable cards, dialogs, chart
    screens/                   Dashboard, Subjects, Planner, Focus, Analytics, Settings
    theme/                     Material 3 color schemes and typography
  util/                        Date, time and statistics helpers
```

## Data and privacy

- App data is stored in a Room database (`study-planner.db`).
- Preferences and the active stopwatch are stored in `SharedPreferences`.
- No account is required and no study data leaves the device. The only network request
  is the optional GitHub release update check.
- Deleting a subject removes its chapters and plans. Importing a backup replaces all
  current data.

## Notes

- Notifications require permission on Android 13+. If reminders are delayed, allow exact
  alarms from **Settings > Notifications** inside the app.
- The app checks the project's GitHub releases once a day and shows an in-app update card
  when a newer version exists. Everything else works offline.
