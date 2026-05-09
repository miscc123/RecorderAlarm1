package com.example.recorderalarm.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.recorderalarm.databinding.ActivityAlarmRingBinding
import com.example.recorderalarm.service.AlarmService

class AlarmRingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        binding = ActivityAlarmRingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val label = intent.getStringExtra("ALARM_LABEL") ?: "鬧鐘"
        binding.tvRingLabel.text = label.ifBlank { "鬧鐘響了！" }

        binding.btnDismiss.setOnClickListener { dismiss() }
        binding.btnSnooze.setOnClickListener { snooze() }
    }

    private fun dismiss() {
        stopAlarmService()
        finish()
    }

    private fun snooze() {
        // Snooze 5 minutes: stop now, reschedule handled by user (simplified)
        stopAlarmService()
        android.widget.Toast.makeText(this, "稍後 5 分鐘再響", android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        startService(intent)
    }

    override fun onBackPressed() {
        // Prevent back-press dismissing without stopping
        dismiss()
    }
}
