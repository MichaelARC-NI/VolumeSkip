package com.volumeskip

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service que intercepta las teclas de volumen
 * a nivel de sistema, incluso con pantalla apagada.
 *
 * Detecta presión larga:
 * - Vol+ sostenido → Siguiente canción
 * - Vol- sostenido → Canción anterior
 * - Presión corta → volumen normal (deja pasar el evento)
 */
class VolumeKeyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VolumeKeyAccess"
        private const val LONG_PRESS_MS = 400L
        var isRunning = false
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private val audioManager: AudioManager? by lazy {
        getSystemService(AUDIO_SERVICE) as? AudioManager
    }

    // Estado de las teclas
    private var volDownTime = 0L
    private var volUpTime = 0L
    private var volDownConsumed = false
    private var volUpConsumed = false
    private var volDownPending = false
    private var volUpPending = false

    private val volDownRunnable = Runnable {
        if (volDownPending) {
            volDownConsumed = true
            volDownPending = false
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            Log.d(TAG, "← Long-press Vol-: ANTERIOR")
        }
    }

    private val volUpRunnable = Runnable {
        if (volUpPending) {
            volUpConsumed = true
            volUpPending = false
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            Log.d(TAG, "→ Long-press Vol+: SIGUIENTE")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d(TAG, "Accessibility Service conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No necesitamos eventos de UI, solo teclas
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrumpido")
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    volDownTime = SystemClock.uptimeMillis()
                    volDownConsumed = false
                    volDownPending = true
                    handler.postDelayed(volDownRunnable, LONG_PRESS_MS)
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    volUpTime = SystemClock.uptimeMillis()
                    volUpConsumed = false
                    volUpPending = true
                    handler.postDelayed(volUpRunnable, LONG_PRESS_MS)
                }
                // Siempre retornamos true en ACTION_DOWN para interceptar
                return true
            }

            KeyEvent.ACTION_UP -> {
                val elapsed: Long
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    elapsed = SystemClock.uptimeMillis() - volDownTime
                    handler.removeCallbacks(volDownRunnable)
                    volDownPending = false

                    if (volDownConsumed) {
                        // Ya procesamos el long-press
                        volDownConsumed = false
                        return true
                    }

                    if (elapsed < LONG_PRESS_MS) {
                        // Presión corta: dejar pasar el evento de volumen normal
                        volDownConsumed = false
                        return false
                    }

                    return true
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    elapsed = SystemClock.uptimeMillis() - volUpTime
                    handler.removeCallbacks(volUpRunnable)
                    volUpPending = false

                    if (volUpConsumed) {
                        volUpConsumed = false
                        return true
                    }

                    if (elapsed < LONG_PRESS_MS) {
                        volUpConsumed = false
                        return false
                    }

                    return true
                }
            }
        }
        return false
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val am = audioManager ?: return
        val now = SystemClock.uptimeMillis()
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up   = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)
        am.dispatchMediaKeyEvent(down)
        am.dispatchMediaKeyEvent(up)
    }
}
