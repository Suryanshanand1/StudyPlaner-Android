package com.studyplanner.app.focus

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

/** Launcher-visible apps the user can choose to block during a session. */
fun loadInstalledApps(context: Context): List<InstalledApp> {
    val manager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return manager.queryIntentActivities(intent, 0)
        .asSequence()
        .filter { it.activityInfo.packageName != context.packageName }
        .map { resolved ->
            InstalledApp(
                packageName = resolved.activityInfo.packageName,
                label = resolved.loadLabel(manager).toString(),
                icon = runCatching { resolved.loadIcon(manager) }.getOrNull(),
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
        .toList()
}
