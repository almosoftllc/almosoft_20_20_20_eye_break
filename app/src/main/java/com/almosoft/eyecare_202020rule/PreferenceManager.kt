package com.almosoft.eyecare_202020rule

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(val context: Context) {
    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val TIMER_INTERVAL = intPreferencesKey("timer_interval")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTOMATIC_WORK_HOURS = booleanPreferencesKey("automatic_work_hours")
        val START_TIME = stringPreferencesKey("start_time")
        val END_TIME = stringPreferencesKey("end_time")
        val NOTIFICATION_TONE_ENABLED = booleanPreferencesKey("notification_tone_enabled")
        val VOICE_NOTIFICATION_ENABLED = booleanPreferencesKey("voice_notification_enabled")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }
    val timerInterval: Flow<Int> = context.dataStore.data.map { it[TIMER_INTERVAL] ?: 20 }
    val isVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[VIBRATION_ENABLED] ?: true }
    val isDarkModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE_ENABLED] ?: false }
    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val isAutomaticWorkHours: Flow<Boolean> = context.dataStore.data.map { it[AUTOMATIC_WORK_HOURS] ?: false }
    val startTime: Flow<String> = context.dataStore.data.map { it[START_TIME] ?: "09:00" }
    val endTime: Flow<String> = context.dataStore.data.map { it[END_TIME] ?: "17:00" }
    val isNotificationToneEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATION_TONE_ENABLED] ?: true }
    val isVoiceNotificationEnabled: Flow<Boolean> = context.dataStore.data.map { it[VOICE_NOTIFICATION_ENABLED] ?: true }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setTimerInterval(minutes: Int) {
        context.dataStore.edit { it[TIMER_INTERVAL] = minutes }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE_ENABLED] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAutomaticWorkHours(enabled: Boolean) {
        context.dataStore.edit { it[AUTOMATIC_WORK_HOURS] = enabled }
    }

    suspend fun setStartTime(time: String) {
        context.dataStore.edit { it[START_TIME] = time }
    }

    suspend fun setEndTime(time: String) {
        context.dataStore.edit { it[END_TIME] = time }
    }

    suspend fun setNotificationToneEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATION_TONE_ENABLED] = enabled }
    }

    suspend fun setVoiceNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VOICE_NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun resetSettings() {
        context.dataStore.edit {
            it.remove(TIMER_INTERVAL)
            it.remove(VIBRATION_ENABLED)
            it.remove(DARK_MODE_ENABLED)
            it.remove(NOTIFICATIONS_ENABLED)
            it.remove(AUTOMATIC_WORK_HOURS)
            it.remove(START_TIME)
            it.remove(END_TIME)
            it.remove(NOTIFICATION_TONE_ENABLED)
            it.remove(VOICE_NOTIFICATION_ENABLED)
        }
    }
}
