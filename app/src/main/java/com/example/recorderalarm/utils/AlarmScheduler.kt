package com.example.recorderalarm.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.recorderalarm.data.Alarm
import com.example.recorderalarm.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    private const val REQUEST_CODE_BASE = 1000

    fun schedule(context: Context, alarm: Alarm) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!manager.canScheduleExactAlarms()) return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("RECORDING_PATH", alarm.recordingPath)
        }

        val days = if (alarm.repeatDays.isBlank()) emptyList()
                   else alarm.repeatDays.split(",").map { it.trim().toInt() }

        if (days.isEmpty()) {
            // One-time alarm
            val triggerAt = nextTriggerMillis(alarm.hour, alarm.minute, -1)
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BASE + alarm.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, pi), pi)
        } else {
            // Repeat alarm – schedule one PendingIntent per day-of-week
            days.forEach { dow ->
                val triggerAt = nextTriggerMillis(alarm.hour, alarm.minute, dow)
                val pi = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_BASE + alarm.id * 10 + dow,
                    intent.apply { putExtra("DOW", dow) },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, pi), pi)
            }
        }
    }

    fun cancel(context: Context, alarm: Alarm) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val days = if (alarm.repeatDays.isBlank()) listOf(-1)
                   else alarm.repeatDays.split(",").map { it.trim().toInt() }

        days.forEach { dow ->
            val reqCode = if (dow == -1) REQUEST_CODE_BASE + alarm.id
                          else REQUEST_CODE_BASE + alarm.id * 10 + dow
            val pi = PendingIntent.getBroadcast(
                context, reqCode,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { manager.cancel(it) }
        }
    }

    private fun nextTriggerMillis(hour: Int, minute: Int, dayOfWeek: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (dayOfWeek in 1..7) {
            cal.set(Calendar.DAY_OF_WEEK, dayOfWeek)
            if (cal.timeInMillis <= System.currentTimeMillis())
                cal.add(Calendar.WEEK_OF_YEAR, 1)
        } else {
            if (cal.timeInMillis <= System.currentTimeMillis())
                cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }
}
