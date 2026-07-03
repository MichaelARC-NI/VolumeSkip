package com.volumeskip

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.widget.Toast

/**
 * Ejecuta acciones de medios usando múltiples estrategias
 * para máxima compatibilidad con distintas apps de música.
 */
class MediaActionHandler(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Siguiente canción — usando múltiples métodos
     */
    fun nextTrack() {
        // Método 1: AudioManager dispatch (funciona en la mayoría)
        dispatchKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        // Método 2: TransportControls por si acaso
        trySkipToNext()
    }

    /**
     * Canción anterior — usando múltiples métodos
     */
    fun previousTrack() {
        // Método 1: AudioManager dispatch
        dispatchKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        // Método 2: También KEYCODE_MEDIA_SKIP_BACKWARD para apps que lo soportan
        dispatchKey(KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)
        // Método 3: TransportControls
        trySkipToPrevious()
    }

    fun playPause() {
        dispatchKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    private fun dispatchKey(keyCode: Int) {
        try {
            val now = SystemClock.uptimeMillis()
            val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
            val up   = KeyEvent(now, now, KeyEvent.ACTION_UP,   keyCode, 0)
            audioManager.dispatchMediaKeyEvent(down)
            audioManager.dispatchMediaKeyEvent(up)
        } catch (e: Exception) {
            // Ignorar
        }
    }

    /**
     * Intenta usar MediaController.TransportControls.skipToNext()
     * como respaldo para apps que no responden a KeyEvent.
     */
    private fun trySkipToNext() {
        try {
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                msm.getActiveSessions(null)
            } else {
                @Suppress("DEPRECATION")
                msm.getActiveSessions(null)
            }
            if (sessions.isNotEmpty()) {
                sessions[0].transportControls.skipToNext()
            }
        } catch (e: Exception) {
            // Sin permisos para sesiones activas — ignorar
        }
    }

    /**
     * Intenta usar MediaController.TransportControls.skipToPrevious()
     */
    private fun trySkipToPrevious() {
        try {
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                msm.getActiveSessions(null)
            } else {
                @Suppress("DEPRECATION")
                msm.getActiveSessions(null)
            }
            if (sessions.isNotEmpty()) {
                sessions[0].transportControls.skipToPrevious()
            }
        } catch (e: Exception) {
            // Sin permisos — ignorar
        }
    }
}
