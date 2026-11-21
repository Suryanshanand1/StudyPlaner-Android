package com.studyplanner.app.focus

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.studyplanner.app.R
import com.studyplanner.app.data.AppSettings
import com.studyplanner.app.data.FocusSessionManager
import com.studyplanner.app.data.SettingsRepository
import com.studyplanner.app.data.YouTubeMode

/**
 * Watches the foreground app while a focus session is active. Chosen apps are covered by
 * [BlockActivity]. YouTube gets a best-effort "study mode": Shorts are blocked and, when
 * an allowlist is configured, videos from other channels are blocked while the rest of
 * the app (home, search, subscriptions) stays usable.
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
            packageName == YOUTUBE_PACKAGE && settings.youtubeMode == YouTubeMode.BLOCK_ALL ->
                block(YOUTUBE_PACKAGE, getString(R.string.block_youtube_label))

            // An explicit app-block entry wins over the YouTube study mode.
            packageName in settings.blockedPackages -> block(packageName, appLabel(packageName))

            packageName == YOUTUBE_PACKAGE && shouldBlockYoutube(settings) ->
                block(YOUTUBE_PACKAGE, getString(R.string.block_youtube_label))
        }
    }

    /**
     * Study mode with no channels behaves like the simple block. Otherwise only Shorts and
     * videos from channels outside the allowlist are blocked: the app's own navigation
     * always contains the word "Shorts", so looking for it anywhere would block every
     * screen and make this mode identical to blocking YouTube entirely.
     */
    private fun shouldBlockYoutube(settings: AppSettings): Boolean {
        if (settings.youtubeChannels.isEmpty()) return true
        val root = rootInActiveWindow
        if (isShortsScreen(root)) return true
        if (!isVideoPlayerScreen(root)) return false
        val labels = collectTexts(root)
        return settings.youtubeChannels.none { channel ->
            labels.any { it.contains(channel, ignoreCase = true) }
        }
    }

    /** True when the Shorts tab is actually selected, not just present in the nav bar. */
    private fun isShortsScreen(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 400) {
            val node = queue.removeFirst()
            visited++
            val label = node.text?.toString() ?: node.contentDescription?.toString()
            if (label != null) {
                if (label.trim().equals("Shorts", ignoreCase = true) && isTabSelected(node)) {
                    return true
                }
                // Shorts-only action, used when the tab selection is not exposed.
                if (label.contains("remix this video", ignoreCase = true)) {
                    return true
                }
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
        }
        return false
    }

    /** Checks the node and a few ancestors, since the label sits inside the selected tab. */
    private fun isTabSelected(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 4) {
            if (current.isSelected) return true
            current = current.parent
            depth++
        }
        return false
    }

    /** Watch pages expose like/dislike buttons; feeds and browse screens do not. */
    private fun isVideoPlayerScreen(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 600) {
            val node = queue.removeFirst()
            visited++
            val label = node.contentDescription?.toString() ?: node.text?.toString()
            if (label != null &&
                (label.contains("like this video", ignoreCase = true) ||
                    label.contains("dislike this video", ignoreCase = true))
            ) {
                return true
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
        }
        return false
    }

    private fun collectTexts(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val result = ArrayList<String>(32)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 400 && result.size < 160) {
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
