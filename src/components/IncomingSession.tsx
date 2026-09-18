"use client"

import { useEffect, useState } from "react"
import { App } from "@capacitor/app"
import { useStore } from "@/lib/store"

interface IncomingSession {
  label: string
  date: string
  startTime: string
  endTime: string
  minutes: number
}

function parseSessionUrl(url: string): IncomingSession | null {
  let parsed: URL
  try {
    parsed = new URL(url)
  } catch {
    return null
  }
  if (parsed.protocol !== "studyplanner:" || parsed.hostname !== "session") return null
  const q = parsed.searchParams
  const date = q.get("date") ?? ""
  const start = q.get("start") ?? ""
  const end = q.get("end") ?? ""
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return null
  if (!/^\d{2}:\d{2}$/.test(start) || !/^\d{2}:\d{2}$/.test(end)) return null
  return {
    label: (q.get("label") || "Allies session").slice(0, 100),
    date,
    startTime: start,
    endTime: end,
    minutes: Math.max(0, Number(q.get("minutes")) || 0),
  }
}

export default function IncomingSession() {
  const { subjects, chapters, addConfirmedStudyPlan } = useStore()
  const [incoming, setIncoming] = useState<IncomingSession | null>(null)
  const [subjectId, setSubjectId] = useState("")
  const [chapterId, setChapterId] = useState("")

  useEffect(() => {
    let alive = true
    App.getLaunchUrl()
      .then((launchUrl) => {
        const url = launchUrl?.url
        if (alive && url) {
          const parsed = parseSessionUrl(url)
          if (parsed) setIncoming(parsed)
        }
      })
      .catch(() => {})
    const sub = App.addListener("appUrlOpen", ({ url }) => {
      const parsed = parseSessionUrl(url)
      if (parsed) {
        setSubjectId("")
        setChapterId("")
        setIncoming(parsed)
      }
    })
    return () => {
      alive = false
      void sub.then((s) => s.remove())
    }
  }, [])

  if (!incoming) return null

  const subjectChapters = chapters.filter((c) => c.subjectId === subjectId)
  const canSave = !!subjectId && !!chapterId

  const handleSave = () => {
    if (!canSave) return
    addConfirmedStudyPlan({
      subjectId,
      chapterId,
      subjectName: subjects.find((s) => s.id === subjectId)?.name ?? "",
      chapterName: subjectChapters.find((c) => c.id === chapterId)?.name ?? "",
      date: incoming.date,
      startTime: incoming.startTime,
      endTime: incoming.endTime,
    })
    setIncoming(null)
    setSubjectId("")
    setChapterId("")
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 dark:bg-black/60">
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl dark:bg-zinc-900">
        <h2 className="text-lg font-semibold">Session from Allies</h2>
        <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
          {incoming.label} · {incoming.date} · {incoming.startTime}–{incoming.endTime}
          {incoming.minutes > 0 ? ` (${incoming.minutes} min)` : ""}
        </p>

        <div className="mt-4 space-y-3">
          <div>
            <label className="mb-1 block text-xs font-medium text-zinc-500 dark:text-zinc-400">Subject</label>
            <select
              value={subjectId}
              onChange={(e) => {
                setSubjectId(e.target.value)
                setChapterId("")
              }}
              className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-3 text-sm outline-none dark:border-zinc-700 dark:bg-zinc-800"
            >
              <option value="">Select subject</option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>

          {subjectId && (
            <div>
              <label className="mb-1 block text-xs font-medium text-zinc-500 dark:text-zinc-400">Chapter</label>
              <select
                value={chapterId}
                onChange={(e) => setChapterId(e.target.value)}
                className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-3 text-sm outline-none dark:border-zinc-700 dark:bg-zinc-800"
              >
                <option value="">Select chapter</option>
                {subjectChapters.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
              {subjectChapters.length === 0 && (
                <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">This subject has no chapters yet — add one first.</p>
              )}
            </div>
          )}
        </div>

        <div className="mt-5 flex gap-2">
          <button
            onClick={() => setIncoming(null)}
            className="flex-1 rounded-xl border border-zinc-200 py-3 text-sm font-medium dark:border-zinc-700"
          >
            Discard
          </button>
          <button
            onClick={handleSave}
            disabled={!canSave}
            className="flex-1 rounded-xl bg-zinc-900 py-3 text-sm font-semibold text-white disabled:opacity-40 dark:bg-white dark:text-zinc-900"
          >
            Save as confirmed
          </button>
        </div>
      </div>
    </div>
  )
}