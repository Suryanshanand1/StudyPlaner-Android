# Study Planner App

A modern, mobile-first study planner web app built with Next.js 16, React 19, TypeScript, and Tailwind CSS.

## Features

- **Subject & Chapter Management** — Add, edit, delete subjects and chapters with automatic progress tracking.
- **Dynamic Study Planner** — Assign study tasks to specific dates/times, view by day or week.
- **Confirm to Study** — Mark a session as actually completed. Your streak counts only confirmed study days, so scheduling alone doesn't inflate it.
- **Monthly Analytics** — Bar charts showing daily study hours and completed chapters (powered by Recharts).
- **Browser Notifications** — Web Notifications API sends reminders when a scheduled session starts.
- **Update Notifications** — The app checks for new releases once a day and alerts you when an update is available.

## Tech Stack

| Layer | Choice |
|-------|--------|
| Framework | Next.js 16 (App Router) |
| UI | React 19 + Tailwind CSS 4 |
| Icons | Lucide React |
| Charts | Recharts |
| Persistence | LocalStorage |
| Notifications | Web Notifications API |

## Getting Started

```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Start production server
npm run start
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

## Usage

1. **Subjects tab** — Add subjects, then add chapters inside each subject. Check off chapters to track progress.
2. **Planner tab** — Create study plans by selecting a subject, chapter, date, and time range. Switch between day/week views.
3. **Confirm to Study** — After studying a scheduled session, tap the checkmark on the plan (Planner or Dashboard). Your **streak** increases only for days with a confirmed session.
4. **Analytics tab** — View monthly stats and bar charts for study hours and chapters completed.
5. **Notifications** — Grant notification permission when prompted. The app checks every 30 seconds and alerts you when a scheduled session begins.
6. **Update notifications** — The app checks GitHub for a newer release once a day. When one is found, you get a notification and an in-app banner with an **Update** button to download the latest APK.

### Mobile / Android (APK)

#### Option 1: PWA (Add to Home Screen) — No build needed
The app includes a PWA manifest and icons. Open in Chrome, tap menu → **Add to Home screen**. It launches full-screen like a native app with its own icon. Notifications work in this mode.

#### Option 2: GitHub Actions (APK) — No local setup
1. Push this repo to GitHub
2. Go to your repo → **Actions** → **Build APK** → **Run workflow**
3. The workflow builds the APK and publishes it as a **GitHub Release**
4. Install the APK from your repo's **Releases** page on your phone

#### Option 3: PWABuilder — No local setup
1. Deploy the app to Vercel: `npx vercel`
2. Paste the URL at https://pwabuilder.com
3. Click **Package for Android** → download the APK

#### Option 4: Build locally (requires Android SDK)
```bash
npm run build
npx cap copy android
npx cap open android   # or: cd android && gradlew assembleDebug
```

## Releasing an update

1. Bump the version in `version.json` (e.g. `1.1.0` → `1.2.0`). Also update `versionName` and `versionCode` in `android/app/build.gradle`.
2. Push to GitHub. The **Build APK** workflow builds the APK and publishes a GitHub Release with the new version.
3. Installed apps check for updates once a day via the release page and show a notification + "Update" banner.

## Project Structure

```
src/
├── app/
│   ├── globals.css        # Global styles
│   ├── layout.tsx         # Root layout with providers
│   └── page.tsx           # Main app with tab navigation
├── components/
│   ├── chart/
│   │   └── MonthlyChart.tsx    # Recharts dashboard
│   ├── planner/
│   │   ├── PlanForm.tsx        # Create study plan modal
│   │   └── StudyPlanner.tsx    # Day/week schedule view
│   ├── settings/
│   │   └── Settings.tsx        # Theme + data settings
│   ├── subjects/
│   │   ├── ChapterForm.tsx     # Add chapter modal
│   │   ├── SubjectCard.tsx     # Subject with chapters & progress
│   │   └── SubjectForm.tsx     # Add/edit subject modal
│   ├── Dashboard.tsx           # Home dashboard summary
│   ├── NotificationManager.tsx # Notification checker
│   └── UpdateChecker.tsx       # Daily update check + banner
├── hooks/
│   └── theme.tsx          # Theme context + accent color
└── lib/
    ├── notifications.ts   # Web Notification helpers
    ├── store.tsx          # React Context + LocalStorage state
    ├── types.ts           # TypeScript interfaces
    ├── update.ts          # Update check + version comparison
    └── utils.ts           # Date/color helpers
```
