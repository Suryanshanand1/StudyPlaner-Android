"use client"

import { useState, useRef } from "react"
import { Sun, Moon, Monitor, Palette, Download, Upload, CheckCircle } from "lucide-react"
import { useTheme, ACCENT_PRESETS } from "@/hooks/theme"
import { useStore } from "@/lib/store"

const THEME_OPTIONS = [
  { value: "light" as const, label: "Light", icon: Sun },
  { value: "dark" as const, label: "Dark", icon: Moon },
  { value: "system" as const, label: "System", icon: Monitor },
]

export default function Settings() {
  const { theme, setTheme, accentColor, setAccentColor } = useTheme()
  const { exportData, importData } = useStore()
  const [showCustom, setShowCustom] = useState(false)
  const [importStatus, setImportStatus] = useState<"idle" | "success" | "error">("idle")
  const fileRef = useRef<HTMLInputElement>(null)

  const handleExport = () => {
    const json = exportData()
    const blob = new Blob([json], { type: "application/json" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `study-planner-backup.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  const handleImport = () => {
    fileRef.current?.click()
  }

  const onFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (ev) => {
      const text = ev.target?.result as string
      const ok = importData(text)
      setImportStatus(ok ? "success" : "error")
      setTimeout(() => setImportStatus("idle"), 2000)
      if (ok) setTimeout(() => window.location.reload(), 800)
    }
    reader.readAsText(file)
    e.target.value = ""
  }

  return (
    <div className="space-y-6">
      <div>
        <h3 className="mb-1 text-xs font-semibold uppercase tracking-wide text-zinc-500 dark:text-zinc-400">
          Appearance
        </h3>
        <p className="mb-3 text-xs text-zinc-400 dark:text-zinc-500">Choose your theme preference</p>
        <div className="flex gap-2">
          {THEME_OPTIONS.map(({ value, label, icon: Icon }) => (
            <button
              key={value}
              onClick={() => setTheme(value)}
              className={`flex flex-1 flex-col items-center gap-2 rounded-xl border-2 p-4 text-sm font-medium transition ${
                theme === value
                  ? "border-accent bg-accent text-white"
                  : "border-zinc-200 bg-white text-zinc-600 hover:border-zinc-300 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-400 dark:hover:border-zinc-600"
              }`}
            >
              <Icon size={24} />
              {label}
            </button>
          ))}
        </div>
      </div>

      <div>
        <div className="mb-3 flex items-center gap-2">
          <Palette size={16} className="text-zinc-400" />
          <h3 className="text-xs font-semibold uppercase tracking-wide text-zinc-500 dark:text-zinc-400">
            Accent Color
          </h3>
        </div>
        <div className="mb-3 flex flex-wrap gap-2">
          {ACCENT_PRESETS.map(({ name, value }) => (
            <button
              key={value}
              title={name}
              onClick={() => { setAccentColor(value); setShowCustom(false) }}
              className={`h-8 w-8 rounded-full transition ${accentColor === value && !showCustom ? "ring-2 ring-zinc-800 ring-offset-2 dark:ring-white dark:ring-offset-zinc-900" : "ring-1 ring-inset ring-black/10"}`}
              style={{ backgroundColor: value }}
            />
          ))}
          <button
            onClick={() => setShowCustom(!showCustom)}
            className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-medium transition ${
              showCustom
                ? "ring-2 ring-zinc-800 ring-offset-2 dark:ring-white dark:ring-offset-zinc-900"
                : "ring-1 ring-inset ring-black/10"
            } border border-dashed border-zinc-300 bg-white text-zinc-500 dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-400`}
          >
            +
          </button>
        </div>
        {showCustom && (
          <div className="flex items-center gap-3 rounded-xl border border-zinc-200 bg-white p-3 dark:border-zinc-700 dark:bg-zinc-900">
            <input
              type="color"
              value={accentColor}
              onChange={(e) => setAccentColor(e.target.value)}
              className="h-10 w-10 cursor-pointer rounded-lg border-0 bg-transparent p-0"
            />
            <input
              type="text"
              value={accentColor}
              onChange={(e) => {
                const v = e.target.value
                if (/^#[0-9a-fA-F]{6}$/.test(v)) setAccentColor(v)
              }}
              className="flex-1 rounded-lg border border-zinc-200 px-3 py-2 text-xs font-mono outline-none focus:border-zinc-400 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"
              placeholder="#3b82f6"
            />
          </div>
        )}
        <div className="mt-2 flex items-center gap-2 rounded-xl bg-zinc-100 p-2 dark:bg-zinc-800">
          <span className="h-5 w-5 rounded" style={{ backgroundColor: accentColor }} />
          <span className="rounded-md bg-accent px-3 py-1 text-xs font-medium text-white">
            Preview Button
          </span>
          <span className="text-xs text-accent font-medium">Preview Link</span>
        </div>
      </div>

      <div>
        <div className="mb-3 flex items-center gap-2">
          <Download size={16} className="text-zinc-400" />
          <h3 className="text-xs font-semibold uppercase tracking-wide text-zinc-500 dark:text-zinc-400">
            Data
          </h3>
        </div>
        <p className="mb-3 text-xs text-zinc-400 dark:text-zinc-500">Backup or restore your data</p>
        <div className="flex gap-2">
          <button
            onClick={handleExport}
            className="flex flex-1 items-center justify-center gap-2 rounded-xl border-2 border-zinc-200 bg-white px-4 py-3 text-sm font-medium text-zinc-700 hover:border-zinc-300 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-300 dark:hover:border-zinc-600"
          >
            <Download size={18} />
            Export
          </button>
          <button
            onClick={handleImport}
            className={`flex flex-1 items-center justify-center gap-2 rounded-xl border-2 px-4 py-3 text-sm font-medium transition ${
              importStatus === "success"
                ? "border-green-500 bg-green-50 text-green-700 dark:bg-green-950"
                : importStatus === "error"
                ? "border-red-500 bg-red-50 text-red-700 dark:bg-red-950"
                : "border-zinc-200 bg-white text-zinc-700 hover:border-zinc-300 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-300 dark:hover:border-zinc-600"
            }`}
          >
            {importStatus === "success" ? <CheckCircle size={18} /> : <Upload size={18} />}
            {importStatus === "success" ? "Imported!" : importStatus === "error" ? "Invalid file" : "Import"}
          </button>
        </div>
        <input ref={fileRef} type="file" accept=".json" onChange={onFileChange} className="hidden" />
      </div>
    </div>
  )
}
