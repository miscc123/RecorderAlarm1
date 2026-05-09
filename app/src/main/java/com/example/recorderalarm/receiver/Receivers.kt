package com.example.recorderalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.recorderalarm.data.AlarmDatabase
import com.example.recorderalarm.service.AlarmService
import com.example.recorderalarm.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val label = intent.getStringExtra("ALARM_LABEL") ?: ""
        val recordingPath = intent.getStringExtra("RECORDING_PATH") ?: ""

        // Start foreground service to play the alarm
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
            putExtra("RECORDING_PATH", recordingPath)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule all enabled alarms after reboot
            CoroutineScope(Dispatchers.IO).launch {
                val dao = AlarmDatabase.getInstance(context).alarmDao()
                // Collect flow once using first()
                val alarms = mutableListOf<com.example.recorderalarm.data.Alarm>()
                dao.getAllAlarms().collect { list ->
                    alarms.addAll(list)
                    return@collect
                }
                alarms.filter { it.isEnabled }.forEach { alarm ->
                    AlarmScheduler.schedule(context, alarm)
                }
            }
        }
    }
}
