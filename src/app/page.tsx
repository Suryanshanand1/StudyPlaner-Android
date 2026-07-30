"use client"

import { useMemo, useState } from "react"
import { House, BookOpen, Calendar, BarChart3, Plus, Settings as Gear, ArrowLeft } from "lucide-react"
import { useStore } from "@/lib/store"
import Dashboard from "@/components/Dashboard"
import SubjectCard from "@/components/SubjectCard"
import SubjectForm from "@/components/SubjectForm"
import StudyPlanner from "@/components/StudyPlanner"
import MonthlyChart from "@/components/MonthlyChart"
import Settings from "@/components/Settings"

type Tab = "dashboard" | "subjects" | "planner" | "chart"

export default function App() {
  const { subjects, chapters, addSubject, editSubject } = useStore()
  const [tab, setTab] = useState<Tab>("dashboard")
  const [showSubjectForm, setShowSubjectForm] = useState(false)
  const [editingSubject, setEditingSubject] = useState<{ id: string; name: string; color: string } | null>(null)
  const [showSettings, setShowSettings] = useState(false)

  const sortedChapters = useMemo(
    () => chapters.filter((ch) => ch.subjectId === "").sort((a, b) => a.order - b.order),
    [],
  )

  return (
    <div className="mx-auto flex h-full max-w-lg flex-col bg-zinc-50 dark:bg-zinc-950">
      <header className="flex items-center justify-between border-b border-zinc-200 bg-white px-5 py-4 dark:border-zinc-800 dark:bg-zinc-900">
        {showSettings ? (
          <button onClick={() => setShowSettings(false)} className="flex items-center gap-2 text-sm font-medium text-zinc-700 dark:text-zinc-300">
            <ArrowLeft size={18} />
            Settings
          </button>
        ) : (
          <h1 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">
            {tab === "dashboard" ? "Dashboard" : tab === "subjects" ? "Subjects" : tab === "planner" ? "Planner" : "Analytics"}
          </h1>
        )}
        {!showSettings && (
          <div className="flex items-center gap-2">
            {tab === "subjects" && (
              <button
                onClick={() => setShowSubjectForm(true)}
                className="flex items-center gap-1.5 rounded-xl bg-accent px-4 py-2 text-xs font-medium text-white"
              >
                <Plus size={16} />
                Subject
              </button>
            )}
            <button
              onClick={() => setShowSettings(true)}
              className="rounded-xl p-2 text-zinc-500 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
            >
              <Gear size={20} />
            </button>
          </div>
        )}
      </header>

      <main className="flex-1 overflow-y-auto px-4 py-4">
        {showSettings ? (
          <Settings />
        ) : tab === "dashboard" ? (
          <Dashboard />
        ) : tab === "subjects" ? (
          <div className="space-y-3">
            {subjects.length === 0 && (
              <div className="flex flex-col items-center justify-center py-16 text-center">
                <BookOpen size={48} className="mb-3 text-zinc-200 dark:text-zinc-700" />
                <p className="text-sm font-medium text-zinc-400 dark:text-zinc-500">No subjects yet</p>
                <p className="mt-1 text-xs text-zinc-300 dark:text-zinc-600">Add your first subject to get started</p>
              </div>
            )}
            {subjects.map((subject) => (
              <SubjectCard
                key={subject.id}
                subject={subject}
                chapters={chapters.filter((ch) => ch.subjectId === subject.id)}
                onEdit={() => setEditingSubject({ id: subject.id, name: subject.name, color: subject.color })}
              />
            ))}
          </div>
        ) : tab === "planner" ? (
          <StudyPlanner />
        ) : (
          <MonthlyChart />
        )}
      </main>

      {!showSettings && (
        <nav className="flex border-t border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
          {([
            { key: "dashboard", label: "Home", icon: House },
            { key: "subjects", label: "Subjects", icon: BookOpen },
            { key: "planner", label: "Planner", icon: Calendar },
            { key: "chart", label: "Analytics", icon: BarChart3 },
          ] as const).map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              className={`flex flex-1 flex-col items-center gap-0.5 py-3 text-xs font-medium transition ${
                tab === key ? "text-accent" : "text-zinc-400 dark:text-zinc-500"
              }`}
            >
              <Icon size={20} />
              {label}
            </button>
          ))}
        </nav>
      )}

      {showSubjectForm && (
        <SubjectForm
          onSave={(name, color) => {
            addSubject(name, color)
            setShowSubjectForm(false)
          }}
          onCancel={() => setShowSubjectForm(false)}
        />
      )}

      {editingSubject && (
        <SubjectForm
          initial={editingSubject}
          onSave={(name, color) => {
            editSubject(editingSubject.id, name, color)
            setEditingSubject(null)
          }}
          onCancel={() => setEditingSubject(null)}
        />
      )}
    </div>
  )
}
