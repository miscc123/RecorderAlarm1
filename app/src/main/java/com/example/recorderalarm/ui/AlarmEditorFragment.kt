package com.example.recorderalarm.ui

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.recorderalarm.data.Alarm
import com.example.recorderalarm.databinding.FragmentAlarmEditorBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AlarmEditorFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAlarmEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var vm: AlarmViewModel

    private var hour = 8
    private var minute = 0
    private var existingAlarm: Alarm? = null

    // Recording state
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var isRecording = false
    private var isPlaying = false
    private var currentRecordingPath = ""
    private var currentRecordingName = ""

    companion object {
        fun newInstance(alarm: Alarm?, hour: Int, minute: Int) =
            AlarmEditorFragment().apply {
                arguments = Bundle().apply {
                    alarm?.let {
                        putInt("id", it.id)
                        putString("label", it.label)
                        putString("repeatDays", it.repeatDays)
                        putString("recordingPath", it.recordingPath)
                        putString("recordingName", it.recordingName)
                        putBoolean("isEnabled", it.isEnabled)
                    }
                    putInt("hour", hour)
                    putInt("minute", minute)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(
            requireActivity(), AlarmViewModelFactory(requireActivity().application)
        )[AlarmViewModel::class.java]

        // Restore args
        arguments?.let { args ->
            hour = args.getInt("hour", 8)
            minute = args.getInt("minute", 0)
            binding.etLabel.setText(args.getString("label", ""))
            currentRecordingPath = args.getString("recordingPath", "")
            currentRecordingName = args.getString("recordingName", "")

            val dayStr = args.getString("repeatDays", "")
            val selectedDays = if (dayStr.isBlank()) emptySet()
                               else dayStr.split(",").map { it.trim().toInt() }.toSet()

            // Check existing alarm
            val id = args.getInt("id", 0)
            if (id != 0) {
                existingAlarm = Alarm(
                    id = id,
                    hour = hour, minute = minute,
                    label = args.getString("label", ""),
                    repeatDays = dayStr,
                    recordingPath = currentRecordingPath,
                    recordingName = currentRecordingName,
                    isEnabled = args.getBoolean("isEnabled", true)
                )
            }

            // Set day chips
            setChipsFromDays(selectedDays)
        }

        binding.tvEditorTime.text = "%02d:%02d".format(hour, minute)
        updateRecordingUI()

        binding.btnRecord.setOnClickListener { toggleRecording() }
        binding.btnPlayback.setOnClickListener { togglePlayback() }
        binding.btnClearRecording.setOnClickListener { clearRecording() }
        binding.btnSave.setOnClickListener { saveAlarm() }
    }

    // ─── Chip helper ───────────────────────────────────────────────────────────

    private fun getSelectedDays(): Set<Int> {
        val result = mutableSetOf<Int>()
        mapOf(
            binding.chipSun to Calendar.SUNDAY,
            binding.chipMon to Calendar.MONDAY,
            binding.chipTue to Calendar.TUESDAY,
            binding.chipWed to Calendar.WEDNESDAY,
            binding.chipThu to Calendar.THURSDAY,
            binding.chipFri to Calendar.FRIDAY,
            binding.chipSat to Calendar.SATURDAY
        ).forEach { (chip, day) -> if (chip.isChecked) result.add(day) }
        return result
    }

    private fun setChipsFromDays(days: Set<Int>) {
        mapOf(
            binding.chipSun to Calendar.SUNDAY,
            binding.chipMon to Calendar.MONDAY,
            binding.chipTue to Calendar.TUESDAY,
            binding.chipWed to Calendar.WEDNESDAY,
            binding.chipThu to Calendar.THURSDAY,
            binding.chipFri to Calendar.FRIDAY,
            binding.chipSat to Calendar.SATURDAY
        ).forEach { (chip, day) -> chip.isChecked = days.contains(day) }
    }

    // ─── Recording ─────────────────────────────────────────────────────────────

    private fun toggleRecording() {
        if (isRecording) stopRecording() else startRecording()
    }

    private fun startRecording() {
        stopPlayback()
        val dir = requireContext().getExternalFilesDir(null) ?: requireContext().filesDir
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "alarm_$ts.m4a")

        recorder = MediaRecorder(requireContext()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                currentRecordingPath = file.absolutePath
                currentRecordingName = "錄音 $ts"
                binding.btnRecord.text = "⏹ 停止錄音"
                binding.btnRecord.setBackgroundColor(0xFFE53935.toInt())
                binding.tvRecordingStatus.text = "錄音中…"
            } catch (e: Exception) {
                Toast.makeText(context, "錄音失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
        } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        isRecording = false
        binding.btnRecord.text = "🎤 重新錄音"
        binding.btnRecord.setBackgroundColor(0xFF1976D2.toInt())
        updateRecordingUI()
    }

    private fun togglePlayback() {
        if (isPlaying) stopPlayback() else startPlayback()
    }

private fun startPlayback() {
    if (currentRecordingPath.isBlank() || !File(currentRecordingPath).exists()) {
        Toast.makeText(context, "沒有錄音可以播放", Toast.LENGTH_SHORT).show()
        return
    }
    val mp = MediaPlayer()
    try {
        mp.setDataSource(currentRecordingPath)
        mp.prepare()
        mp.start()
        mp.setOnCompletionListener {
            isPlaying = false
            binding.btnPlayback.text = "▶ 試聽錄音"
        }
        player = mp
        isPlaying = true
        binding.btnPlayback.text = "⏹ 停止播放"
    } catch (e: Exception) {
        mp.release()
        Toast.makeText(context, "播放失敗", Toast.LENGTH_SHORT).show()
    }
}

    private fun stopPlayback() {
        player?.stop()
        player?.release()
        player = null
        isPlaying = false
        binding.btnPlayback.text = "▶ 試聽錄音"
    }

    private fun clearRecording() {
        stopPlayback()
        stopRecording()
        currentRecordingPath = ""
        currentRecordingName = ""
        updateRecordingUI()
    }

    private fun updateRecordingUI() {
        val hasRec = currentRecordingPath.isNotBlank() && File(currentRecordingPath).exists()
        binding.tvRecordingStatus.text = if (hasRec) "✅ 已選擇：$currentRecordingName" else "尚未錄音（將使用預設鈴聲）"
        binding.btnPlayback.visibility = if (hasRec) View.VISIBLE else View.GONE
        binding.btnClearRecording.visibility = if (hasRec) View.VISIBLE else View.GONE
        if (hasRec) binding.btnRecord.text = "🎤 重新錄音"
        else binding.btnRecord.text = "🎤 開始錄音"
    }

    // ─── Save ──────────────────────────────────────────────────────────────────

    private fun saveAlarm() {
        stopRecording()
        stopPlayback()

        val days = getSelectedDays()
        val alarm = (existingAlarm ?: Alarm(hour = hour, minute = minute)).copy(
            hour = hour,
            minute = minute,
            label = binding.etLabel.text?.toString()?.trim() ?: "",
            repeatDays = days.sorted().joinToString(","),
            recordingPath = currentRecordingPath,
            recordingName = currentRecordingName,
            isEnabled = true
        )

        if (existingAlarm != null) vm.updateAlarm(alarm)
        else vm.addAlarm(alarm)

        Toast.makeText(context, "鬧鐘已儲存 ✓", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    override fun onDestroyView() {
        stopRecording()
        stopPlayback()
        _binding = null
        super.onDestroyView()
    }
}
