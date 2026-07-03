package com.volumeskip

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

/**
 * Ejecuta acciones de medios SOLO con AudioManager.dispatchMediaKeyEvent().
 * Es el método más confiable y compatible con segundo plano/pantalla apagada.
 *
 * - Vol+ largo → KEYCODE_MEDIA_NEXT (siguiente canción)
 * - Vol- largo → KEYCODE_MEDIA_PREVIOUS (canción anterior)
 */
class MediaActionHandler(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun nextTrack() {
        dispatchKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previousTrack() {
        dispatchKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
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
            // Silencioso
        }
    }
}
