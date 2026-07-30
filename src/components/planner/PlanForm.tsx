"use client"

import { useState } from "react"
import { X } from "lucide-react"
import { useStore } from "@/lib/store"
import { getToday } from "@/lib/utils"

interface PlanFormProps {
  onSave: () => void
  onCancel: () => void
}

export default function PlanForm({ onSave, onCancel }: PlanFormProps) {
  const { subjects, chapters, addStudyPlan } = useStore()
  const [subjectId, setSubjectId] = useState(subjects[0]?.id ?? "")
  const [chapterId, setChapterId] = useState("")
  const [date, setDate] = useState(getToday())
  const [startTime, setStartTime] = useState("09:00")
  const [endTime, setEndTime] = useState("10:00")

  const filteredChapters = chapters.filter((ch) => ch.subjectId === subjectId)
  const selectedSubject = subjects.find((s) => s.id === subjectId)

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 dark:bg-black/60">
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl dark:bg-zinc-900">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold dark:text-zinc-100">New Study Plan</h2>
          <button onClick={onCancel} className="rounded-full p-1 hover:bg-zinc-100 dark:hover:bg-zinc-800">
            <X size={20} className="text-zinc-500 dark:text-zinc-400" />
          </button>
        </div>

        <div className="mb-3">
          <label className="mb-1 block text-xs font-medium text-zinc-500 dark:text-zinc-400">Subject</label>
          <select
            value={subjectId}
            onChange={(e) => { setSubjectId(e.target.value); setChapterId("") }}
            className="w-full rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none focus:border-accent dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
          >
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>

        <div className="mb-3">
          <label className="mb-1 block text-xs font-medium text-zinc-500 dark:text-zinc-400">Chapter</label>
          <select
            value={chapterId}
            onChange={(e) => setChapterId(e.target.value)}
            className="w-full rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none focus:border-accent dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
          >
            <option value="">Select chapter</option>
            {filteredChapters.map((ch) => (
              <option key={ch.id} value={ch.id}>{ch.name}</option>
            ))}
          </select>
        </div>

        <div className="mb-3">
          <label className="mb-1 block text-xs font-medium text-zinc-500 dark:text-zinc-400">Date</label>
          <input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-full rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none focus:border-accent dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
          />
        </div>

        <div className="mb-4 grid grid-cols-2 gap-3">
          <div>
            <label className="mb-1 block text-xs font-medium text-zinc-500 dark:text-zinc-400">Start</label>
            <input
              type="time"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              className="w-full rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none focus:border-accent dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-zinc-500 dark:text-zinc-400">End</label>
            <input
              type="time"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              className="w-full rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none focus:border-accent dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
            />
          </div>
        </div>

        <button
          onClick={() => {
            if (!chapterId || !subjectId) return
            const ch = chapters.find((c) => c.id === chapterId)
            addStudyPlan({
              subjectId,
              chapterId,
              subjectName: selectedSubject?.name ?? "",
              chapterName: ch?.name ?? "",
              date,
              startTime,
              endTime,
            })
            onSave()
          }}
          disabled={!chapterId || !subjectId}
          className="w-full rounded-xl bg-accent py-3 text-sm font-medium text-white disabled:opacity-40"
        >
          Add to Schedule
        </button>
      </div>
    </div>
  )
}
