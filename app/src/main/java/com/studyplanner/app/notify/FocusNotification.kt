package com.studyplanner.app.notify

import android.Manifest
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
import com.studyplanner.app.MainActivity
import com.studyplanner.app.R
import com.studyplanner.app.data.ActiveSession
import com.studyplanner.app.ui.EXTRA_OPEN_TAB
import com.studyplanner.app.ui.TAB_FOCUS
import com.studyplanner.app.util.Dates

/**
 * Ongoing notification for the live stopwatch. While running it uses the system
 * chronometer so the elapsed time keeps ticking without waking the app.
 */
object FocusNotification {
    private const val CHANNEL_FOCUS = "focus_session"
    private const val NOTIFICATION_ID = 5202

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_FOCUS,
            context.getString(R.string.focus_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.focus_channel_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun update(context: Context, session: ActiveSession?) {
        createChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (session == null) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val now = System.currentTimeMillis()
        val elapsed = session.elapsed(now)
        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(session.label)
            .setContentIntent(openApp(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (session.isRunning) {
            builder
                .setContentText(context.getString(R.string.focus_status_studying))
                .setUsesChronometer(true)
                .setWhen(now - elapsed)
                .addAction(
                    R.drawable.ic_focus_pause,
                    context.getString(R.string.focus_notification_pause),
                    action(context, FocusActionReceiver.ACTION_PAUSE),
                )
        } else {
            builder
                .setContentText(
                    "${context.getString(R.string.focus_status_paused)} · ${Dates.clock(elapsed)}",
                )
                .setUsesChronometer(false)
                .addAction(
                    R.drawable.ic_focus_play,
                    context.getString(R.string.focus_notification_resume),
                    action(context, FocusActionReceiver.ACTION_RESUME),
                )
        }
        builder.addAction(
            R.drawable.ic_focus_stop,
            context.getString(R.string.focus_notification_stop),
            action(context, FocusActionReceiver.ACTION_STOP),
        )

        try {
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission was revoked between the check and notify().
        }
    }

    private fun action(context: Context, action: String): PendingIntent {
        val intent = Intent(context, FocusActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, TAB_FOCUS)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
