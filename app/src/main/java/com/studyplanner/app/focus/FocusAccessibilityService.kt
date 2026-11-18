package com.studyplanner.app.focus

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.studyplanner.app.R
import com.studyplanner.app.data.FocusSessionManager
import com.studyplanner.app.data.SettingsRepository
import com.studyplanner.app.data.YouTubeMode

/**
 * Watches the foreground app while a focus session is active. Chosen apps are covered by
 * [BlockActivity]. YouTube gets a best-effort "study mode": Shorts are blocked and, when
 * an allowlist is configured, only screens that mention an allowed channel are permitted.
 */
class FocusAccessibilityService : AccessibilityService() {
    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }
        if (FocusSessionManager.current == null) return

        val settings = SettingsRepository.get(this).settings.value
        if (!settings.deepFocusMode || !settings.blockingEnabled) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return
        if (packageName == "com.android.systemui") return

        when {
            packageName == YOUTUBE_PACKAGE && settings.youtubeMode == YouTubeMode.BLOCK_ALL -> {
                block(YOUTUBE_PACKAGE, getString(R.string.block_youtube_label))
            }

            packageName == YOUTUBE_PACKAGE -> {
                if (shouldBlockYoutube(settings.youtubeChannels)) {
                    block(YOUTUBE_PACKAGE, getString(R.string.block_youtube_label))
                }
            }

            packageName in settings.blockedPackages -> block(packageName, appLabel(packageName))
        }
    }

    private fun shouldBlockYoutube(channels: Set<String>): Boolean {
        val texts = collectTexts(rootInActiveWindow)
        if (texts.any { it.contains("shorts", ignoreCase = true) }) return true
        if (channels.isEmpty()) return true
        return channels.none { channel ->
            texts.any { it.contains(channel, ignoreCase = true) }
        }
    }

    private fun collectTexts(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val result = ArrayList<String>(32)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 250 && result.size < 120) {
            val node = queue.removeFirst()
            visited++
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let(result::add)
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(result::add)
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
        }
        return result
    }

    private fun appLabel(packageName: String): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    private fun block(packageName: String, label: String) {
        val now = SystemClock.elapsedRealtime()
        if (packageName == lastBlockedPackage && now - lastBlockedAt < 1500) return
        lastBlockedPackage = packageName
        lastBlockedAt = now

        val intent = Intent(this, BlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BlockActivity.EXTRA_PACKAGE, packageName)
            putExtra(BlockActivity.EXTRA_LABEL, label)
        }
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() = Unit

    private companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
