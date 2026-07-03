package com.volumeskip

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.media.AudioManager
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
 * Funciona como los Motorola:
 * - Vol+ sostenido (400ms) → Siguiente canción
 * - Vol- sostenido (400ms) → Canción anterior
 * - Presión corta → Volumen normal
 */
class VolumeKeyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VolumeSkip[Acc]"
        private const val LONG_PRESS_MS = 400L
        var isRunning = false
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private var audioManager: AudioManager? = null

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
            Log.d(TAG, "← Vol-: RETROCEDER")
            sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    private val volUpRunnable = Runnable {
        if (volUpPending) {
            volUpConsumed = true
            volUpPending = false
            Log.d(TAG, "→ Vol+: ADELANTAR")
            sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = serviceInfo
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            serviceInfo = info
            isRunning = true
            Log.d(TAG, "✅ Accesibilidad activa")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onServiceConnected: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.d(TAG, "Interrumpido")
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
                    handler.removeCallbacks(volDownRunnable)
                    volDownTime = SystemClock.uptimeMillis()
                    volDownConsumed = false
                    volDownPending = true
                    handler.postDelayed(volDownRunnable, LONG_PRESS_MS)
                } else {
                    handler.removeCallbacks(volUpRunnable)
                    volUpTime = SystemClock.uptimeMillis()
                    volUpConsumed = false
                    volUpPending = true
                    handler.postDelayed(volUpRunnable, LONG_PRESS_MS)
                }
                return true
            }

            KeyEvent.ACTION_UP -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    handler.removeCallbacks(volDownRunnable)
                    val elapsed = SystemClock.uptimeMillis() - volDownTime
                    volDownPending = false
                    if (volDownConsumed) {
                        volDownConsumed = false
                        return true
                    }
                    if (elapsed < LONG_PRESS_MS) return false
                    return true
                } else {
                    handler.removeCallbacks(volUpRunnable)
                    val elapsed = SystemClock.uptimeMillis() - volUpTime
                    volUpPending = false
                    if (volUpConsumed) {
                        volUpConsumed = false
                        return true
                    }
                    if (elapsed < LONG_PRESS_MS) return false
                    return true
                }
            }
        }
        return false
    }

    private fun sendKey(keyCode: Int) {
        try {
            val am = audioManager ?: return
            val now = SystemClock.uptimeMillis()
            am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar tecla $keyCode: ${e.message}")
        }
    }
}
