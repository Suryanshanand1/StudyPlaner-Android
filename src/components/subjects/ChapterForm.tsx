"use client"

import { useState } from "react"
import { X } from "lucide-react"

interface ChapterFormProps {
  onSave: (name: string) => void
  onCancel: () => void
}

export default function ChapterForm({ onSave, onCancel }: ChapterFormProps) {
  const [name, setName] = useState("")

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 dark:bg-black/60">
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl dark:bg-zinc-900">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold dark:text-zinc-100">New Chapter</h2>
          <button onClick={onCancel} className="rounded-full p-1 hover:bg-zinc-100 dark:hover:bg-zinc-800">
            <X size={20} className="text-zinc-500 dark:text-zinc-400" />
          </button>
        </div>
        <input
          autoFocus
          placeholder="Chapter name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="mb-4 w-full rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none focus:border-accent dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
        />
        <button
          onClick={() => {
            if (name.trim()) onSave(name.trim())
          }}
          disabled={!name.trim()}
          className="w-full rounded-xl bg-accent py-3 text-sm font-medium text-white disabled:opacity-40"
        >
          Add Chapter
        </button>
      </div>
    </div>
  )
}
