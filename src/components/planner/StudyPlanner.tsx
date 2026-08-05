"use client"

import { useState, useMemo } from "react"
import { Plus, Trash2, ChevronLeft, ChevronRight, CheckCircle, Circle } from "lucide-react"
import { useStore } from "@/lib/store"
import { getToday, toDateStr, getWeekDays, formatDate, formatTime } from "@/lib/utils"
import PlanForm from "./PlanForm"

export default function StudyPlanner() {
  const { studyPlans, subjects, deleteStudyPlan, togglePlanConfirmed, getWeekPlans } = useStore()
  const [view, setView] = useState<"day" | "week">("day")
  const [currentDate, setCurrentDate] = useState(getToday())
  const [showForm, setShowForm] = useState(false)

  const weekDays = useMemo(() => getWeekDays(new Date(currentDate)), [currentDate])

  const dayPlans = useMemo(
    () => studyPlans.filter((p) => p.date === currentDate).sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [studyPlans, currentDate],
  )

  const weekPlans = useMemo(
    () => getWeekPlans(weekDays),
    [getWeekPlans, weekDays],
  )

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex gap-1 rounded-xl bg-zinc-100 p-1 dark:bg-zinc-800">
          <button
            onClick={() => setView("day")}
            className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${view === "day" ? "bg-accent text-white shadow-sm" : "text-zinc-500 dark:text-zinc-400"}`}
          >
            Day
          </button>
          <button
            onClick={() => setView("week")}
            className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${view === "week" ? "bg-accent text-white shadow-sm" : "text-zinc-500 dark:text-zinc-400"}`}
          >
            Week
          </button>
        </div>
        <button
          onClick={() => setShowForm(true)}
          className="flex items-center gap-1.5 rounded-xl bg-accent px-4 py-2 text-xs font-medium text-white"
        >
          <Plus size={16} />
          New Plan
        </button>
      </div>

      {view === "day" && (
        <div>
          <div className="mb-3 flex items-center justify-between">
            <button onClick={() => {
              const d = new Date(currentDate)
              d.setDate(d.getDate() - 1)
              setCurrentDate(toDateStr(d))
            }} className="rounded-lg p-1.5 hover:bg-zinc-100 dark:hover:bg-zinc-800">
              <ChevronLeft size={18} className="text-zinc-600 dark:text-zinc-400" />
            </button>
            <span className="text-sm font-semibold text-zinc-700 dark:text-zinc-300">{formatDate(currentDate)}</span>
            <button onClick={() => {
              const d = new Date(currentDate)
              d.setDate(d.getDate() + 1)
              setCurrentDate(toDateStr(d))
            }} className="rounded-lg p-1.5 hover:bg-zinc-100 dark:hover:bg-zinc-800">
              <ChevronRight size={18} className="text-zinc-600 dark:text-zinc-400" />
            </button>
          </div>
          <div className="space-y-2">
            {dayPlans.length === 0 && (
              <p className="py-8 text-center text-sm text-zinc-400 dark:text-zinc-500">No study plans for this day</p>
            )}
            {dayPlans.map((plan) => {
              const sub = subjects.find((s) => s.id === plan.subjectId)
              return (
                <div key={plan.id} className={`flex items-center justify-between rounded-xl border p-3 shadow-sm dark:border-zinc-800 dark:bg-zinc-900 ${plan.confirmed ? "border-green-200 bg-green-50/60 dark:border-green-900 dark:bg-green-950/30" : "border-zinc-100 bg-white"}`}>
                  <div className="flex items-center gap-3">
                    <div className="h-3 w-3 rounded-full" style={{ backgroundColor: sub?.color }} />
                    <div>
                      <p className={`text-sm font-medium ${plan.confirmed ? "line-through text-zinc-400 dark:text-zinc-500" : "text-zinc-800 dark:text-zinc-200"}`}>{plan.subjectName} - {plan.chapterName}</p>
                      <p className="text-xs text-zinc-400 dark:text-zinc-500">{formatTime(plan.startTime)} - {formatTime(plan.endTime)}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => togglePlanConfirmed(plan.id)}
                      className={`rounded-lg p-1.5 transition ${plan.confirmed ? "text-green-500" : "text-zinc-300 hover:text-green-500 dark:text-zinc-600 dark:hover:text-green-400"}`}
                      title={plan.confirmed ? "Mark as not studied" : "Mark as studied"}
                    >
                      {plan.confirmed ? <CheckCircle size={16} /> : <Circle size={16} />}
                    </button>
                    <button onClick={() => deleteStudyPlan(plan.id)} className="rounded-lg p-1.5 text-zinc-300 hover:bg-red-50 hover:text-red-500 dark:text-zinc-600 dark:hover:bg-red-950/40 dark:hover:text-red-400">
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {view === "week" && (
        <div className="space-y-3">
          {weekDays.map((day) => {
            const dayName = new Date(day + "T00:00:00").toLocaleDateString("en-US", { weekday: "short" })
            const dayNum = new Date(day + "T00:00:00").getDate()
            const isToday = day === getToday()
            const plans = weekPlans.filter((p) => p.date === day)
            return (
              <div key={day} className={`rounded-xl border p-3 ${isToday ? "border-zinc-300 bg-zinc-50 dark:border-zinc-600 dark:bg-zinc-800/60" : "border-zinc-100 bg-white dark:border-zinc-800 dark:bg-zinc-900"}`}>
                <div className="mb-2 flex items-center gap-2">
                  <span className={`text-xs font-semibold ${isToday ? "text-zinc-900 dark:text-zinc-100" : "text-zinc-500 dark:text-zinc-400"}`}>{dayName}</span>
                  <span className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-medium ${isToday ? "bg-accent text-white" : "text-zinc-600 dark:text-zinc-400"}`}>{dayNum}</span>
                </div>
                {plans.length === 0 && <p className="text-xs text-zinc-400 dark:text-zinc-500">No plans</p>}
                {plans.map((plan) => {
                  const sub = subjects.find((s) => s.id === plan.subjectId)
                  return (
                    <div key={plan.id} className={`flex items-center justify-between rounded-lg px-2 py-1 ${plan.confirmed ? "bg-green-50 dark:bg-green-950/30" : ""}`}>
                      <div className="flex items-center gap-2">
                        <div className="h-2 w-2 rounded-full" style={{ backgroundColor: sub?.color }} />
                        <span className={`text-xs ${plan.confirmed ? "line-through text-zinc-400 dark:text-zinc-500" : "text-zinc-700 dark:text-zinc-300"}`}>{plan.chapterName}</span>
                        <span className="text-xs text-zinc-400 dark:text-zinc-500">{formatTime(plan.startTime)}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => togglePlanConfirmed(plan.id)}
                          className={`transition ${plan.confirmed ? "text-green-500" : "text-zinc-300 hover:text-green-500 dark:text-zinc-600 dark:hover:text-green-400"}`}
                          title={plan.confirmed ? "Mark as not studied" : "Mark as studied"}
                        >
                          {plan.confirmed ? <CheckCircle size={14} /> : <Circle size={14} />}
                        </button>
                        <button onClick={() => deleteStudyPlan(plan.id)} className="text-zinc-300 hover:text-red-500 dark:text-zinc-600 dark:hover:text-red-400">
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            )
          })}
        </div>
      )}

      {showForm && (
        <PlanForm
          onSave={() => setShowForm(false)}
          onCancel={() => setShowForm(false)}
        />
      )}
    </div>
  )
}
