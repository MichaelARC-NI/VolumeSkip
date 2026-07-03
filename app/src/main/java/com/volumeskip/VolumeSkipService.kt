package com.volumeskip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import androidx.core.app.NotificationCompat

/**
 * Servicio en primer plano que mantiene activo el MediaSession
 * para controlar la música.
 */
class VolumeSkipService : Service() {

    companion object {
        const val CHANNEL_ID = "volume_skip_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.volumeskip.STOP"
    }

    private var volumeSkipManager: VolumeSkipManager? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        volumeSkipManager = VolumeSkipManager(this)
        setupMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        volumeSkipManager?.destroy()
        mediaSession?.release()
        super.onDestroy()
    }

    private fun setupMediaSession() {
        val session = MediaSession(this, "VolumeSkip")
        session.setCallback(object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                val event: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                }
                if (event != null) {
                    val handled = volumeSkipManager?.handleKeyEvent(event) ?: false
                    if (handled) return true
                }
                return super.onMediaButtonEvent(mediaButtonIntent)
            }

            override fun onSkipToNext() {
                val mgr = volumeSkipManager
                if (mgr != null) {
                    val key = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT)
                    mgr.handleKeyEvent(key)
                }
            }

            override fun onSkipToPrevious() {
                val mgr = volumeSkipManager
                if (mgr != null) {
                    val key = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    mgr.handleKeyEvent(key)
                }
            }
        })
        session.isActive = true
        mediaSession = session
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Control de Música",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación para controlar música con los botones de volumen"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VolumeSkip")
            .setContentText("Mantén Vol+ para siguiente, Vol- para anterior")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
