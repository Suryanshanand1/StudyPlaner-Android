"use client"

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react"
import type { Subject, Chapter, StudyPlan, StudySession } from "./types"
import { generateId } from "./utils"

interface AppState {
  subjects: Subject[]
  chapters: Chapter[]
  studyPlans: StudyPlan[]
}

interface AppStore {
  subjects: Subject[]
  chapters: Chapter[]
  studyPlans: StudyPlan[]
  addSubject: (name: string, color: string) => void
  editSubject: (id: string, name: string, color: string) => void
  deleteSubject: (id: string) => void
  addChapter: (subjectId: string, name: string) => void
  toggleChapter: (id: string) => void
  deleteChapter: (id: string) => void
  reorderChapter: (chapterId: string, subjectId: string, toIndex: number) => void
  addStudyPlan: (plan: Omit<StudyPlan, "id" | "createdAt">) => void
  deleteStudyPlan: (id: string) => void
  getCompletionPercent: (subjectId: string) => number
  getUpcomingPlans: (date: string) => StudyPlan[]
  getWeekPlans: (weekDays: string[]) => StudyPlan[]
  getMonthlySessions: (year: number, month: number) => StudySession[]
  exportData: () => string
  importData: (json: string) => boolean
}

const StoreContext = createContext<AppStore | null>(null)

const STORAGE_KEY = "study-planner-data"

export const BACKUP_VERSION = 1

function normalizeList<T>(
  list: unknown,
  defaults: (item: Record<string, unknown>, index: number) => Record<string, unknown>,
  fallback: T,
): T {
  if (!Array.isArray(list)) return fallback
  return list.map((item, i) => {
    const known = defaults(item as Record<string, unknown>, i)
    const out: Record<string, unknown> = { ...known }
    if (item && typeof item === "object") {
      for (const key of Object.keys(item)) {
        if (!(key in out)) out[key] = (item as Record<string, unknown>)[key]
      }
    }
    return out as unknown
  }) as unknown as T
}

function normalizeState(raw: unknown): AppState {
  const src = raw && typeof raw === "object" ? (raw as Record<string, unknown>) : {}
  const now = Date.now()
  return {
    subjects: normalizeList<Subject[]>(src.subjects, (s, i) => ({
      id: typeof s?.id === "string" ? s.id : `s-${i}`,
      name: typeof s?.name === "string" ? s.name : "",
      color: typeof s?.color === "string" ? s.color : "#3b82f6",
      createdAt: typeof s?.createdAt === "number" ? s.createdAt : now,
    }), []),
    chapters: normalizeList<Chapter[]>(src.chapters, (ch, i) => ({
      id: typeof ch?.id === "string" ? ch.id : `c-${i}`,
      subjectId: typeof ch?.subjectId === "string" ? ch.subjectId : "",
      name: typeof ch?.name === "string" ? ch.name : "",
      completed: !!ch?.completed,
      order: typeof ch?.order === "number" ? ch.order : i,
      createdAt: typeof ch?.createdAt === "number" ? ch.createdAt : now,
    }), []),
    studyPlans: normalizeList<StudyPlan[]>(src.studyPlans, (p, i) => ({
      id: typeof p?.id === "string" ? p.id : `p-${i}`,
      subjectId: typeof p?.subjectId === "string" ? p.subjectId : "",
      chapterId: typeof p?.chapterId === "string" ? p.chapterId : "",
      subjectName: typeof p?.subjectName === "string" ? p.subjectName : "",
      chapterName: typeof p?.chapterName === "string" ? p.chapterName : "",
      date: typeof p?.date === "string" ? p.date : "",
      startTime: typeof p?.startTime === "string" ? p.startTime : "",
      endTime: typeof p?.endTime === "string" ? p.endTime : "",
      createdAt: typeof p?.createdAt === "number" ? p.createdAt : now,
    }), []),
  }
}

function loadState(): AppState {
  if (typeof window === "undefined") return { subjects: [], chapters: [], studyPlans: [] }
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return normalizeState(JSON.parse(raw))
  } catch {}
  return { subjects: [], chapters: [], studyPlans: [] }
}

function saveState(state: AppState) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch {}
}

export function StoreProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AppState>({ subjects: [], chapters: [], studyPlans: [] })
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    setState(loadState())
    setLoaded(true)
  }, [])

  useEffect(() => {
    if (loaded) saveState(state)
  }, [state, loaded])

  const addSubject = useCallback((name: string, color: string) => {
    setState((s) => ({
      ...s,
      subjects: [...s.subjects, { id: generateId(), name, color, createdAt: Date.now() }],
    }))
  }, [])

  const editSubject = useCallback((id: string, name: string, color: string) => {
    setState((s) => ({
      ...s,
      subjects: s.subjects.map((sub) => (sub.id === id ? { ...sub, name, color } : sub)),
    }))
  }, [])

  const deleteSubject = useCallback((id: string) => {
    setState((s) => ({
      subjects: s.subjects.filter((sub) => sub.id !== id),
      chapters: s.chapters.filter((ch) => ch.subjectId !== id),
      studyPlans: s.studyPlans.filter((p) => p.subjectId !== id),
    }))
  }, [])

  const addChapter = useCallback((subjectId: string, name: string) => {
    setState((s) => {
      const subjectChapters = s.chapters.filter((ch) => ch.subjectId === subjectId)
      const maxOrder = subjectChapters.length > 0 ? Math.max(...subjectChapters.map((ch) => ch.order)) : -1
      return {
        ...s,
        chapters: [...s.chapters, { id: generateId(), subjectId, name, completed: false, order: maxOrder + 1, createdAt: Date.now() }],
      }
    })
  }, [])

  const toggleChapter = useCallback((id: string) => {
    setState((s) => ({
      ...s,
      chapters: s.chapters.map((ch) => (ch.id === id ? { ...ch, completed: !ch.completed } : ch)),
    }))
  }, [])

  const deleteChapter = useCallback((id: string) => {
    setState((s) => ({
      ...s,
      chapters: s.chapters.filter((ch) => ch.id !== id),
      studyPlans: s.studyPlans.filter((p) => p.chapterId !== id),
    }))
  }, [])

  const reorderChapter = useCallback((chapterId: string, subjectId: string, toIndex: number) => {
    setState((s) => {
      const subjectChs = s.chapters
        .filter((ch) => ch.subjectId === subjectId)
        .sort((a, b) => a.order - b.order)
      const fromIndex = subjectChs.findIndex((ch) => ch.id === chapterId)
      if (fromIndex === -1) return s
      const [moved] = subjectChs.splice(fromIndex, 1)
      subjectChs.splice(toIndex, 0, moved)
      const reordered = subjectChs.map((ch, i) => ({ ...ch, order: i }))
      const otherChs = s.chapters.filter((ch) => ch.subjectId !== subjectId)
      return { ...s, chapters: [...otherChs, ...reordered] }
    })
  }, [])

  const addStudyPlan = useCallback((plan: Omit<StudyPlan, "id" | "createdAt">) => {
    setState((s) => ({
      ...s,
      studyPlans: [...s.studyPlans, { ...plan, id: generateId(), createdAt: Date.now() }],
    }))
  }, [])

  const deleteStudyPlan = useCallback((id: string) => {
    setState((s) => ({
      ...s,
      studyPlans: s.studyPlans.filter((p) => p.id !== id),
    }))
  }, [])

  const getCompletionPercent = useCallback(
    (subjectId: string) => {
      const chs = state.chapters.filter((ch) => ch.subjectId === subjectId)
      if (chs.length === 0) return 0
      return Math.round((chs.filter((ch) => ch.completed).length / chs.length) * 100)
    },
    [state.chapters],
  )

  const getUpcomingPlans = useCallback(
    (date: string) => {
      return state.studyPlans.filter((p) => p.date >= date).sort((a, b) => a.date.localeCompare(b.date) || a.startTime.localeCompare(b.startTime))
    },
    [state.studyPlans],
  )

  const getWeekPlans = useCallback(
    (weekDays: string[]) => {
      return state.studyPlans.filter((p) => weekDays.includes(p.date)).sort((a, b) => a.date.localeCompare(b.date) || a.startTime.localeCompare(b.startTime))
    },
    [state.studyPlans],
  )

  const getMonthlySessions = useCallback(
    (year: number, month: number) => {
      const days = new Date(year, month + 1, 0).getDate()
      const sessions: StudySession[] = []
      for (let d = 1; d <= days; d++) {
        const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`
        const dayPlans = state.studyPlans.filter((p) => p.date === dateStr)
        let hours = 0
        let chaptersCompleted = 0
        for (const plan of dayPlans) {
          const [sh, sm] = plan.startTime.split(":").map(Number)
          const [eh, em] = plan.endTime.split(":").map(Number)
          hours += (eh * 60 + em - (sh * 60 + sm)) / 60
          const ch = state.chapters.find((c) => c.id === plan.chapterId)
          if (ch?.completed) chaptersCompleted++
        }
        sessions.push({ date: dateStr, hours: Math.round(hours * 10) / 10, chaptersCompleted })
      }
      return sessions
    },
    [state.studyPlans, state.chapters],
  )

  const exportData = useCallback(() => {
    return JSON.stringify({ version: BACKUP_VERSION, ...state }, null, 2)
  }, [state])

  const importData = useCallback((json: string) => {
    try {
      const data = JSON.parse(json)
      if (!data || typeof data !== "object") return false
      if (!Array.isArray(data.subjects) || !Array.isArray(data.chapters) || !Array.isArray(data.studyPlans)) return false
      setState(normalizeState(data))
      return true
    } catch {
      return false
    }
  }, [])

  return (
    <StoreContext.Provider
      value={{
        subjects: state.subjects,
        chapters: state.chapters,
        studyPlans: state.studyPlans,
        addSubject,
        editSubject,
        deleteSubject,
        addChapter,
        toggleChapter,
        deleteChapter,
        reorderChapter,
        addStudyPlan,
        deleteStudyPlan,
        getCompletionPercent,
        getUpcomingPlans,
        getWeekPlans,
        getMonthlySessions,
        exportData,
        importData,
      }}
    >
      {children}
    </StoreContext.Provider>
  )
}

export function useStore() {
  const ctx = useContext(StoreContext)
  if (!ctx) throw new Error("useStore must be used within StoreProvider")
  return ctx
}
