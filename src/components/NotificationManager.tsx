"use client"

import { useEffect, useRef } from "react"
import { useStore } from "@/lib/store"
import { requestNotificationPermission, rescheduleAllPlans, cancelAllStudyNotifications, scheduleStudyNotification, cancelStudyNotification } from "@/lib/notifications"

export default function NotificationManager() {
  const { studyPlans } = useStore()
  const prevPlansRef = useRef<string>("")

  useEffect(() => {
    requestNotificationPermission()
  }, [])

  useEffect(() => {
    const serialized = JSON.stringify(studyPlans.map((p) => p.id + p.date + p.startTime))
    if (serialized === prevPlansRef.current) return
    prevPlansRef.current = serialized

    rescheduleAllPlans(studyPlans)
  }, [studyPlans])

  return null
}
