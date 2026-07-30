import { LocalNotifications } from "@capacitor/local-notifications"
import type { StudyPlan } from "./types"

function isNative(): boolean {
  return typeof window !== "undefined" && !!(window as any).Capacitor?.isNativePlatform
}

function planIdToNumber(id: string): number {
  let hash = 0
  for (let i = 0; i < id.length; i++) {
    hash = (hash * 31 + id.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}

export async function requestNotificationPermission(): Promise<boolean> {
  if (isNative()) {
    try {
      const perm = await LocalNotifications.requestPermissions()
      return perm.display === "granted"
    } catch {
      return false
    }
  }
  if (!("Notification" in window)) return false
  if (Notification.permission === "granted") return true
  if (Notification.permission === "denied") return false
  const p = await Notification.requestPermission()
  return p === "granted"
}

export async function getPermissionStatus(): Promise<"granted" | "denied" | "prompt"> {
  if (isNative()) {
    try {
      const perm = await LocalNotifications.requestPermissions()
      return perm.display as "granted" | "denied" | "prompt"
    } catch {
      return "denied"
    }
  }
  if (!("Notification" in window)) return "denied"
  return Notification.permission as "granted" | "denied" | "prompt"
}

export async function scheduleStudyNotification(plan: StudyPlan): Promise<void> {
  const title = "Study Time!"
  const body = `${plan.subjectName}: ${plan.chapterName}`
  const id = planIdToNumber(plan.id)

  const [year, month, day] = plan.date.split("-").map(Number)
  const [hour, minute] = plan.startTime.split(":").map(Number)
  const at = new Date(year, month - 1, day, hour, minute)

  if (at.getTime() <= Date.now()) return

  if (isNative()) {
    try {
      await LocalNotifications.schedule({
        notifications: [
          {
            id,
            title,
            body,
            schedule: { at },
            sound: undefined,
          },
        ],
      })
    } catch {
      /* fallback to web */
    }
  } else {
    sendWebNotification(title, body)
  }
}

export async function cancelStudyNotification(planId: string): Promise<void> {
  if (isNative()) {
    try {
      await LocalNotifications.cancel({ notifications: [{ id: planIdToNumber(planId) }] })
    } catch {
      /* ignore */
    }
  }
}

export async function cancelAllStudyNotifications(): Promise<void> {
  if (isNative()) {
    try {
      const pending = await LocalNotifications.getPending()
      if (pending.notifications.length > 0) {
        await LocalNotifications.cancel(pending)
      }
    } catch {
      /* ignore */
    }
  }
}

export async function rescheduleAllPlans(plans: StudyPlan[]): Promise<void> {
  await cancelAllStudyNotifications()
  const now = Date.now()
  for (const plan of plans) {
    const [year, month, day] = plan.date.split("-").map(Number)
    const [hour, minute] = plan.startTime.split(":").map(Number)
    const at = new Date(year, month - 1, day, hour, minute)
    if (at.getTime() <= now) continue

    if (isNative()) {
      try {
        await LocalNotifications.schedule({
          notifications: [
            {
              id: planIdToNumber(plan.id),
              title: "Study Time!",
              body: `${plan.subjectName}: ${plan.chapterName}`,
              schedule: { at },
              sound: undefined,
            },
          ],
        })
      } catch {
        /* ignore */
      }
    }
  }
}

function sendWebNotification(title: string, body: string) {
  if (!("Notification" in window)) return
  if (Notification.permission !== "granted") return
  try {
    new Notification(title, { body, icon: "/icon-192.png" })
  } catch {
    /* ignore */
  }
}
