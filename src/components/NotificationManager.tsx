"use client"

import { useEffect, useRef } from "react"
import { useStore } from "@/lib/store"
import { requestNotificationPermission, sendStudyNotification } from "@/lib/notifications"

export default function NotificationManager() {
  const { studyPlans } = useStore()
  const notifiedRef = useRef<Set<string>>(new Set())

  useEffect(() => {
    requestNotificationPermission()
  }, [])

  useEffect(() => {
    const interval = setInterval(() => {
      const now = new Date()
      const currentTime = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`
      const today = now.toISOString().split("T")[0]

      for (const plan of studyPlans) {
        if (plan.date !== today) continue
        if (plan.startTime !== currentTime) continue
        const key = plan.id
        if (notifiedRef.current.has(key)) continue
        notifiedRef.current.add(key)
        sendStudyNotification(
          "Study Time!",
          `${plan.subjectName}: ${plan.chapterName}`,
        )
      }
    }, 30000)

    return () => clearInterval(interval)
  }, [studyPlans])

  return null
}
