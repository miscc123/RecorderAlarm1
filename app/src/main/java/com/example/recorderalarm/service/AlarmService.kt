package com.example.recorderalarm.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.recorderalarm.R
import com.example.recorderalarm.ui.AlarmRingActivity
import java.io.File

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.example.recorderalarm.STOP_ALARM"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val label = intent?.getStringExtra("ALARM_LABEL") ?: "鬧鐘"
        val recordingPath = intent?.getStringExtra("RECORDING_PATH") ?: ""

        // Build notification
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this, alarmId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(label.ifBlank { "鬧鐘響了！" })
            .setContentText("點擊關閉鬧鐘")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPi, true)
            .addAction(R.drawable.ic_alarm, "停止", stopPi)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Launch ring screen
        startActivity(fullScreenIntent)

        // Play audio
        playAudio(recordingPath)

        // Vibrate
        vibrate()

        // Auto-stop after 5 minutes
        Handler(Looper.getMainLooper()).postDelayed({ stopAlarm() }, 5 * 60 * 1000L)

        return START_STICKY
    }

    private fun playAudio(path: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            isLooping = true
            try {
                if (path.isNotBlank() && File(path).exists()) {
                    setDataSource(path)
                } else {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    setDataSource(applicationContext, uri)
                }
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun vibrate() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 500, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        mediaPlayer?.release()
        vibrator?.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "鬧鐘通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "鬧鐘提醒通知"
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
