package com.almosoft.eyecare_202020rule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed class TimerEvent {
    object BreakStarted : TimerEvent()
    object WorkStarted : TimerEvent()
}

class MainViewModel(private val preferenceManager: PreferenceManager) : ViewModel() {
    private val _timerSeconds = MutableStateFlow(20 * 60)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isBreak = MutableStateFlow(false)
    val isBreak: StateFlow<Boolean> = _isBreak.asStateFlow()

    private val _timerEvents = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 1)
    val timerEvents: SharedFlow<TimerEvent> = _timerEvents.asSharedFlow()

    private var timerJob: Job? = null
    private var isManuallyPaused = false
    
    val timerInterval = preferenceManager.timerInterval
    val isVibrationEnabled = preferenceManager.isVibrationEnabled
    val isDarkModeEnabled = preferenceManager.isDarkModeEnabled
    val isAutomaticWorkHours = preferenceManager.isAutomaticWorkHours
    val startTime = preferenceManager.startTime
    val endTime = preferenceManager.endTime
    val isNotificationToneEnabled = preferenceManager.isNotificationToneEnabled
    val isVoiceNotificationEnabled = preferenceManager.isVoiceNotificationEnabled

    init {
        viewModelScope.launch {
            val interval = timerInterval.first()
            _timerSeconds.value = interval * 60
            
            // If automatic is enabled, ensure background reminders are scheduled
            if (isAutomaticWorkHours.first()) {
                scheduleBackgroundReminders()
            }
        }
        monitorWorkHours()
    }

    private fun monitorWorkHours() {
        var lastInWindow: Boolean? = null
        viewModelScope.launch {
            while (true) {
                val automatic = isAutomaticWorkHours.first()
                if (automatic) {
                    val start = startTime.first()
                    val end = endTime.first()
                    val inWindow = isCurrentTimeInWindow(start, end)
                    
                    if (lastInWindow != null && inWindow != lastInWindow) {
                        // Transition occurred between work/off hours
                        if (inWindow) {
                            // Entered work hours -> Auto-start if not manually paused
                            if (!_isTimerRunning.value && !isManuallyPaused) {
                                startTimerInternal()
                            }
                        } else {
                            // Exited work hours -> Auto-pause
                            if (_isTimerRunning.value) {
                                pauseTimerInternal()
                            }
                            // Reset manual pause state for the next session
                            isManuallyPaused = false
                        }
                    } else if (lastInWindow == null) {
                        // Initial check on app start
                        if (inWindow && !_isTimerRunning.value && !isManuallyPaused) {
                            startTimerInternal()
                        }
                    }
                    lastInWindow = inWindow
                } else {
                    lastInWindow = null
                }
                delay(5000)
            }
        }
    }

    private fun isCurrentTimeInWindow(start: String, end: String): Boolean {
        return try {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            
            val startParts = start.split(":").map { it.toInt() }
            val startMinutes = startParts[0] * 60 + startParts[1]
            
            val endParts = end.split(":").map { it.toInt() }
            val endMinutes = endParts[0] * 60 + endParts[1]
            
            if (endMinutes < startMinutes) {
                // Window spans across midnight
                currentMinutes >= startMinutes || currentMinutes < endMinutes
            } else {
                currentMinutes >= startMinutes && currentMinutes < endMinutes
            }
        } catch (e: Exception) {
            true // Default to in-window if parsing fails
        }
    }

    fun startTimer() {
        isManuallyPaused = false
        startTimerInternal()
    }

    private fun startTimerInternal() {
        _isTimerRunning.value = true
        runTimer()
        scheduleBackgroundReminders()
    }

    private fun scheduleBackgroundReminders() {
        viewModelScope.launch {
            val interval = timerInterval.first().toLong()
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                interval, java.util.concurrent.TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(preferenceManager.context).enqueueUniquePeriodicWork(
                "EyeCareReminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                if (_timerSeconds.value > 0) {
                    delay(1000)
                    _timerSeconds.value -= 1
                }
                
                if (_timerSeconds.value == 0) {
                    if (!_isBreak.value) {
                        // Work session finished -> Start 20s break
                        _isBreak.value = true
                        _timerSeconds.value = 20
                        _timerEvents.tryEmit(TimerEvent.BreakStarted)
                    } else {
                        // Break finished -> Restart work session
                        _isBreak.value = false
                        val interval = timerInterval.first()
                        _timerSeconds.value = interval * 60
                        _timerEvents.tryEmit(TimerEvent.WorkStarted)
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        isManuallyPaused = true
        pauseTimerInternal()
    }

    private fun pauseTimerInternal() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        cancelBackgroundReminders()
    }

    private fun cancelBackgroundReminders() {
        WorkManager.getInstance(preferenceManager.context).cancelUniqueWork("EyeCareReminder")
    }

    fun resumeTimer() {
        isManuallyPaused = false
        startTimer()
    }

    fun resetTimer() {
        pauseTimer()
        viewModelScope.launch {
            val interval = timerInterval.first()
            _timerSeconds.value = interval * 60
            _isBreak.value = false
        }
    }

    fun setTimerInterval(minutes: Int) {
        viewModelScope.launch {
            preferenceManager.setTimerInterval(minutes)
            if (!_isTimerRunning.value && !_isBreak.value) {
                _timerSeconds.value = minutes * 60
            }
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setVibrationEnabled(enabled) }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setDarkModeEnabled(enabled) }
    }

    fun setAutomaticWorkHours(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setAutomaticWorkHours(enabled) }
    }

    fun setStartTime(time: String) {
        viewModelScope.launch { preferenceManager.setStartTime(time) }
    }

    fun setEndTime(time: String) {
        viewModelScope.launch { preferenceManager.setEndTime(time) }
    }

    fun setNotificationToneEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setNotificationToneEnabled(enabled) }
    }

    fun setVoiceNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setVoiceNotificationEnabled(enabled) }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch { preferenceManager.setOnboardingCompleted(true) }
    }
    
    fun resetAllSettings() {
        viewModelScope.launch {
            preferenceManager.resetSettings()
            resetTimer()
        }
    }
}
