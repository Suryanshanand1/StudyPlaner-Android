export const APP_VERSION: string = process.env.NEXT_PUBLIC_APP_VERSION || "0.0.0"

export const UPDATE_CHECK_URL = "https://api.github.com/repos/Suryanshanand1/StudyPlaner-Android/releases/latest"

export const APK_DOWNLOAD_URL = "https://github.com/Suryanshanand1/StudyPlaner-Android/releases/latest/download/StudyPlanner.apk"

export interface LatestRelease {
  version: string
  apkUrl: string
  notes: string
}

export function compareVersions(a: string, b: string): number {
  const pa = a.replace(/^v/, "").split(".").map((n) => parseInt(n, 10) || 0)
  const pb = b.replace(/^v/, "").split(".").map((n) => parseInt(n, 10) || 0)
  const len = Math.max(pa.length, pb.length)
  for (let i = 0; i < len; i++) {
    const na = pa[i] ?? 0
    const nb = pb[i] ?? 0
    if (na > nb) return 1
    if (na < nb) return -1
  }
  return 0
}

export function isNewer(latest: string, current: string): boolean {
  return compareVersions(latest, current) > 0
}

export async function getLatestRelease(): Promise<LatestRelease | null> {
  try {
    const res = await fetch(UPDATE_CHECK_URL, { headers: { Accept: "application/vnd.github+json" } })
    if (!res.ok) return null
    const data = await res.json()
    const assets = Array.isArray(data.assets) ? data.assets : []
    const apkAsset = assets.find((a: { name?: string; browser_download_url?: string }) => a.name?.endsWith(".apk"))
    return {
      version: typeof data.tag_name === "string" ? data.tag_name.replace(/^v/, "") : "",
      apkUrl: apkAsset?.browser_download_url ?? APK_DOWNLOAD_URL,
      notes: typeof data.body === "string" ? data.body : "",
    }
  } catch {
    return null
  }
}
