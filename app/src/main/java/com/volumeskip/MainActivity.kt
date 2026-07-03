package com.volumeskip

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var volumeSkipManager: VolumeSkipManager
    private var serviceBound = false
    private var serviceIntent: Intent? = null

    private lateinit var btnToggle: Button
    private lateinit var btnAccessibility: Button
    private lateinit var statusText: TextView
    private lateinit var accessibilityStatus: TextView
    private lateinit var instructionsText: TextView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBound = true
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        volumeSkipManager = VolumeSkipManager(this)
        serviceIntent = Intent(this, VolumeSkipService::class.java)

        btnToggle = findViewById(R.id.btnToggle)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        statusText = findViewById(R.id.statusText)
        accessibilityStatus = findViewById(R.id.accessibilityStatus)
        instructionsText = findViewById(R.id.instructionsText)

        // Botón de servicio
        btnToggle.setOnClickListener {
            if (serviceBound) {
                stopService()
            } else {
                startService()
            }
        }

        // Botón de Accessibility Service
        btnAccessibility.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                // Abrir configuración de accesibilidad
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this,
                    "Busca 'VolumeSkip' y actívalo en Accesibilidad",
                    Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this,
                    "✅ Accessibility Service ya activo",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Botones de prueba
        findViewById<Button>(R.id.btnTestNext).setOnClickListener {
            val handler = MediaActionHandler(this)
            handler.nextTrack()
            Toast.makeText(this, "⏭ Siguiente", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnTestPrev).setOnClickListener {
            val handler = MediaActionHandler(this)
            handler.previousTrack()
            Toast.makeText(this, "⏮ Anterior", Toast.LENGTH_SHORT).show()
        }

        // Botones de redes sociales
        findViewById<Button>(R.id.btnFacebook).setOnClickListener {
            openUrl("https://www.facebook.com/share/1D1pfVdbXE/")
        }
        findViewById<Button>(R.id.btnTelegram).setOnClickListener {
            openUrl("https://t.me/Michael_Antonio_Rodriguez")
        }
        findViewById<Button>(R.id.btnWhatsApp).setOnClickListener {
            openUrl("https://wa.me/50583341349?text=Hola%20Michael")
        }
        findViewById<Button>(R.id.btnYouTube).setOnClickListener {
            openUrl("https://youtube.com/@androidmovil?si=dqzoWBDy1EsNaM7v")
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        checkNotificationPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeSkipManager.destroy()
        try { unbindService(connection) } catch (e: Exception) {}
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && volumeSkipManager.handleKeyEvent(event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && volumeSkipManager.handleKeyEvent(event)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun startService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent!!)
        } else {
            startService(serviceIntent!!)
        }
        bindService(serviceIntent!!, connection, BIND_AUTO_CREATE)
        Toast.makeText(this, "✅ Servicio activado", Toast.LENGTH_SHORT).show()
    }

    private fun stopService() {
        if (serviceIntent != null) stopService(serviceIntent!!)
        try { unbindService(connection) } catch (e: Exception) {}
        serviceBound = false
        updateUI()
        Toast.makeText(this, "⏹ Servicio desactivado", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        val accEnabled = isAccessibilityServiceEnabled()

        if (accEnabled) {
            accessibilityStatus.text = "● Accesibilidad: ACTIVA"
            accessibilityStatus.setTextColor(0xFFFFD700.toInt())
            btnAccessibility.text = "✅ Accesibilidad OK"
            btnAccessibility.setBackgroundColor(0xFF4CAF50.toInt())
        } else {
            accessibilityStatus.text = "○ Accesibilidad: INACTIVA"
            accessibilityStatus.setTextColor(0xFFA0848F.toInt())
            btnAccessibility.text = "⚙ ACTIVAR ACCESIBILIDAD"
            btnAccessibility.setBackgroundColor(0xFFFF4D6D.toInt())
        }

        if (serviceBound) {
            btnToggle.setTextColor(0xFFFFFFFF.toInt())
            btnToggle.setBackgroundColor(0xFFFF4D6D.toInt())
            btnToggle.text = "⏹ DESACTIVAR SERVICIO"
            statusText.text = "● Servicio: ACTIVO"
            statusText.setTextColor(0xFFFFD700.toInt())
        } else {
            btnToggle.setTextColor(0xFF0D0208.toInt())
            btnToggle.setBackgroundColor(0xFFFFD700.toInt())
            btnToggle.text = "▶ ACTIVAR SERVICIO"
            statusText.text = "○ Servicio: INACTIVO"
            statusText.setTextColor(0xFFA0848F.toInt())
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        try {
            val enabled = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return enabled?.contains(packageName + "/.VolumeKeyAccessibilityService") == true
        } catch (e: Exception) {
            return false
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                Toast.makeText(this,
                    "Concede permisos de notificación en Configuración",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "No se puede abrir el enlace", Toast.LENGTH_SHORT).show()
        }
    }
}
