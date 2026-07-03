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
 * Accessibility Service que intercepta las teclas de volumen.
 *
 * - Presión corta (< 400ms) → Volumen normal
 * - Presión larga (≥ 400ms) → Adelantar/Retroceder canción
 *
 * En pantalla de bloqueo, compensa inmediatamente cada auto-repetición
 * de volumen que el sistema aplique antes de entregarnos el evento.
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

    // Vol- state
    private var volDownFirstTime = 0L
    private var volDownConsumed = false
    private var volDownPending = false

    // Vol+ state
    private var volUpFirstTime = 0L
    private var volUpConsumed = false
    private var volUpPending = false

    private val volDownRunnable = Runnable {
        if (volDownPending) {
            volDownConsumed = true
            volDownPending = false
            Log.d(TAG, "← Vol- LARGO: RETROCEDER")
            sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            // Compensación extra por si alguna repetición se coló
            compensateVolume(AudioManager.ADJUST_RAISE)
        }
    }

    private val volUpRunnable = Runnable {
        if (volUpPending) {
            volUpConsumed = true
            volUpPending = false
            Log.d(TAG, "→ Vol+ LARGO: ADELANTAR")
            sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            // Compensación extra por si alguna repetición se coló
            compensateVolume(AudioManager.ADJUST_LOWER)
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
            Log.e(TAG, "Error: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

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

        val isDown = event.action == KeyEvent.ACTION_DOWN
        val repeat = event.repeatCount

        Log.d(TAG, "${if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) "Vol+" else "Vol-"} " +
                "${if (isDown) "▼" else "▲"} (r=$repeat)")

        if (isDown) {
            if (repeat == 0) {
                // Presión nueva — iniciar timer para detectar pulso largo
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    handler.removeCallbacks(volDownRunnable)
                    volDownFirstTime = SystemClock.uptimeMillis()
                    volDownConsumed = false
                    volDownPending = true
                    handler.postDelayed(volDownRunnable, LONG_PRESS_MS)
                } else {
                    handler.removeCallbacks(volUpRunnable)
                    volUpFirstTime = SystemClock.uptimeMillis()
                    volUpConsumed = false
                    volUpPending = true
                    handler.postDelayed(volUpRunnable, LONG_PRESS_MS)
                }
            } else {
                // Auto-repetición del sistema → ya cambió volumen, compensar al instante
                val dir = if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                    AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                compensateVolume(dir)
            }
            return true
        }

        // ACTION_UP
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            handler.removeCallbacks(volDownRunnable)
            volDownPending = false
            if (volDownConsumed) {
                volDownConsumed = false
                return true
            }
            // Pulso corto → aplicar volumen
            changeVolume(AudioManager.ADJUST_LOWER)
        } else {
            handler.removeCallbacks(volUpRunnable)
            volUpPending = false
            if (volUpConsumed) {
                volUpConsumed = false
                return true
            }
            changeVolume(AudioManager.ADJUST_RAISE)
        }
        return true
    }

    private fun sendKey(keyCode: Int) {
        try {
            val am = audioManager ?: return
            val now = SystemClock.uptimeMillis()
            am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        } catch (e: Exception) {
            Log.e(TAG, "Error sendKey: ${e.message}")
        }
    }

    /** Compensa volumen sin sonido/UI (para contrarrestar cambios del sistema) */
    private fun compensateVolume(direction: Int) {
        try {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, direction, 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error compensate: ${e.message}")
        }
    }

    /** Cambio de volumen normal con sonido e indicador visual */
    private fun changeVolume(direction: Int) {
        try {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, direction,
                AudioManager.FLAG_PLAY_SOUND or AudioManager.FLAG_SHOW_UI
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error volume: ${e.message}")
        }
    }
}
