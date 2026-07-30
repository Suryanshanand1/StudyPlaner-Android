export function generateId(): string {
  return crypto.randomUUID()
}

export function getToday(): string {
  return new Date().toISOString().split("T")[0]
}

export function getWeekDays(date: Date = new Date()): string[] {
  const start = new Date(date)
  start.setDate(start.getDate() - start.getDay())
  const days: string[] = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(start)
    d.setDate(d.getDate() + i)
    days.push(d.toISOString().split("T")[0])
  }
  return days
}

export function getMonthDays(year: number, month: number): string[] {
  const days: string[] = []
  const totalDays = new Date(year, month + 1, 0).getDate()
  for (let d = 1; d <= totalDays; d++) {
    const m = String(month + 1).padStart(2, "0")
    const day = String(d).padStart(2, "0")
    days.push(`${year}-${m}-${day}`)
  }
  return days
}

export function formatDate(dateStr: string): string {
  const d = new Date(dateStr + "T00:00:00")
  return d.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" })
}

export function formatTime(time: string): string {
  const [h, m] = time.split(":")
  const hour = parseInt(h)
  const ampm = hour >= 12 ? "PM" : "AM"
  const h12 = hour % 12 || 12
  return `${h12}:${m} ${ampm}`
}

export function getMonthName(month: number): string {
  return new Date(2000, month).toLocaleString("en-US", { month: "long" })
}

export const SUBJECT_COLORS = [
  "#3b82f6",
  "#ef4444",
  "#10b981",
  "#f59e0b",
  "#8b5cf6",
  "#ec4899",
  "#06b6d4",
  "#f97316",
  "#14b8a6",
  "#6366f1",
]
