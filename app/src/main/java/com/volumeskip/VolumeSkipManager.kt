package com.volumeskip

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent

/**
 * Detecta presiones largas de los botones de volumen
 * y las convierte en comandos de música (siguiente/anterior).
 *
 * Funciona como los Motorola: mantén presionado Vol+ → siguiente canción
 * mantén presionado Vol- → canción anterior
 */
class VolumeSkipManager(private val context: Context) {

    companion object {
        private const val LONG_PRESS_TIMEOUT = 400L // ms para considerar "presión larga"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private val mediaAction = MediaActionHandler(context)

    // Estado de las teclas
    private var volDownLongPress = false
    private var volUpLongPress = false
    private var volDownDownTime = 0L
    private var volUpDownTime = 0L

    // Callbacks para detectar long-press
    private val volDownRunnable = Runnable {
        volDownLongPress = true
        mediaAction.previousTrack()
    }
    private val volUpRunnable = Runnable {
        volUpLongPress = true
        mediaAction.nextTrack()
    }

    /**
     * Procesa eventos KeyEvent de los botones de volumen.
     * Retorna true si el evento fue consumido.
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    volDownDownTime = SystemClock.uptimeMillis()
                    volDownLongPress = false
                    handler.postDelayed(volDownRunnable, LONG_PRESS_TIMEOUT)
                    return true
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    volUpDownTime = SystemClock.uptimeMillis()
                    volUpLongPress = false
                    handler.postDelayed(volUpRunnable, LONG_PRESS_TIMEOUT)
                    return true
                }
            }
            KeyEvent.ACTION_UP -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    handler.removeCallbacks(volDownRunnable)
                    if (!volDownLongPress) {
                        // Presión corta — dejar que el sistema maneje el volumen normal
                        return false
                    }
                    volDownLongPress = false
                    return true
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    handler.removeCallbacks(volUpRunnable)
                    if (!volUpLongPress) {
                        return false
                    }
                    volUpLongPress = false
                    return true
                }
            }
        }
        return false
    }

    /**
     * Libera recursos
     */
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
    }
}
