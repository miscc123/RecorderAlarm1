package com.example.recorderalarm.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.recorderalarm.data.Alarm
import com.example.recorderalarm.data.AlarmDatabase
import com.example.recorderalarm.data.AlarmRepository
import com.example.recorderalarm.utils.AlarmScheduler
import kotlinx.coroutines.launch

class AlarmViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AlarmRepository(AlarmDatabase.getInstance(app).alarmDao())
    val alarms: LiveData<List<Alarm>> = repo.allAlarms.asLiveData()

    fun addAlarm(alarm: Alarm) = viewModelScope.launch {
        val id = repo.insert(alarm).toInt()
        val saved = alarm.copy(id = id)
        if (saved.isEnabled) AlarmScheduler.schedule(getApplication(), saved)
    }

    fun updateAlarm(alarm: Alarm) = viewModelScope.launch {
        repo.update(alarm)
        AlarmScheduler.cancel(getApplication(), alarm)
        if (alarm.isEnabled) AlarmScheduler.schedule(getApplication(), alarm)
    }

    fun deleteAlarm(alarm: Alarm) = viewModelScope.launch {
        AlarmScheduler.cancel(getApplication(), alarm)
        repo.delete(alarm)
    }

    fun toggleAlarm(alarm: Alarm, enabled: Boolean) = viewModelScope.launch {
        val updated = alarm.copy(isEnabled = enabled)
        repo.update(updated)
        if (enabled) AlarmScheduler.schedule(getApplication(), updated)
        else AlarmScheduler.cancel(getApplication(), updated)
    }
}

class AlarmViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AlarmViewModel(app) as T
    }
}
