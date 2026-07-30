export function requestNotificationPermission(): Promise<boolean> {
  if (!("Notification" in window)) return Promise.resolve(false)
  if (Notification.permission === "granted") return Promise.resolve(true)
  if (Notification.permission === "denied") return Promise.resolve(false)
  return Notification.requestPermission().then((p) => p === "granted")
}

export function sendStudyNotification(title: string, body: string) {
  if (!("Notification" in window)) return
  if (Notification.permission !== "granted") return
  try {
    new Notification(title, { body, icon: "/favicon.ico" })
  } catch {
  }
}
