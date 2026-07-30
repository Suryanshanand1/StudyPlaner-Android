import { LocalNotifications } from "@capacitor/local-notifications"
import { isPlatform } from "@capacitor/core"

export async function requestNotificationPermission(): Promise<boolean> {
  if (isPlatform("android") || isPlatform("ios")) {
    const perm = await LocalNotifications.requestPermissions()
    return perm.display === "granted"
  }
  if (!("Notification" in window)) return false
  if (Notification.permission === "granted") return true
  if (Notification.permission === "denied") return false
  const p = await Notification.requestPermission()
  return p === "granted"
}

export async function sendStudyNotification(title: string, body: string) {
  if (isPlatform("android") || isPlatform("ios")) {
    try {
      await LocalNotifications.schedule({
        notifications: [
          {
            title,
            body,
            id: Date.now(),
            schedule: { at: new Date() },
          },
        ],
      })
    } catch {}
    return
  }
  if (!("Notification" in window)) return
  if (Notification.permission !== "granted") return
  try {
    new Notification(title, { body, icon: "/icon-192.png" })
  } catch {}
}
