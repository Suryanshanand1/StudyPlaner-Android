"use client"

import { useState } from "react"
import { Check, X } from "lucide-react"
import { useStore } from "@/lib/store"
import { SUBJECT_COLORS } from "@/lib/utils"

interface CustomSubjectAddProps {
  onCreated: (subjectId: string, chapterId: string) => void
  onCancel: () => void
}

/**
 * Inline quick-add for a brand-new subject plus a starter chapter
 * (default "Revision"), used wherever a subject has to be picked today.
 * Creates both through the store and reports the new ids upward.
 */
export default function CustomSubjectAdd({ onCreated, onCancel }: CustomSubjectAddProps) {
  const { addSubject, addChapter } = useStore()
  const [name, setName] = useState("")
  const [chapterName, setChapterName] = useState("Revision")
  const [color, setColor] = useState(SUBJECT_COLORS[0])

  const valid = name.trim().length > 0

  const handleCreate = () => {
    if (!valid) return
    const subjectId = addSubject(name.trim(), color)
    const chapterId = addChapter(subjectId, chapterName.trim() || "Revision")
    onCreated(subjectId, chapterId)
  }

  return (
    <div className="space-y-3 rounded-xl border border-dashed border-zinc-300 p-3 dark:border-zinc-600">
      <div className="flex items-center justify-between">
        <p className="text-xs font-semibold text-zinc-600 dark:text-zinc-300">Add custom subject</p>
        <button onClick={onCancel} className="rounded-full p-1 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-600 dark:hover:bg-zinc-800 dark:hover:text-zinc-300">
          <X size={16} />
        </button>
      </div>

      <input
        autoFocus
        placeholder="Subject name (e.g. Revision)"
        value={name}
        onChange={(e) => setName(e.target.value)}
        className="w-full rounded-xl border border-zinc-200 px-4 py-2.5 text-sm outline-none focus:border-accent dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
      />

      <input
        placeholder="Starter chapter (default: Revision)"
        value={chapterName}
        onChange={(e) => setChapterName(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" && valid) handleCreate()
        }}
        className="w-full rounded-xl border border-zinc-200 px-4 py-2.5 text-sm outline-none focus:border-accent dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
      />

      <div className="flex gap-2">
        {SUBJECT_COLORS.map((c) => (
          <button
            key={c}
            onClick={() => setColor(c)}
            aria-label={`Color ${c}`}
            className={`h-6 w-6 rounded-full transition ${color === c ? "ring-2 ring-accent ring-offset-2 dark:ring-offset-zinc-900" : ""}`}
            style={{ backgroundColor: c }}
          />
        ))}
      </div>

      <button
        onClick={handleCreate}
        disabled={!valid}
        className="flex w-full items-center justify-center gap-1.5 rounded-xl bg-accent py-2.5 text-sm font-medium text-white disabled:opacity-40"
      >
        <Check size={16} />
        Create subject &amp; chapter
      </button>
    </div>
  )
}