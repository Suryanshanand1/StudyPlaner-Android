"use client"

import { useEffect, useRef, useState } from "react"
import { X, Download, RotateCw } from "lucide-react"
import { LocalNotifications } from "@capacitor/local-notifications"
import { APP_VERSION, getLatestRelease, isNewer, type LatestRelease } from "@/lib/update"
import { requestNotificationPermission } from "@/lib/notifications"

const LAST_CHECK_KEY = "study-planner-update-check"
const NOTIFIED_KEY = "study-planner-update-notified"
const DAY_MS = 24 * 60 * 60 * 1000

function isNative(): boolean {
  return typeof window !== "undefined" && !!(window as any).Capacitor?.isNativePlatform
}

async function sendUpdateNotification(version: string) {
  const ok = await requestNotificationPermission()
  if (!ok) return
  const title = "New update available"
  const body = `Study Planner ${version} is ready to install. Tap to update.`
  if (isNative()) {
    await LocalNotifications.schedule({
      notifications: [{ id: 9999, title, body }],
    })
  } else if ("Notification" in window && Notification.permission === "granted") {
    try {
      new Notification(title, { body, icon: "/icon-192.png" })
    } catch {}
  }
}

export default function UpdateChecker() {
  const [release, setRelease] = useState<LatestRelease | null>(null)
  const [dismissed, setDismissed] = useState(false)
  const notifiedRef = useRef<string>("")

  useEffect(() => {
    let cancelled = false

    const run = async () => {
      const lastCheck = Number(localStorage.getItem(LAST_CHECK_KEY) || 0)
      const now = Date.now()
      if (now - lastCheck < DAY_MS) return

      const latest = await getLatestRelease()
      if (!latest || !latest.version) return

      localStorage.setItem(LAST_CHECK_KEY, String(now))

      if (!isNewer(latest.version, APP_VERSION)) return

      if (notifiedRef.current !== latest.version && localStorage.getItem(NOTIFIED_KEY) !== latest.version) {
        notifiedRef.current = latest.version
        localStorage.setItem(NOTIFIED_KEY, latest.version)
        sendUpdateNotification(latest.version)
      }

      if (!cancelled) setRelease(latest)
    }

    run()
    return () => {
      cancelled = true
    }
  }, [])

  if (!release || dismissed) return null

  return (
    <div className="fixed inset-x-0 bottom-20 z-40 mx-auto max-w-lg px-4">
      <div className="flex items-center gap-3 rounded-2xl border border-zinc-200 bg-white p-4 shadow-lg dark:border-zinc-800 dark:bg-zinc-900">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-accent/10 text-accent">
          <Download size={20} />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-zinc-800 dark:text-zinc-200">
            Version {release.version} available
          </p>
          {release.notes && (
            <p className="truncate text-xs text-zinc-400 dark:text-zinc-500">{release.notes}</p>
          )}
        </div>
        <a
          href={release.apkUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="flex shrink-0 items-center gap-1.5 rounded-xl bg-accent px-3 py-2 text-xs font-medium text-white"
        >
          <RotateCw size={14} />
          Update
        </a>
        <button
          onClick={() => setDismissed(true)}
          className="shrink-0 rounded-full p-1.5 text-zinc-400 hover:bg-zinc-100 dark:text-zinc-500 dark:hover:bg-zinc-800"
        >
          <X size={16} />
        </button>
      </div>
    </div>
  )
}
