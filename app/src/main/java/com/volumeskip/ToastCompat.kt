package com.volumeskip

import android.content.Context
import android.widget.Toast

/**
 * Helper para mostrar Toasts desde cualquier contexto (incluyendo Services)
 */
object ToastCompat {
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        try {
            Toast.makeText(context.applicationContext, message, duration).show()
        } catch (e: Exception) {
            // Ignorar errores de Toast desde servicios
        }
    }
}
