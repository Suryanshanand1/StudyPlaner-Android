"use client"

import { useState } from "react"
import { X } from "lucide-react"
import { SUBJECT_COLORS } from "@/lib/utils"

interface SubjectFormProps {
  initial?: { id: string; name: string; color: string }
  onSave: (name: string, color: string) => void
  onCancel: () => void
}

export default function SubjectForm({ initial, onSave, onCancel }: SubjectFormProps) {
  const [name, setName] = useState(initial?.name ?? "")
  const [color, setColor] = useState(initial?.color ?? SUBJECT_COLORS[0])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 dark:bg-black/60">
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl dark:bg-zinc-900">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold dark:text-zinc-100">{initial ? "Edit Subject" : "New Subject"}</h2>
          <button onClick={onCancel} className="rounded-full p-1 hover:bg-zinc-100 dark:hover:bg-zinc-800">
            <X size={20} className="text-zinc-500 dark:text-zinc-400" />
          </button>
        </div>
        <input
          autoFocus
          placeholder="Subject name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="mb-4 w-full rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none focus:border-accent dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
        />
        <div className="mb-4">
          <p className="mb-2 text-xs font-medium text-zinc-500 dark:text-zinc-400">Color</p>
          <div className="flex gap-2">
            {SUBJECT_COLORS.map((c) => (
              <button
                key={c}
                onClick={() => setColor(c)}
                className={`h-7 w-7 rounded-full ${color === c ? "ring-2 ring-accent ring-offset-2 dark:ring-offset-zinc-900" : ""}`}
                style={{ backgroundColor: c }}
              />
            ))}
          </div>
        </div>
        <button
          onClick={() => {
            if (name.trim()) onSave(name.trim(), color)
          }}
          disabled={!name.trim()}
          className="w-full rounded-xl bg-accent py-3 text-sm font-medium text-white disabled:opacity-40"
        >
          {initial ? "Save" : "Add Subject"}
        </button>
      </div>
    </div>
  )
}
