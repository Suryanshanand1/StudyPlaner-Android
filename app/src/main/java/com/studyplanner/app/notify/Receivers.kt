package com.studyplanner.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyplanner.app.data.StudyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val BOOT_ACTIONS = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
)

class PlanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Study Time!"
        val body = intent.getStringExtra("body") ?: "Your study session is starting"
        val planId = intent.getStringExtra("planId") ?: body
        NotificationScheduler.showNotification(
            context = context,
            channelId = NotificationScheduler.CHANNEL_REMINDERS,
            notificationId = planId.hashCode(),
            title = title,
            body = body,
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in BOOT_ACTIONS) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val plans = StudyDatabase.get(appContext).dao().getPlans()
                NotificationScheduler.syncPlans(appContext, plans)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
