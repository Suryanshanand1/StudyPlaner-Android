package com.studyplanner.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val apkUrl: String,
    val notes: String,
)

/**
 * Reads the same GitHub release feed that the original web app used.
 * Network failures are silent; the app is fully usable offline.
 */
object UpdateChecker {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/Suryanshanand1/StudyPlaner-Android/releases/latest"
    private const val FALLBACK_APK_URL =
        "https://github.com/Suryanshanand1/StudyPlaner-Android/releases/latest/download/StudyPlanner.apk"

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "StudyPlanner-Android")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val version = json.optString("tag_name").removePrefix("v")
                if (version.isBlank()) return@runCatching null

                var apkUrl = FALLBACK_APK_URL
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", FALLBACK_APK_URL)
                            break
                        }
                    }
                }

                UpdateInfo(
                    version = version,
                    apkUrl = apkUrl,
                    notes = json.optString("body").trim(),
                )
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    fun isNewer(latest: String, current: String): Boolean =
        compareVersions(latest, current) > 0

    private fun compareVersions(a: String, b: String): Int {
        val left = a.removePrefix("v").split(".")
        val right = b.removePrefix("v").split(".")
        val length = maxOf(left.size, right.size)
        for (i in 0 until length) {
            val l = left.getOrNull(i)?.toIntOrNull() ?: 0
            val r = right.getOrNull(i)?.toIntOrNull() ?: 0
            if (l > r) return 1
            if (l < r) return -1
        }
        return 0
    }
}
