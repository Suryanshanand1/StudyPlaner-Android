"use client"

import { useMemo, useState } from "react"
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend } from "recharts"
import { ChevronLeft, ChevronRight, TrendingUp, BookOpen, Clock } from "lucide-react"
import { useStore } from "@/lib/store"
import { getMonthName } from "@/lib/utils"
import { useTheme } from "@/hooks/theme"

export default function MonthlyChart() {
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth())
  const { getMonthlySessions } = useStore()
  const { resolvedTheme } = useTheme()
  const isDark = resolvedTheme === "dark"

  const sessions = useMemo(() => getMonthlySessions(year, month), [getMonthlySessions, year, month])

  const totalHours = useMemo(() => sessions.reduce((a, s) => a + s.hours, 0), [sessions])
  const totalCompleted = useMemo(() => sessions.reduce((a, s) => a + s.chaptersCompleted, 0), [sessions])
  const studyDays = useMemo(() => sessions.filter((s) => s.hours > 0).length, [sessions])

  const chartData = useMemo(
    () =>
      sessions.map((s) => ({
        day: new Date(s.date + "T00:00:00").getDate().toString(),
        hours: s.hours,
        chapters: s.chaptersCompleted,
      })),
    [sessions],
  )

  const navigate = (dir: number) => {
    const d = new Date(year, month + dir, 1)
    setYear(d.getFullYear())
    setMonth(d.getMonth())
  }

  const tickColor = isDark ? "#a1a1aa" : "#71717a"
  const gridColor = isDark ? "#27272a" : "#f0f0f0"
  const tooltipBg = isDark ? "#18181b" : "#ffffff"
  const tooltipBorder = isDark ? "#3f3f46" : "#e4e4e7"

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <button onClick={() => navigate(-1)} className="rounded-lg p-1.5 hover:bg-zinc-100 dark:hover:bg-zinc-800">
          <ChevronLeft size={18} className="text-zinc-600 dark:text-zinc-400" />
        </button>
        <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-200">
          {getMonthName(month)} {year}
        </h3>
        <button onClick={() => navigate(1)} className="rounded-lg p-1.5 hover:bg-zinc-100 dark:hover:bg-zinc-800">
          <ChevronRight size={18} className="text-zinc-600 dark:text-zinc-400" />
        </button>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <div className="rounded-xl bg-blue-50 p-3 text-center dark:bg-blue-950/40">
          <Clock size={18} className="mx-auto mb-1 text-blue-500" />
          <p className="text-lg font-bold text-blue-700 dark:text-blue-300">{totalHours.toFixed(1)}</p>
          <p className="text-xs text-blue-500 dark:text-blue-400">Hours</p>
        </div>
        <div className="rounded-xl bg-green-50 p-3 text-center dark:bg-green-950/40">
          <BookOpen size={18} className="mx-auto mb-1 text-green-500" />
          <p className="text-lg font-bold text-green-700 dark:text-green-300">{totalCompleted}</p>
          <p className="text-xs text-green-500 dark:text-green-400">Chapters</p>
        </div>
        <div className="rounded-xl bg-purple-50 p-3 text-center dark:bg-purple-950/40">
          <TrendingUp size={18} className="mx-auto mb-1 text-purple-500" />
          <p className="text-lg font-bold text-purple-700 dark:text-purple-300">{studyDays}</p>
          <p className="text-xs text-purple-500 dark:text-purple-400">Days</p>
        </div>
      </div>

      <div className="rounded-xl border border-zinc-100 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
        <h4 className="mb-3 text-xs font-semibold text-zinc-500 dark:text-zinc-400 uppercase tracking-wide">Daily Study Hours</h4>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={chartData} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
            <XAxis dataKey="day" tick={{ fontSize: 11, fill: tickColor }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 11, fill: tickColor }} axisLine={false} tickLine={false} />
            <Tooltip
              contentStyle={{ borderRadius: 12, border: `1px solid ${tooltipBorder}`, fontSize: 12, backgroundColor: tooltipBg, color: isDark ? "#e4e4e7" : "#18181b" }}
              formatter={(value) => [`${value}`, ""]}
            />
            <Legend wrapperStyle={{ fontSize: 11, color: tickColor }} />
            <Bar dataKey="hours" name="Hours" fill="#3b82f6" radius={[4, 4, 0, 0]} maxBarSize={20} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="rounded-xl border border-zinc-100 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
        <h4 className="mb-3 text-xs font-semibold text-zinc-500 dark:text-zinc-400 uppercase tracking-wide">Chapters Completed</h4>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={chartData} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
            <XAxis dataKey="day" tick={{ fontSize: 11, fill: tickColor }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 11, fill: tickColor }} axisLine={false} tickLine={false} />
            <Tooltip
              contentStyle={{ borderRadius: 12, border: `1px solid ${tooltipBorder}`, fontSize: 12, backgroundColor: tooltipBg, color: isDark ? "#e4e4e7" : "#18181b" }}
              formatter={(value) => [`${value}`, ""]}
            />
            <Legend wrapperStyle={{ fontSize: 11, color: tickColor }} />
            <Bar dataKey="chapters" name="Chapters" fill="#10b981" radius={[4, 4, 0, 0]} maxBarSize={20} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
