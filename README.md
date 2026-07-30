# Study Planner App

A modern, mobile-first study planner web app built with Next.js 16, React 19, TypeScript, and Tailwind CSS.

## Features

- **Subject & Chapter Management** — Add, edit, delete subjects and chapters with automatic progress tracking.
- **Dynamic Study Planner** — Assign study tasks to specific dates/times, view by day or week.
- **Monthly Analytics** — Bar charts showing daily study hours and completed chapters (powered by Recharts).
- **Browser Notifications** — Web Notifications API sends reminders when a scheduled session starts.

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
3. **Analytics tab** — View monthly stats and bar charts for study hours and chapters completed.
4. **Notifications** — Grant notification permission when prompted. The app checks every 30 seconds and alerts you when a scheduled session begins.

### Mobile / Android (APK)

#### Option 1: PWA (Add to Home Screen) — No build needed
The app includes a PWA manifest and icons. Open in Chrome, tap menu → **Add to Home screen**. It launches full-screen like a native app with its own icon. Notifications work in this mode.

#### Option 2: GitHub Actions (APK) — No local setup
1. Push this repo to GitHub
2. Go to your repo → **Actions** → **Build APK** → **Run workflow**
3. Download the APK artifact and install it on your phone

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

## Project Structure

```
src/
├── app/
│   ├── globals.css        # Global styles
│   ├── layout.tsx         # Root layout with providers
│   └── page.tsx           # Main app with tab navigation
├── components/
│   ├── ChapterForm.tsx     # Add chapter modal
│   ├── MonthlyChart.tsx    # Recharts dashboard
│   ├── NotificationManager.tsx  # Notification checker
│   ├── PlanForm.tsx        # Create study plan modal
│   ├── StudyPlanner.tsx    # Day/week schedule view
│   ├── SubjectCard.tsx     # Subject with chapters & progress
│   └── SubjectForm.tsx     # Add/edit subject modal
└── lib/
    ├── notifications.ts   # Web Notification helpers
    ├── store.tsx          # React Context + LocalStorage state
    ├── types.ts           # TypeScript interfaces
    └── utils.ts           # Date/color helpers
```
