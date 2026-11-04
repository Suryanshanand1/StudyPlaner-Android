package com.studyplanner.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.studyplanner.app.MainActivity
import com.studyplanner.app.R
import com.studyplanner.app.data.StudyPlanEntity
import com.studyplanner.app.util.Dates

object NotificationScheduler {
    const val CHANNEL_REMINDERS = "study_reminders"

    private const val LEGACY_CHANNEL_TIMER = "focus_timer"
    private const val PREFS = "scheduled_plan_alarms"
    private const val KEY_IDS = "plan_ids"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders when a scheduled study session starts"
            enableVibration(true)
        }

        manager.createNotificationChannel(reminders)
        // The countdown timer is gone, so remove its channel from older installs.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_TIMER)
    }

    fun syncPlans(context: Context, plans: List<StudyPlanEntity>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previouslyScheduled = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()

        val desired = plans.filter { plan ->
            !plan.confirmed && (Dates.toMillis(plan.date, plan.startTime) ?: 0L) > System.currentTimeMillis()
        }
        val desiredIds = desired.map { it.id }.toSet()

        (previouslyScheduled - desiredIds).forEach { cancelPlanAlarm(context, it) }
        desired.forEach { schedulePlanAlarm(context, it) }

        prefs.edit { putStringSet(KEY_IDS, desiredIds.toMutableSet()) }
    }

    fun schedulePlanAlarm(context: Context, plan: StudyPlanEntity) {
        val triggerAt = Dates.toMillis(plan.date, plan.startTime) ?: return
        if (triggerAt <= System.currentTimeMillis()) return

        val intent = Intent(context, PlanAlarmReceiver::class.java).apply {
            putExtra("planId", plan.id)
            putExtra("title", "Study Time!")
            putExtra("body", buildString {
                if (plan.subjectName.isNotBlank()) append(plan.subjectName)
                if (plan.subjectName.isNotBlank() && plan.chapterName.isNotBlank()) append(": ")
                if (plan.chapterName.isNotBlank()) append(plan.chapterName)
                if (isBlank()) append("Your study session is starting")
            })
        }
        val pending = PendingIntent.getBroadcast(
            context,
            plan.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setAlarm(context, triggerAt, pending)
    }

    fun cancelPlanAlarm(context: Context, planId: String) {
        val intent = Intent(context, PlanAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            planId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
    }

    private fun setAlarm(context: Context, triggerAt: Long, pending: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (exactAllowed) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannels(context)

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and notify().
        }
    }
}
