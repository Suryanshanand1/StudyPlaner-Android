package com.studyplanner.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.studyplanner.app.MainActivity
import com.studyplanner.app.R
import com.studyplanner.app.data.ActiveSession
import com.studyplanner.app.data.FocusSessionManager
import com.studyplanner.app.notify.FocusActionReceiver
import com.studyplanner.app.ui.EXTRA_OPEN_TAB
import com.studyplanner.app.ui.TAB_FOCUS
import com.studyplanner.app.util.Dates

/**
 * Home-screen stopwatch widget. The running timer uses a Chronometer view, so it keeps
 * ticking without periodic app updates.
 */
class FocusWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        FocusSessionManager.initialize(context)
        val views = buildViews(context)
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
    }

    companion object {
        fun updateAll(context: Context, session: ActiveSession?) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, FocusWidgetProvider::class.java),
            )
            if (ids.isEmpty()) return
            val views = buildViews(context, session)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(
            context: Context,
            session: ActiveSession? = FocusSessionManager.current,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_focus)
            views.setOnClickPendingIntent(R.id.widget_root, openApp(context))

            val elapsed = session?.elapsed(System.currentTimeMillis()) ?: 0L
            when {
                session == null -> {
                    views.setTextViewText(
                        R.id.widget_status,
                        context.getString(R.string.widget_idle),
                    )
                    views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                    views.setViewVisibility(R.id.widget_time, View.VISIBLE)
                    views.setTextViewText(R.id.widget_time, "00:00:00")
                    views.setTextViewText(
                        R.id.widget_action_primary,
                        context.getString(R.string.widget_start),
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_action_primary,
                        action(context, FocusActionReceiver.ACTION_START, 1),
                    )
                    views.setViewVisibility(R.id.widget_action_stop, View.GONE)
                }
                session.isRunning -> {
                    views.setTextViewText(
                        R.id.widget_status,
                        context.getString(R.string.widget_studying),
                    )
                    views.setViewVisibility(R.id.widget_time, View.GONE)
                    views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                    views.setChronometer(
                        R.id.widget_chronometer,
                        SystemClock.elapsedRealtime() - elapsed,
                        null,
                        true,
                    )
                    views.setTextViewText(
                        R.id.widget_action_primary,
                        context.getString(R.string.widget_pause),
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_action_primary,
                        action(context, FocusActionReceiver.ACTION_PAUSE, 1),
                    )
                    views.setViewVisibility(R.id.widget_action_stop, View.VISIBLE)
                    views.setOnClickPendingIntent(
                        R.id.widget_action_stop,
                        action(context, FocusActionReceiver.ACTION_STOP, 2),
                    )
                }
                else -> {
                    views.setTextViewText(
                        R.id.widget_status,
                        context.getString(R.string.widget_paused),
                    )
                    views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                    views.setViewVisibility(R.id.widget_time, View.VISIBLE)
                    views.setTextViewText(R.id.widget_time, Dates.clock(elapsed))
                    views.setTextViewText(
                        R.id.widget_action_primary,
                        context.getString(R.string.widget_resume),
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_action_primary,
                        action(context, FocusActionReceiver.ACTION_RESUME, 1),
                    )
                    views.setViewVisibility(R.id.widget_action_stop, View.VISIBLE)
                    views.setOnClickPendingIntent(
                        R.id.widget_action_stop,
                        action(context, FocusActionReceiver.ACTION_STOP, 2),
                    )
                }
            }
            return views
        }

        private fun action(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, FocusActionReceiver::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
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
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
