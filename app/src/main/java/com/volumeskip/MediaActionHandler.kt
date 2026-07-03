package com.volumeskip

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

/**
 * Ejecuta acciones de medios (siguiente/anterior/reproducir)
 * usando AudioManager.dispatchMediaKeyEvent() para controlar
 * cualquier app de música que esté reproduciendo en segundo plano.
 *
 * Funciona como los Motorola: envía comandos de medios
 * al sistema que los redirige a la app de música activa.
 */
class MediaActionHandler(private val context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Siguiente canción
     */
    fun nextTrack() {
        dispatchKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    /**
     * Canción anterior
     */
    fun previousTrack() {
        dispatchKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    /**
     * Play/Pause
     */
    fun playPause() {
        dispatchKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    private fun dispatchKey(keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up   = KeyEvent(now, now, KeyEvent.ACTION_UP,   keyCode, 0)

        audioManager.dispatchMediaKeyEvent(down)
        audioManager.dispatchMediaKeyEvent(up)
    }
}
