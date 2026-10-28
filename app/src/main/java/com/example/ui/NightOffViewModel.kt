package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NightOffApplication
import com.example.model.ExecutionLog
import com.example.model.NightSchedule
import com.example.model.SleepAction
import com.example.util.PermissionHelper
import com.example.util.PermissionState
import com.example.util.RootUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NightOffViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as NightOffApplication).repository

    val allSchedules: StateFlow<List<NightSchedule>> = repository.allSchedules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeQuickTimer: StateFlow<NightSchedule?> = repository.activeQuickTimer
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val executionLogs: StateFlow<List<ExecutionLog>> = repository.allExecutionLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _permissionsState = MutableStateFlow(
        PermissionState(
            hasExactAlarm = true,
            hasNotification = true,
            hasAccessibility = false,
            hasDeviceAdmin = false,
            hasDnd = false,
            hasIgnoreBatteryOpt = false,
            hasBluetooth = false,
            isRealRooted = false,
            isVirtualRootEnabled = true
        )
    )
    val permissionsState: StateFlow<PermissionState> = _permissionsState.asStateFlow()

    private val _selectedPresetMinutes = MutableStateFlow(30)
    val selectedPresetMinutes: StateFlow<Int> = _selectedPresetMinutes.asStateFlow()

    private val _customMinutes = MutableStateFlow(45)
    val customMinutes: StateFlow<Int> = _customMinutes.asStateFlow()

    private val _quickTimerActions = MutableStateFlow(SleepAction.defaultActions())
    val quickTimerActions: StateFlow<Set<SleepAction>> = _quickTimerActions.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    init {
        // Initial silent background check
        refreshPermissions()
    }

    // Every time app opens, permission check is performed in the background thread (Dispatchers.IO)
    fun refreshPermissions() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = PermissionHelper.checkAll(getApplication())
            _permissionsState.value = state
        }
    }

    fun setVirtualRoot(enabled: Boolean) {
        RootUtil.setVirtualRootEnabled(getApplication(), enabled)
        refreshPermissions()
    }

    fun selectPresetMinutes(minutes: Int) {
        _selectedPresetMinutes.value = minutes
    }

    fun setCustomMinutes(minutes: Int) {
        _customMinutes.value = minutes.coerceIn(1, 720)
    }

    fun toggleQuickAction(action: SleepAction) {
        val current = _quickTimerActions.value.toMutableSet()
        if (current.contains(action)) {
            current.remove(action)
        } else {
            current.add(action)
        }
        _quickTimerActions.value = current
    }

    fun startQuickTimer() {
        val minutes = if (_selectedPresetMinutes.value == -1) {
            _customMinutes.value
        } else {
            _selectedPresetMinutes.value
        }
        viewModelScope.launch {
            repository.startQuickTimer(minutes, _quickTimerActions.value)
        }
    }

    fun cancelQuickTimer() {
        viewModelScope.launch {
            repository.cancelQuickTimer()
        }
    }

    fun snoozeTimer(minutes: Int) {
        viewModelScope.launch {
            repository.snoozeActiveTimer(minutes)
        }
    }

    fun saveSchedule(schedule: NightSchedule) {
        viewModelScope.launch {
            repository.saveSchedule(schedule)
        }
    }

    fun toggleSchedule(schedule: NightSchedule, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSchedule(schedule, isEnabled)
        }
    }

    fun deleteSchedule(schedule: NightSchedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun runNow(scheduleId: Long) {
        repository.runNow(scheduleId)
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }
}
