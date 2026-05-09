package com.example.recorderalarm.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recorderalarm.data.Alarm
import com.example.recorderalarm.databinding.ItemAlarmBinding
import java.io.File

class AlarmAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onEdit: (Alarm) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Alarm>() {
            override fun areItemsTheSame(a: Alarm, b: Alarm) = a.id == b.id
            override fun areContentsTheSame(a: Alarm, b: Alarm) = a == b
        }
        private val DAY_NAMES = listOf("日", "一", "二", "三", "四", "五", "六")
    }

    inner class VH(val binding: ItemAlarmBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val alarm = getItem(position)
        with(holder.binding) {
            tvTime.text = "%02d:%02d".format(alarm.hour, alarm.minute)
            tvLabel.text = alarm.label.ifBlank { "鬧鐘" }

            // Repeat days
            val days = if (alarm.repeatDays.isBlank()) emptyList()
                       else alarm.repeatDays.split(",").map { it.trim().toInt() }
            tvRepeat.text = when {
                days.isEmpty() -> "只響一次"
                days.size == 7 -> "每天"
                days.containsAll(listOf(2, 3, 4, 5, 6)) && days.size == 5 -> "週一至五"
                else -> "每週" + days.sorted().joinToString("、") { "週${DAY_NAMES[it % 7]}" }
            }

            // Recording indicator
            val hasRec = alarm.recordingPath.isNotBlank() && File(alarm.recordingPath).exists()
            tvRecording.text = if (hasRec) "🎤 ${alarm.recordingName.ifBlank { "自訂錄音" }}" else "🔔 預設鈴聲"

            switchEnabled.isChecked = alarm.isEnabled
            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.setOnCheckedChangeListener { _, checked ->
                onToggle(alarm, checked)
            }

            root.setOnClickListener { onEdit(alarm) }
            btnDelete.setOnClickListener { onDelete(alarm) }
        }
    }
}
