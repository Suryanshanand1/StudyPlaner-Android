"use client"

import { useEffect, useState } from "react"
import { Play, Pause, RotateCcw, Save, CheckCircle, Timer } from "lucide-react"
import { useStore } from "@/lib/store"
import { getToday } from "@/lib/utils"

const SESSION_KEY = "study-watch-session"
const MIN_SAVE_MS = 60 * 1000

interface WatchSession {
  subjectId: string
  chapterId: string
  subjectName: string
  chapterName: string
  startedDate: string
  startedAt: number
  segmentStartedAt: number
  accumulatedMs: number
  state: "running" | "paused"
}

function pad(n: number): string {
  return String(n).padStart(2, "0")
}

function fmtClock(ms: number): string {
  const totalSec = Math.floor(Math.max(0, ms) / 1000)
  const h = Math.floor(totalSec / 3600)
  const m = Math.floor((totalSec % 3600) / 60)
  const s = totalSec % 60
  return `${pad(h)}:${pad(m)}:${pad(s)}`
}

function toTimeStr(d: Date): string {
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function fmtDuration(ms: number): string {
  const minutes = Math.floor(ms / 60000)
  const hours = Math.floor(minutes / 60)
  return hours > 0 ? `${hours}h ${minutes % 60}m` : `${minutes}m`
}

function loadSession(): WatchSession | null {
  if (typeof window === "undefined") return null
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    if (!raw) return null
    const s = JSON.parse(raw)
    if (!s || typeof s !== "object") return null
    return {
      subjectId: String(s.subjectId ?? ""),
      chapterId: String(s.chapterId ?? ""),
      subjectName: String(s.subjectName ?? ""),
      chapterName: String(s.chapterName ?? ""),
      startedDate: String(s.startedDate ?? ""),
      startedAt: Number(s.startedAt) || Date.now(),
      segmentStartedAt: Number(s.segmentStartedAt) || Date.now(),
      accumulatedMs: Number(s.accumulatedMs) || 0,
      state: s.state === "paused" ? "paused" : "running",
    }
  } catch {
    return null
  }
}

function persistSession(session: WatchSession | null) {
  try {
    if (session) localStorage.setItem(SESSION_KEY, JSON.stringify(session))
    else localStorage.removeItem(SESSION_KEY)
  } catch {}
}

export default function StudyWatch() {
  const { subjects, chapters, addConfirmedStudyPlan } = useStore()
  const [session, setSession] = useState<WatchSession | null>(loadSession)
  const [subjectId, setSubjectId] = useState("")
  const [chapterId, setChapterId] = useState("")
  const [now, setNow] = useState(() => Date.now())
  const [savedLabel, setSavedLabel] = useState<string | null>(null)

  const running = session?.state === "running"

  useEffect(() => {
    if (!running) return
    const id = setInterval(() => setNow(Date.now()), 250)
    return () => clearInterval(id)
  }, [running])

  useEffect(() => {
    persistSession(session)
  }, [session])

  const elapsedMs = session
    ? session.accumulatedMs + (running ? now - session.segmentStartedAt : 0)
    : 0
  const canSave = !!session && elapsedMs >= MIN_SAVE_MS

  const effectiveSubjectId = subjectId !== "" ? subjectId : subjects[0]?.id ?? ""
  const filteredChapters = chapters.filter((ch) => ch.subjectId === effectiveSubjectId)
  const selectedSubject = subjects.find((s) => s.id === effectiveSubjectId)

  const handleStart = () => {
    if (!effectiveSubjectId || !chapterId) return
    const ch = chapters.find((c) => c.id === chapterId)
    const timestamp = Date.now()
    setNow(timestamp)
    setSession({
      subjectId: effectiveSubjectId,
      chapterId,
      subjectName: selectedSubject?.name ?? "",
      chapterName: ch?.name ?? "",
      startedDate: getToday(),
      startedAt: timestamp,
      segmentStartedAt: timestamp,
      accumulatedMs: 0,
      state: "running",
    })
    setSavedLabel(null)
  }

  const handlePause = () => {
    if (!session || session.state !== "running") return
    setSession({
      ...session,
      accumulatedMs: session.accumulatedMs + (Date.now() - session.segmentStartedAt),
      state: "paused",
    })
    setNow(Date.now())
  }

  const handleResume = () => {
    if (!session || session.state !== "paused") return
    setNow(Date.now())
    setSession({ ...session, segmentStartedAt: Date.now(), state: "running" })
  }

  const handleReset = () => {
    setSession(null)
    setSavedLabel(null)
  }

  const handleSave = () => {
    if (!session || !canSave) return
    const total = elapsedMs
    const startDate = new Date(session.startedAt)
    const endDate = new Date(session.startedAt + total)
    addConfirmedStudyPlan({
      subjectId: session.subjectId,
      chapterId: session.chapterId,
      subjectName: session.subjectName,
      chapterName: session.chapterName,
      date: session.startedDate,
      startTime: toTimeStr(startDate),
      endTime: toTimeStr(endDate),
    })
    setSession(null)
    setSavedLabel(`${session.subjectName} — ${session.chapterName} (${fmtDuration(total)})`)
  }

  return (
    <div className="space-y-4">
      <div className="rounded-xl border border-zinc-100 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
        <div className="mb-3 flex items-center gap-2">
          <Timer size={16} className="text-accent" />
          <h3 className="text-sm font-semibold text-zinc-800 dark:text-zinc-200">Study Session</h3>
        </div>

        {!session && (
          <div className="space-y-3">
            <div>
              <label className="mb-1 block text-xs font-medium text-zinc-500 dark:text-zinc-400">Subject</label>
              <select
                value={effectiveSubjectId}
                onChange={(e) => { setSubjectId(e.target.value); setChapterId("") }}
                className="w-full rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none focus:border-accent dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
              >
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </div>
            <div>
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
          </div>
        )}

        {session && (
          <div className="flex items-center gap-2 rounded-xl bg-zinc-100 px-3 py-2 dark:bg-zinc-800">
            <div className="h-3 w-3 rounded-full" style={{ backgroundColor: selectedSubject?.color }} />
            <p className="flex-1 truncate text-sm font-medium text-zinc-700 dark:text-zinc-300">
              {session.subjectName} — {session.chapterName}
            </p>
            <span className={`text-xs font-medium ${running ? "text-accent" : "text-zinc-400"}`}>
              {running ? "Studying" : "Paused"}
            </span>
          </div>
        )}

        <div className="my-6 text-center">
          <p className="font-mono text-5xl font-bold tracking-tight text-zinc-900 tabular-nums dark:text-zinc-100">
            {fmtClock(elapsedMs)}
          </p>
          <p className="mt-2 text-xs text-zinc-400 dark:text-zinc-500">
            {session ? "Session in progress" : "Ready to study"}
          </p>
        </div>

        <div className="flex gap-2">
          {!session ? (
            <button
              onClick={handleStart}
              disabled={!effectiveSubjectId || !chapterId}
              className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-accent py-3 text-sm font-medium text-white disabled:opacity-40"
            >
              <Play size={18} />
              Start
            </button>
          ) : (
            <>
              {running ? (
                <button
                  onClick={handlePause}
                  className="flex items-center justify-center gap-2 rounded-xl border-2 border-zinc-200 bg-white px-4 py-3 text-sm font-medium text-zinc-700 hover:border-zinc-300 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-300 dark:hover:border-zinc-600"
                >
                  <Pause size={18} />
                  Pause
                </button>
              ) : (
                <button
                  onClick={handleResume}
                  className="flex items-center justify-center gap-2 rounded-xl bg-accent px-4 py-3 text-sm font-medium text-white"
                >
                  <Play size={18} />
                  Resume
                </button>
              )}
              <button
                onClick={handleReset}
                className="flex items-center justify-center rounded-xl border-2 border-zinc-200 bg-white px-4 py-3 text-zinc-500 hover:border-zinc-300 hover:text-red-500 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-400 dark:hover:border-zinc-600 dark:hover:text-red-400"
                title="Discard session"
              >
                <RotateCcw size={18} />
              </button>
              <button
                onClick={handleSave}
                disabled={!canSave}
                className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-accent py-3 text-sm font-medium text-white disabled:opacity-40"
              >
                <Save size={18} />
                Save Session
              </button>
            </>
          )}
        </div>

        {session && elapsedMs < MIN_SAVE_MS && (
          <p className="mt-2 text-center text-xs text-zinc-400 dark:text-zinc-500">
            Study for at least 1 minute before saving
          </p>
        )}
      </div>

      {savedLabel && (
        <div className="flex items-start gap-3 rounded-xl border border-green-200 bg-green-50 p-4 dark:border-green-900 dark:bg-green-950/30">
          <CheckCircle size={18} className="mt-0.5 shrink-0 text-green-500" />
          <div className="min-w-0">
            <p className="text-sm font-medium text-green-800 dark:text-green-300">Session saved to schedule</p>
            <p className="mt-0.5 truncate text-xs text-green-600 dark:text-green-400">{savedLabel}</p>
          </div>
        </div>
      )}
    </div>
  )
}