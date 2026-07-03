package com.volumeskip

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
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
 * - Presión corta → Volumen normal (restaura el cambio de volumen)
 * - Presión larga (400ms) → Adelantar/Retroceder canción
 *
 * Se puede desactivar desde la app (SharedPreferences "disabled").
 */
class VolumeKeyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VolumeSkip[Acc]"
        private const val LONG_PRESS_MS = 400L
        private const val PREFS_NAME = "volume_skip_prefs"
        private const val KEY_DISABLED = "accessibility_disabled"
        var isRunning = false
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private var audioManager: AudioManager? = null
    private var prefs: SharedPreferences? = null

    // Estado de teclas
    private var volDownTime = 0L
    private var volUpTime = 0L
    private var volDownConsumed = false
    private var volUpConsumed = false
    private var volDownPending = false
    private var volUpPending = false
    private var volDownKey = false
    private var volUpKey = false

    private val volDownRunnable = Runnable {
        if (volDownPending) {
            volDownConsumed = true
            volDownPending = false
            Log.d(TAG, "← Vol- LARGO: RETROCEDER")
            sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    private val volUpRunnable = Runnable {
        if (volUpPending) {
            volUpConsumed = true
            volUpPending = false
            Log.d(TAG, "→ Vol+ LARGO: ADELANTAR")
            sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
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

    /**
     * Intercepta las teclas de volumen.
     *
     * En ACTION_DOWN: interceptamos para evitar el cambio de volumen normal.
     * En ACTION_UP:
     *   - Si fue presión corta: restauramos el volumen manualmente (1 paso)
     *   - Si fue presión larga: ya se envió el comando de música
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Si está desactivado en preferencias, dejar pasar todo
        if (prefs?.getBoolean(KEY_DISABLED, false) == true) {
            return false
        }

        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    volDownKey = true
                    handler.removeCallbacks(volDownRunnable)
                    volDownTime = SystemClock.uptimeMillis()
                    volDownConsumed = false
                    volDownPending = true
                    handler.postDelayed(volDownRunnable, LONG_PRESS_MS)
                } else {
                    volUpKey = true
                    handler.removeCallbacks(volUpRunnable)
                    volUpTime = SystemClock.uptimeMillis()
                    volUpConsumed = false
                    volUpPending = true
                    handler.postDelayed(volUpRunnable, LONG_PRESS_MS)
                }
                // Interceptamos para evitar que el sistema cambie el volumen
                return true
            }

            KeyEvent.ACTION_UP -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    handler.removeCallbacks(volDownRunnable)
                    val elapsed = SystemClock.uptimeMillis() - volDownTime
                    volDownPending = false
                    volDownKey = false

                    if (volDownConsumed) {
                        // Presión larga: ya enviamos comando de música
                        volDownConsumed = false
                        return true
                    }

                    if (elapsed < LONG_PRESS_MS) {
                        // Presión corta: restaurar volumen manualmente
                        adjustVolume(AudioManager.ADJUST_LOWER)
                        return true
                    }

                    return true
                } else {
                    handler.removeCallbacks(volUpRunnable)
                    val elapsed = SystemClock.uptimeMillis() - volUpTime
                    volUpPending = false
                    volUpKey = false

                    if (volUpConsumed) {
                        volUpConsumed = false
                        return true
                    }

                    if (elapsed < LONG_PRESS_MS) {
                        // Presión corta: restaurar volumen manualmente
                        adjustVolume(AudioManager.ADJUST_RAISE)
                        return true
                    }

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

    private fun adjustVolume(direction: Int) {
        try {
            audioManager?.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
            Log.e(TAG, "Error al ajustar volumen: ${e.message}")
        }
    }
}
