package com.studyplanner.app.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.studyplanner.app.MainActivity
import com.studyplanner.app.R
import com.studyplanner.app.data.SettingsRepository
import com.studyplanner.app.ui.EXTRA_OPEN_TAB
import com.studyplanner.app.ui.TAB_FOCUS
import java.io.File

/**
 * Foreground media playback for the optional focus sound. It plays either one of the sounds
 * bundled in res/raw or a user-imported file, which the UI copies into app storage so
 * playback never depends on the original document.
 */
class FocusSoundService : Service() {
    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startPlayback()
            ACTION_PAUSE -> pausePlayback()
            ACTION_RESUME -> resumePlayback()
            ACTION_VOLUME -> updateVolume(intent.getFloatExtra(EXTRA_VOLUME, -1f))
            ACTION_STOP -> stopPlayback()
            else -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        releasePlayer()
        super.onDestroy()
    }

    private fun startPlayback() {
        val settings = SettingsRepository.get(this).settings.value
        val builtin = builtinSoundById(settings.builtinSound)
        val filePath = settings.soundPath
        val hasSource = builtin != null || (!filePath.isNullOrBlank() && File(filePath).exists())
        if (!settings.deepFocusMode || !settings.soundEnabled || !hasSource) {
            stopPlayback()
            return
        }

        createChannel()
        try {
            startForeground(NOTIFICATION_ID, buildNotification(soundTitle(settings), playing = true))
        } catch (_: Exception) {
            stopSelf()
            return
        }

        releasePlayer()
        if (builtin != null) {
            val created = MediaPlayer.create(this, builtin.resId)
            if (created == null) {
                stopPlayback()
                return
            }
            created.isLooping = true
            created.setAudioAttributes(mediaAttributes())
            created.setVolume(settings.soundVolume, settings.soundVolume)
            created.start()
            player = created
        } else {
            player = MediaPlayer().apply {
                setAudioAttributes(mediaAttributes())
                setDataSource(filePath)
                isLooping = true
                setVolume(settings.soundVolume, settings.soundVolume)
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    true
                }
                prepareAsync()
            }
        }
    }

    private fun mediaAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    private fun pausePlayback() {
        val current = player ?: return
        if (current.isPlaying) current.pause()
        updateNotification(playing = false)
    }

    private fun resumePlayback() {
        val current = player
        if (current == null) {
            // Nothing prepared yet (for example, the service was started while paused),
            // so start a fresh playback instead of silently doing nothing.
            startPlayback()
            return
        }
        runCatching { current.start() }
        updateNotification(playing = true)
    }

    private fun updateVolume(volume: Float) {
        if (volume < 0f) return
        val safe = volume.coerceIn(0f, 1f)
        player?.setVolume(safe, safe)
    }

    private fun updateNotification(playing: Boolean) {
        val title = soundTitle(SettingsRepository.get(this).settings.value)
        runCatching {
            startForeground(NOTIFICATION_ID, buildNotification(title, playing))
        }
    }

    private fun soundTitle(settings: com.studyplanner.app.data.AppSettings): String {
        return settings.soundName
            ?: builtinSoundById(settings.builtinSound)?.label
            ?: getString(R.string.sound_default_name)
    }

    private fun stopPlayback() {
        releasePlayer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayer() {
        player?.runCatching {
            if (isPlaying) stop()
        }
        player?.release()
        player = null
    }

    private fun buildNotification(name: String?, playing: Boolean): Notification {
        val title = name ?: getString(R.string.sound_default_name)
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_TAB, TAB_FOCUS)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, FocusSoundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_SOUND)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.sound_notification_title))
            .setContentText(
                if (playing) {
                    title
                } else {
                    getString(R.string.sound_notification_paused, title)
                },
            )
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_focus_stop,
                getString(R.string.sound_notification_stop),
                stop,
            )
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_SOUND,
            getString(R.string.sound_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.sound_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_SOUND = "focus_sound"
        private const val NOTIFICATION_ID = 5303
        private const val ACTION_START = "com.studyplanner.app.action.SOUND_START"
        private const val ACTION_PAUSE = "com.studyplanner.app.action.SOUND_PAUSE"
        private const val ACTION_RESUME = "com.studyplanner.app.action.SOUND_RESUME"
        private const val ACTION_STOP = "com.studyplanner.app.action.SOUND_STOP"
        private const val ACTION_VOLUME = "com.studyplanner.app.action.SOUND_VOLUME"
        private const val EXTRA_VOLUME = "com.studyplanner.app.extra.SOUND_VOLUME"

        @Volatile
        private var running = false

        fun isRunning(): Boolean = running

        fun start(context: Context) {
            val settings = SettingsRepository.get(context).settings.value
            val hasSource = settings.builtinSound != null || !settings.soundPath.isNullOrBlank()
            if (!settings.deepFocusMode || !settings.soundEnabled || !hasSource) return
            val intent = Intent(context, FocusSoundService::class.java).setAction(ACTION_START)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun pause(context: Context) {
            if (running) send(context, ACTION_PAUSE)
        }

        fun resume(context: Context) {
            // A session paused before the sound service existed (for example, Deep Focus was
            // switched on mid-session) starts playback on resume.
            if (running) send(context, ACTION_RESUME) else start(context)
        }

        fun stop(context: Context) {
            send(context, ACTION_STOP)
        }

        fun updateVolume(context: Context, volume: Float) {
            if (!running) return
            send(context, ACTION_VOLUME) { putExtra(EXTRA_VOLUME, volume) }
        }

        private fun send(context: Context, action: String, fill: Intent.() -> Unit = {}) {
            val intent = Intent(context, FocusSoundService::class.java).setAction(action).apply(fill)
            runCatching { context.startService(intent) }
        }
    }
}
