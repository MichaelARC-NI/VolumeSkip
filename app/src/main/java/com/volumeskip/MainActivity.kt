package com.volumeskip

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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
    private lateinit var statusText: TextView
    private lateinit var instructionsText: TextView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBound = true
            updateUI(true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            updateUI(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        volumeSkipManager = VolumeSkipManager(this)
        serviceIntent = Intent(this, VolumeSkipService::class.java)

        btnToggle = findViewById(R.id.btnToggle)
        statusText = findViewById(R.id.statusText)
        instructionsText = findViewById(R.id.instructionsText)

        btnToggle.setOnClickListener {
            if (serviceBound) {
                stopService()
            } else {
                startService()
            }
        }

        findViewById<Button>(R.id.btnTestNext).setOnClickListener {
            toggleMusic()
            val handler = MediaActionHandler(this)
            handler.nextTrack()
            Toast.makeText(this, "⏭ Siguiente canción", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnTestPrev).setOnClickListener {
            val handler = MediaActionHandler(this)
            handler.previousTrack()
            Toast.makeText(this, "⏮ Canción anterior", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
        if (serviceBound) updateUI(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeSkipManager.destroy()
        try { unbindService(connection) } catch (e: Exception) {}
    }

    /**
     * Intercepta botones de volumen para detectar presión larga
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && volumeSkipManager.handleKeyEvent(event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && volumeSkipManager.handleKeyEvent(event)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun startService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent!!)
        } else {
            startService(serviceIntent!!)
        }
        bindService(serviceIntent!!, connection, BIND_AUTO_CREATE)
        Toast.makeText(this, "✅ VolumeSkip activado", Toast.LENGTH_SHORT).show()
    }

    private fun stopService() {
        if (serviceIntent != null) {
            stopService(serviceIntent!!)
        }
        try { unbindService(connection) } catch (e: Exception) {}
        serviceBound = false
        updateUI(false)
        Toast.makeText(this, "⏹ VolumeSkip desactivado", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI(active: Boolean) {
        if (active) {
            btnToggle.setTextColor(0xFF0D0208.toInt())
            btnToggle.setBackgroundColor(0xFFFF4D6D.toInt())
            btnToggle.text = "⏹ DESACTIVAR"
            statusText.text = "●  ACTIVO"
            statusText.setTextColor(0xFFFFD700.toInt())
            instructionsText.setTextColor(0xFFFFFFFF.toInt())
        } else {
            btnToggle.setTextColor(0xFF0D0208.toInt())
            btnToggle.setBackgroundColor(0xFFFFD700.toInt())
            btnToggle.text = "▶  ACTIVAR"
            statusText.text = "○  INACTIVO"
            statusText.setTextColor(0xFFA0848F.toInt())
            instructionsText.setTextColor(0xFF5A4A50.toInt())
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

    private fun toggleMusic() {
        // Auto-start service on first interaction
        if (!serviceBound) {
            startService()
        }
    }
}
