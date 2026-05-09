package com.example.recorderalarm.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recorderalarm.data.Alarm
import com.example.recorderalarm.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vm: AlarmViewModel
    private lateinit var adapter: AlarmAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val denied = perms.filter { !it.value }.keys
        if (denied.isNotEmpty()) {
            Snackbar.make(binding.root, "需要麥克風權限才能錄音", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm = ViewModelProvider(this, AlarmViewModelFactory(application))[AlarmViewModel::class.java]

        setupRecyclerView()
        setupFab()
        requestPermissions()

        vm.alarms.observe(this) { list ->
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupRecyclerView() {
        adapter = AlarmAdapter(
            onToggle = { alarm, enabled -> vm.toggleAlarm(alarm, enabled) },
            onEdit   = { alarm -> openEditDialog(alarm) },
            onDelete = { alarm ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("刪除鬧鐘")
                    .setMessage("確定要刪除「${alarm.label.ifBlank { formatTime(alarm.hour, alarm.minute) }}」嗎？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("刪除") { _, _ -> vm.deleteAlarm(alarm) }
                    .show()
            }
        )
        binding.rvAlarms.layoutManager = LinearLayoutManager(this)
        binding.rvAlarms.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener { openAddDialog() }
    }

    private fun openAddDialog() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setTitleText("設定鬧鐘時間")
            .build()
        picker.addOnPositiveButtonClickListener {
            openAlarmEditor(null, picker.hour, picker.minute)
        }
        picker.show(supportFragmentManager, "time_picker")
    }

    private fun openEditDialog(alarm: Alarm) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(alarm.hour)
            .setMinute(alarm.minute)
            .setTitleText("修改鬧鐘時間")
            .build()
        picker.addOnPositiveButtonClickListener {
            openAlarmEditor(alarm, picker.hour, picker.minute)
        }
        picker.show(supportFragmentManager, "time_picker_edit")
    }

    private fun openAlarmEditor(existingAlarm: Alarm?, hour: Int, minute: Int) {
        AlarmEditorFragment.newInstance(existingAlarm, hour, minute)
            .show(supportFragmentManager, "alarm_editor")
    }

    private fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    companion object {
        fun formatTime(h: Int, m: Int) = "%02d:%02d".format(h, m)
    }
}
