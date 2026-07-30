"use client"

import { useMemo, useState, useRef } from "react"
import { Plus, Trash2, CheckCircle, Circle, Pencil, GripVertical, ChevronDown, ChevronRight } from "lucide-react"
import type { Subject, Chapter } from "@/lib/types"
import { useStore } from "@/lib/store"
import ChapterForm from "./ChapterForm"

export default function SubjectCard({
  subject,
  chapters,
  onEdit,
}: {
  subject: Subject
  chapters: Chapter[]
  onEdit: () => void
}) {
  const { addChapter, toggleChapter, deleteChapter, deleteSubject, getCompletionPercent, reorderChapter } = useStore()
  const [collapsed, setCollapsed] = useState(false)
  const [showForm, setShowForm] = useState(false)
  const [dragId, setDragId] = useState<string | null>(null)
  const [dragOverId, setDragOverId] = useState<string | null>(null)
  const percent = getCompletionPercent(subject.id)

  const sorted = useMemo(
    () => [...chapters].sort((a, b) => a.order - b.order),
    [chapters],
  )

  return (
    <div className="rounded-2xl border border-zinc-100 bg-white p-4 shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
      <div className="mb-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="h-4 w-4 rounded-full" style={{ backgroundColor: subject.color }} />
          <h3 className="font-semibold text-zinc-800 dark:text-zinc-200">{subject.name}</h3>
          <button onClick={() => setCollapsed(!collapsed)} className="rounded-lg p-1 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-600 dark:hover:bg-zinc-800 dark:hover:text-zinc-300">
            {collapsed ? <ChevronRight size={16} /> : <ChevronDown size={16} />}
          </button>
        </div>
        <div className="flex items-center gap-1">
          <button onClick={onEdit} className="rounded-lg p-1.5 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-600 dark:hover:bg-zinc-800 dark:hover:text-zinc-300">
            <Pencil size={16} />
          </button>
          <button
            onClick={() => deleteSubject(subject.id)}
            className="rounded-lg p-1.5 text-zinc-400 hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-950/40 dark:hover:text-red-400"
          >
            <Trash2 size={16} />
          </button>
        </div>
      </div>

      <div className="mb-3">
        <div className="mb-1 flex items-center justify-between text-xs text-zinc-500 dark:text-zinc-400">
          <span>Progress</span>
          <span>{percent}%</span>
        </div>
        <div className="h-2 rounded-full bg-zinc-100 dark:bg-zinc-800">
          <div
            className="h-2 rounded-full transition-all"
            style={{ width: `${percent}%`, backgroundColor: subject.color }}
          />
        </div>
      </div>

      {collapsed ? (
        sorted.length > 0 && (
          <div className="mb-2 rounded-lg bg-zinc-50 px-3 py-2 text-xs text-zinc-500 dark:bg-zinc-800/60 dark:text-zinc-400">
            {sorted.filter((ch) => ch.completed).length}/{sorted.length} chapters completed
          </div>
        )
      ) : (
        <div className="space-y-0.5">
          {sorted.map((ch, idx) => (
            <div
              key={ch.id}
              draggable
              onDragStart={() => setDragId(ch.id)}
              onDragEnd={() => { setDragId(null); setDragOverId(null) }}
              onDragOver={(e) => {
                e.preventDefault()
                setDragOverId(ch.id)
              }}
              onDragLeave={() => setDragOverId(null)}
              onDrop={(e) => {
                e.preventDefault()
                if (dragId && dragId !== ch.id) {
                  reorderChapter(dragId, subject.id, idx)
                }
                setDragId(null)
                setDragOverId(null)
              }}
              className={`flex items-center justify-between rounded-lg px-1 py-1.5 transition ${
                dragOverId === ch.id ? "bg-zinc-100 dark:bg-zinc-700" : dragId === ch.id ? "opacity-50" : "hover:bg-zinc-50 dark:hover:bg-zinc-800/60"
              }`}
            >
              <div className="flex min-w-0 flex-1 items-center gap-1.5">
                <div className="cursor-grab touch-none text-zinc-300 dark:text-zinc-600" onMouseDown={(e) => e.currentTarget.closest<HTMLDivElement>("[draggable]")?.setAttribute("draggable", "true")}>
                  <GripVertical size={16} />
                </div>
                <button onClick={() => toggleChapter(ch.id)} className="shrink-0">
                  {ch.completed ? (
                    <CheckCircle size={18} className="text-green-500" />
                  ) : (
                    <Circle size={18} className="text-zinc-300 dark:text-zinc-600" />
                  )}
                </button>
                <span className={`truncate text-sm ${ch.completed ? "text-zinc-400 line-through dark:text-zinc-500" : "text-zinc-700 dark:text-zinc-300"}`}>
                  {ch.name}
                </span>
              </div>
              <button
                onClick={() => deleteChapter(ch.id)}
                className="shrink-0 rounded p-1 text-zinc-300 hover:bg-red-50 hover:text-red-500 dark:text-zinc-600 dark:hover:bg-red-950/40 dark:hover:text-red-400"
              >
                <Trash2 size={14} />
              </button>
            </div>
          ))}
        </div>
      )}

      <button
        onClick={() => setShowForm(true)}
        className="mt-2 flex w-full items-center justify-center gap-1.5 rounded-xl border border-dashed border-zinc-200 py-2 text-sm text-zinc-400 hover:border-zinc-300 hover:text-zinc-600 dark:border-zinc-700 dark:text-zinc-500 dark:hover:border-zinc-600 dark:hover:text-zinc-300"
      >
        <Plus size={16} />
        Add Chapter
      </button>

      {showForm && (
        <ChapterForm
          onSave={(name) => {
            addChapter(subject.id, name)
            setShowForm(false)
          }}
          onCancel={() => setShowForm(false)}
        />
      )}
    </div>
  )
}
