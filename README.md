# Study Planner

A native Android study planner with a built-in focus stopwatch. It combines subject and
chapter tracking, day/week planning, streaks, and monthly analytics in one app written in
Kotlin with Jetpack Compose and Material 3. All data stays on the device.

- **Package:** `com.studyplanner.app`
- **Version:** 2.3.1 (versionCode 12)
- **Android:** minSdk 26, compile/targetSdk 36

## Features

- **Focus stopwatch** — an open-ended stopwatch to time any study session, with
  start, pause/resume, discard, and save.
- **Active-session notification** — an ongoing notification shows the live timer and lets
  you pause, resume, or stop the session without reopening the app.
- **Home-screen widget** — a distraction-free focus widget that starts, pauses, resumes,
  and stops a session right from the home screen.
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
- **Deep Focus** — flip one switch on the Focus tab to add a full-screen strict mode, optional
  focus sounds, and best-effort app/YouTube blocking to the same stopwatch.
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

### Controlling a session outside the app

- **Ongoing notification** — while a session is active, a silent, ongoing notification
  shows the running timer (a system chronometer when running, a fixed time when paused)
  with **Pause**/**Resume** and **Stop** actions. Tapping the notification opens the app
  on the Focus tab.
- **Home-screen widget** — add the *Focus stopwatch* widget and control the session with
  the same actions. The widget shows the elapsed time and updates itself whenever the
  state changes from the app, the notification, or the widget.

Both routes share one in-process session controller, so the app, widget, and notification
always show the same state. Stopping from anywhere saves the session when it lasted at
least one minute; shorter sessions are discarded with a toast.

> Version 2.1.0 removed the countdown timer and the `studyplanner://session` Allies deep
> link, version 2.2.0 adds the focus widget and active-session notification, version 2.3.0
> adds Deep Focus to the Focus tab, and version 2.3.1 keeps focus sounds playing under Do
> Not Disturb and fixes YouTube study mode blocking the whole app.

## Deep Focus

**Deep Focus** is a switch at the top of the Focus tab. Leave it off for the plain stopwatch,
or turn it on to add strict mode, focus sounds, and app blocking to the same session. The
extra settings only appear while it is on, and flipping the switch during a live session
applies or removes those effects immediately. Starting a session from Deep Focus is the same
session shown in the history, the notification, and the widget.

### Strict focus mode

- **Strict focus screen** — a full-screen, distraction-free timer that blocks Back while a
  session runs. It follows you across tabs and stays up until you end the session; pausing
  keeps it up with a Resume button.
- **Keep screen on** — prevents the display from sleeping during strict focus.
- **Do Not Disturb** - turns on Android's priority-only mode so calls and notifications stay
  quiet for the session while media (including the focus sound) keeps playing. Your previous
  interruption filter is restored when the session ends. Requires notification-policy access,
  which the screen can open for you.
- **Confirm before stopping** — asks once before ending a session.

Sessions shorter than one minute are still discarded, and longer ones are saved exactly
like a normal focus session.

### Focus sounds

Three ambience loops ship with the app — *Nature with melody*, *Rain with calm music*, and
*Rain* — so you can start a session without hunting for a file. You can also **import your
own**: pick a rain loop, lo-fi track, or any local audio file with the system picker. The
imported copy is stored in app storage, so it plays offline. A media-playback foreground
service loops the sound and starts, pauses, resumes, and stops it along with the session.
The volume is adjustable, and the sound can be swapped or removed at any time.

> Bundling the three tracks adds about 23 MB to the APK. The import option remains if you
> prefer to keep the download smaller.

### App and YouTube blocking

Blocking uses an Android **accessibility service** to notice which app is in the foreground
during a session:

- **Blocked apps** — pick any installed app; while a session is active it is replaced by a
  Deep Focus block screen with **Return to study** and **Go to home screen**. Both leave the
  blocked app, and Back returns to Study Planner.
- **YouTube** - either block it entirely, or use *Study channels only*, which blocks Shorts and
  videos from channels you have not listed while leaving home, search, and subscriptions
  usable. With no channels added, study mode blocks the whole app.

Blocking is a best-effort helper. YouTube changes its interface regularly, so the channel
allowlist is inherently fragile — blocking the whole app is the reliable option. The
accessibility service only reads which app/screen is open to enforce these rules; nothing is
collected or shared, and blocking only applies while a session is running.

> Deep Focus needs two system grants: **accessibility** for app blocking and
> **notification policy access** for Do Not Disturb. The Focus tab shows a warning and a
> shortcut whenever either is missing.

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
  data/                        Room entities/DAO/database, repository, settings, focus session state
  focus/                       Deep Focus: session effects, sound service, accessibility blocker, block screen
  notify/                      AlarmManager scheduler, active-session notification, broadcast receivers
  widget/                      Home-screen focus widget provider
  ui/                          ViewModel, root scaffold and screens
    components/                Reusable cards, dialogs, chart
    screens/                   Dashboard, Subjects, Planner, Focus (with Deep Focus sections), Analytics, Settings
    theme/                     Material 3 color schemes and typography
  util/                        Date, time and statistics helpers
```

## Data and privacy

- App data is stored in a Room database (`study-planner.db`).
- Preferences and the active stopwatch are stored in `SharedPreferences`.
- No account is required and no study data leaves the device. The only network request
  is the optional GitHub release update check.
- Deep Focus also stays on-device: the accessibility service only reports which app or
  screen is in the foreground so chosen apps can be blocked, and imported focus sounds are
  copied into app-private storage. Neither is uploaded anywhere.
- Deleting a subject removes its chapters and plans. Importing a backup replaces all
  current data.

## Notes

- Notifications require permission on Android 13+. If reminders are delayed, allow exact
  alarms from **Settings > Notifications** inside the app.
- The app checks the project's GitHub releases once a day and shows an in-app update card
  when a newer version exists. Everything else works offline.
