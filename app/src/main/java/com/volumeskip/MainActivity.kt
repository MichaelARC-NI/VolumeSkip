package com.volumeskip

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "volume_skip_prefs"
        private const val KEY_DISABLED = "accessibility_disabled"
    }

    private lateinit var volumeSkipManager: VolumeSkipManager
    private var serviceBound = false
    private var serviceIntent: Intent? = null
    private lateinit var prefs: SharedPreferences

    private lateinit var btnToggle: Button
    private lateinit var btnAccessibility: Button
    private lateinit var statusText: TextView
    private lateinit var accessibilityStatus: TextView
    private lateinit var instructionsText: TextView
    private lateinit var switchDisable: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        volumeSkipManager = VolumeSkipManager(this)
        serviceIntent = Intent(this, VolumeSkipService::class.java)

        btnToggle = findViewById(R.id.btnToggle)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        statusText = findViewById(R.id.statusText)
        accessibilityStatus = findViewById(R.id.accessibilityStatus)
        instructionsText = findViewById(R.id.instructionsText)
        switchDisable = findViewById(R.id.switchDisable)

        // Estado del switch
        switchDisable.isChecked = prefs.getBoolean(KEY_DISABLED, false)
        switchDisable.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DISABLED, isChecked).apply()
            if (isChecked) {
                Toast.makeText(this, "⚪ VolumeSkip desactivado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "🟢 VolumeSkip activado", Toast.LENGTH_SHORT).show()
            }
        }

        btnToggle.setOnClickListener {
            if (serviceBound) stopService()
            else startService()
        }

        btnAccessibility.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this,
                    "Busca 'VolumeSkip' y actívalo en Accesibilidad",
                    Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "✅ Accesibilidad ya activa", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnTestNext).setOnClickListener {
            val handler = MediaActionHandler(this)
            handler.nextTrack()
            Toast.makeText(this, "⏭ Adelantar", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnTestPrev).setOnClickListener {
            val handler = MediaActionHandler(this)
            handler.previousTrack()
            Toast.makeText(this, "⏮ Retroceder", Toast.LENGTH_SHORT).show()
        }

        // Redes sociales
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
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeSkipManager.destroy()
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
        Toast.makeText(this, "✅ Servicio activado", Toast.LENGTH_SHORT).show()
    }

    private fun stopService() {
        if (serviceIntent != null) stopService(serviceIntent!!)
        Toast.makeText(this, "⏹ Servicio desactivado", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        val accEnabled = isAccessibilityServiceEnabled()
        val disabled = prefs.getBoolean(KEY_DISABLED, false)

        if (accEnabled) {
            accessibilityStatus.text = if (disabled) "● Accesibilidad: DESACTIVADA"
                                       else "● Accesibilidad: ACTIVA"
            accessibilityStatus.setTextColor(
                if (disabled) 0xFFA0848F.toInt() else 0xFFFFD700.toInt())
            btnAccessibility.text = if (disabled) "⚪ DESACTIVADA (ir a Ajustes)"
                                    else "✅ Accesibilidad OK"
            btnAccessibility.setBackgroundColor(
                if (disabled) 0xFF5A4A50.toInt() else 0xFF4CAF50.toInt())
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

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {}
    }
}
