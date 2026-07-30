"use client"

import { useMemo } from "react"
import { BookOpen, Calendar, TrendingUp, Clock, Flame, Target } from "lucide-react"
import { useStore } from "@/lib/store"
import { getToday, formatDate, formatTime } from "@/lib/utils"

export default function Dashboard() {
  const { subjects, chapters, studyPlans, getUpcomingPlans } = useStore()
  const today = getToday()

  const greeting = useMemo(() => {
    const h = new Date().getHours()
    if (h < 12) return "Good Morning"
    if (h < 17) return "Good Afternoon"
    return "Good Evening"
  }, [])

  const todayPlans = useMemo(
    () => studyPlans.filter((p) => p.date === today).sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [studyPlans, today],
  )

  const upcomingPlans = useMemo(() => getUpcomingPlans(today), [getUpcomingPlans, today])

  const todayHours = useMemo(
    () =>
      todayPlans.reduce((sum, p) => {
        const [sh, sm] = p.startTime.split(":").map(Number)
        const [eh, em] = p.endTime.split(":").map(Number)
        return sum + (eh * 60 + em - (sh * 60 + sm)) / 60
      }, 0),
    [todayPlans],
  )

  const totalChapters = useMemo(() => chapters.length, [chapters])
  const completedChapters = useMemo(() => chapters.filter((c) => c.completed).length, [chapters])
  const overallPercent = useMemo(
    () => (totalChapters > 0 ? Math.round((completedChapters / totalChapters) * 100) : 0),
    [totalChapters, completedChapters],
  )

  const streak = useMemo(() => {
    let count = 0
    const d = new Date()
    while (true) {
      const dateStr = d.toISOString().split("T")[0]
      const hasPlans = studyPlans.some((p) => p.date === dateStr)
      if (!hasPlans) break
      count++
      d.setDate(d.getDate() - 1)
    }
    return count
  }, [studyPlans])

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-bold text-zinc-900 dark:text-zinc-100">{greeting}!</h2>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">{formatDate(today)}</p>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="rounded-xl border border-zinc-100 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
          <div className="mb-2 flex items-center gap-2">
            <BookOpen size={16} className="text-accent" />
            <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400">Subjects</span>
          </div>
          <p className="text-2xl font-bold text-zinc-800 dark:text-zinc-200">{subjects.length}</p>
        </div>

        <div className="rounded-xl border border-zinc-100 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
          <div className="mb-2 flex items-center gap-2">
            <Target size={16} className="text-accent" />
            <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400">Progress</span>
          </div>
          <div className="flex items-center gap-2">
            <p className="text-2xl font-bold text-zinc-800 dark:text-zinc-200">{overallPercent}%</p>
            <div className="flex-1">
              <div className="h-2 rounded-full bg-zinc-100 dark:bg-zinc-800">
                <div className="h-2 rounded-full bg-accent" style={{ width: `${overallPercent}%` }} />
              </div>
              <p className="mt-0.5 text-xs text-zinc-400 dark:text-zinc-500">{completedChapters}/{totalChapters} chapters</p>
            </div>
          </div>
        </div>

        <div className="rounded-xl border border-zinc-100 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
          <div className="mb-2 flex items-center gap-2">
            <Clock size={16} className="text-accent" />
            <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400">Today</span>
          </div>
          <p className="text-2xl font-bold text-zinc-800 dark:text-zinc-200">{todayHours.toFixed(1)}h</p>
          <p className="text-xs text-zinc-400 dark:text-zinc-500">{todayPlans.length} sessions</p>
        </div>

        <div className="rounded-xl border border-zinc-100 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
          <div className="mb-2 flex items-center gap-2">
            <Flame size={16} className={streak > 0 ? "text-orange-500" : "text-zinc-400"} />
            <span className="text-xs font-medium text-zinc-500 dark:text-zinc-400">Streak</span>
          </div>
          <p className="text-2xl font-bold text-zinc-800 dark:text-zinc-200">{streak}</p>
          <p className="text-xs text-zinc-400 dark:text-zinc-500">consecutive days</p>
        </div>
      </div>

      {todayPlans.length > 0 && (
        <div className="rounded-xl border border-zinc-100 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
          <div className="mb-3 flex items-center gap-2">
            <Calendar size={16} className="text-accent" />
            <h3 className="text-sm font-semibold text-zinc-800 dark:text-zinc-200">Today's Schedule</h3>
          </div>
          <div className="space-y-2">
            {todayPlans.slice(0, 5).map((plan) => (
              <div key={plan.id} className="flex items-center gap-3 text-sm">
                <span className="min-w-[52px] text-xs font-medium text-accent">{formatTime(plan.startTime)}</span>
                <span className="text-zinc-700 dark:text-zinc-300">{plan.subjectName} — {plan.chapterName}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {upcomingPlans.length > 0 && (
        <div className="rounded-xl border border-zinc-100 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
          <div className="mb-3 flex items-center gap-2">
            <TrendingUp size={16} className="text-accent" />
            <h3 className="text-sm font-semibold text-zinc-800 dark:text-zinc-200">Upcoming</h3>
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400">{upcomingPlans.length} study sessions planned ahead</p>
        </div>
      )}
    </div>
  )
}
