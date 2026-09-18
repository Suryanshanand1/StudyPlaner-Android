"use client"

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react"

export type ThemeMode = "light" | "dark" | "system"

export const ACCENT_PRESETS = [
  { name: "Blue", value: "#3b82f6" },
  { name: "Green", value: "#10b981" },
  { name: "Purple", value: "#8b5cf6" },
  { name: "Orange", value: "#f97316" },
  { name: "Rose", value: "#f43f5e" },
  { name: "Cyan", value: "#06b6d4" },
  { name: "Amber", value: "#f59e0b" },
  { name: "Teal", value: "#14b8a6" },
]

interface ThemeContext {
  theme: ThemeMode
  resolvedTheme: "light" | "dark"
  accentColor: string
  setTheme: (t: ThemeMode) => void
  setAccentColor: (c: string) => void
}

const ThemeCtx = createContext<ThemeContext | null>(null)

const THEME_KEY = "study-planner-theme"
const ACCENT_KEY = "study-planner-accent"

function getStored<T>(key: string, fallback: T): T {
  if (typeof window === "undefined") return fallback
  try {
    const v = localStorage.getItem(key)
    if (v !== null) return JSON.parse(v) as T
  } catch {}
  return fallback
}

function resolve(t: ThemeMode): "light" | "dark" {
  if (t === "light") return "light"
  if (t === "dark") return "dark"
  if (typeof window === "undefined") return "light"
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light"
}

function applyTheme(resolved: "light" | "dark") {
  document.documentElement.classList.toggle("dark", resolved === "dark")
}

function applyAccent(color: string) {
  document.documentElement.style.setProperty("--accent", color)
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeMode>(() => getStored<ThemeMode>(THEME_KEY, "system"))
  const [resolvedTheme, setResolved] = useState<"light" | "dark">(() =>
    resolve(getStored<ThemeMode>(THEME_KEY, "system")),
  )
  const [accentColor, setAccentState] = useState<string>(() =>
    getStored<string>(ACCENT_KEY, ACCENT_PRESETS[0].value),
  )

  useEffect(() => {
    applyTheme(resolvedTheme)
  }, [resolvedTheme])

  useEffect(() => {
    applyAccent(accentColor)
  }, [accentColor])

  useEffect(() => {
    try { localStorage.setItem(THEME_KEY, JSON.stringify(theme)) } catch {}
  }, [theme])

  useEffect(() => {
    try { localStorage.setItem(ACCENT_KEY, JSON.stringify(accentColor)) } catch {}
  }, [accentColor])

  useEffect(() => {
    if (theme !== "system") return
    const mq = window.matchMedia("(prefers-color-scheme: dark)")
    const handler = () => setResolved(mq.matches ? "dark" : "light")
    mq.addEventListener("change", handler)
    return () => mq.removeEventListener("change", handler)
  }, [theme])

  const setTheme = useCallback((t: ThemeMode) => {
    setThemeState(t)
    setResolved(resolve(t))
  }, [])

  const setAccentColor = useCallback((c: string) => {
    setAccentState(c)
  }, [])

  return (
    <ThemeCtx.Provider value={{ theme, resolvedTheme, accentColor, setTheme, setAccentColor }}>
      {children}
    </ThemeCtx.Provider>
  )
}

export function useTheme() {
  const ctx = useContext(ThemeCtx)
  if (!ctx) throw new Error("useTheme must be used within ThemeProvider")
  return ctx
}
