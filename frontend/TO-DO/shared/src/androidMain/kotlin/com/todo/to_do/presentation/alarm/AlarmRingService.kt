package com.todo.to_do.presentation.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

private const val CHANNEL_ID = "task_flow_reminders"
private const val NOTIFICATION_ID = 4200
private const val RING_DURATION_MS = 60_000L
private const val ACTION_DISMISS = "com.todo.to_do.alarm.DISMISS"

class AlarmRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val stopHandler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_ALARM_TITLE).orEmpty().ifBlank { "Task reminder" }
        startForeground(NOTIFICATION_ID, buildNotification(title))
        startRinging()
        stopHandler.postDelayed(stopRunnable, RING_DURATION_MS)
        return START_NOT_STICKY
    }

    private fun startRinging() {
        val alarmUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            runCatching {
                setDataSource(this@AlarmRingService, alarmUri)
                prepare()
                start()
            }
        }
    }

    private fun buildNotification(title: String): Notification {
        ensureChannel()

        val dismissIntent = Intent(this, AlarmRingService::class.java).setAction(ACTION_DISMISS)
        val dismissPendingIntent = PendingIntent.getService(
            this,
            0,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Reminder")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Task reminders"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopHandler.removeCallbacks(stopRunnable)
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
        super.onDestroy()
    }
}

