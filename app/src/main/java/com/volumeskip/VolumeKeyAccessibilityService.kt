package com.volumeskip

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service que intercepta las teclas de volumen
 * a NIVEL DE SISTEMA, incluso con pantalla apagada.
 *
 * Requiere:
 * - android:canRequestFilterKeyEvents="true" en XML
 * - FLAG_REQUEST_FILTER_KEY_EVENTS en onServiceConnected()
 *
 * Funciona como los Motorola:
 * - Vol+ sostenido (400ms) → Siguiente canción
 * - Vol- sostenido (400ms) → Canción anterior
 * - Presión corta → Volumen normal (deja pasar el evento)
 */
class VolumeKeyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VolumeSkip[Access]"
        private const val LONG_PRESS_MS = 400L
        var isRunning = false
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private val audioManager: AudioManager? by lazy {
        getSystemService(AUDIO_SERVICE) as? AudioManager
    }

    // Estado de cada tecla
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
            Log.d(TAG, "← PRESIÓN LARGA Vol-: ANTERIOR")
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            ToastCompat.show(this, "⏮ Anterior")
        }
    }

    private val volUpRunnable = Runnable {
        if (volUpPending) {
            volUpConsumed = true
            volUpPending = false
            Log.d(TAG, "→ PRESIÓN LARGA Vol+: SIGUIENTE")
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            ToastCompat.show(this, "⏭ Siguiente")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // ⚠️ FUNDAMENTAL: Activar filtro de teclas
        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info

        isRunning = true
        Log.d(TAG, "✅ Accessibility Service CONECTADO - Filtro de teclas activado")
        ToastCompat.show(this, "✅ VolumeSkip accesibilidad activa")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No necesitamos eventos de ventana
    }

    override fun onInterrupt() {
        Log.d(TAG, "⚠️ Accessibility Service interrumpido")
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "❌ Accessibility Service destruido")
        super.onDestroy()
    }

    /**
     * Este método se llama para CADA TECLA presionada,
     * incluyendo VOLUMEN, cuando FLAG_REQUEST_FILTER_KEY_EVENTS está activo.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode

        // Solo nos interesan las teclas de volumen
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        Log.d(TAG, "Tecla: ${if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) "VOL+" else "VOL-"} " +
                "Acción: ${if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"} " +
                "Repeat: ${event.repeatCount}")

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    handler.removeCallbacks(volDownRunnable)
                    volDownTime = SystemClock.uptimeMillis()
                    volDownConsumed = false
                    volDownPending = true
                    handler.postDelayed(volDownRunnable, LONG_PRESS_MS)
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    handler.removeCallbacks(volUpRunnable)
                    volUpTime = SystemClock.uptimeMillis()
                    volUpConsumed = false
                    volUpPending = true
                    handler.postDelayed(volUpRunnable, LONG_PRESS_MS)
                }
                return true // Consumimos siempre el ACTION_DOWN
            }

            KeyEvent.ACTION_UP -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    handler.removeCallbacks(volDownRunnable)
                    val elapsed = SystemClock.uptimeMillis() - volDownTime
                    volDownPending = false

                    if (volDownConsumed) {
                        // Long-press ya se ejecutó, consumimos el UP
                        volDownConsumed = false
                        Log.d(TAG, "← Vol- UP (long-press ya procesado)")
                        return true
                    }

                    if (elapsed < LONG_PRESS_MS) {
                        // Presión corta: dejar pasar al sistema para volumen normal
                        Log.d(TAG, "← Vol- presión corta ($elapsed ms) → volumen normal")
                        return false
                    }

                    return true
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    handler.removeCallbacks(volUpRunnable)
                    val elapsed = SystemClock.uptimeMillis() - volUpTime
                    volUpPending = false

                    if (volUpConsumed) {
                        volUpConsumed = false
                        Log.d(TAG, "→ Vol+ UP (long-press ya procesado)")
                        return true
                    }

                    if (elapsed < LONG_PRESS_MS) {
                        Log.d(TAG, "→ Vol+ presión corta ($elapsed ms) → volumen normal")
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
