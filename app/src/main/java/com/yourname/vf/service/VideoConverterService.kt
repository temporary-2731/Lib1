package com.yourname.vf.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.yourname.vf.manager.ConversionManager
import com.yourname.vf.media.ConversionEngine
import com.yourname.vf.model.ConversionState
import kotlinx.coroutines.*

class VideoConverterService : Service() {
    companion object {
        const val CHANNEL_ID = "vf_conversion"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.yourname.vf.START"
        const val ACTION_PAUSE = "com.yourname.vf.PAUSE"
        const val ACTION_RESUME = "com.yourname.vf.RESUME"
        const val ACTION_CANCEL = "com.yourname.vf.CANCEL"
    }

    private var engine: ConversionEngine? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val json = intent.getStringExtra("state") ?: return START_NOT_STICKY
                val state = Gson().fromJson(json, ConversionState::class.java)
                ConversionManager.setState(state)
                startForeground(NOTIFICATION_ID, buildNotification("Starting...", 0, 0))
                engine = ConversionEngine(state)
                job = scope.launch {
                    engine?.start { progress, elapsed, remaining ->
                        val pct = (progress * 100).toInt()
                        updateNotification("Converting... $pct%", pct, 100)
                        ConversionManager.updateChunkProgress(0, 0, elapsed, remaining)
                    }
                }
            }
            ACTION_PAUSE -> { engine?.pause(); updateNotification("Paused", 0, 0) }
            ACTION_RESUME -> {}
            ACTION_CANCEL -> { engine?.cancel(); stopSelf() }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(text: String, progress: Int, max: Int): Notification {
        val pauseIntent = PendingIntent.getService(this, 0,
            Intent(this, VideoConverterService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_IMMUTABLE)
        val cancelIntent = PendingIntent.getService(this, 1,
            Intent(this, VideoConverterService::class.java).apply { action = ACTION_CANCEL },
            PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VF Conversion")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setProgress(max, progress, false)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
            .build()
    }

    private fun updateNotification(text: String, progress: Int, max: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text, progress, max))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Conversion", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
