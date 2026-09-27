package dev.ericferguson.watertracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore by preferencesDataStore(name = "settings")

data class ReminderSettings(
    val enabled: Boolean = false,
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    val bedTime: LocalTime = LocalTime.of(22, 0),
)

/** The amounts logged by the widget's two buttons. */
data class WidgetButtons(val firstOz: Int = 8, val secondOz: Int = 16)

class SettingsRepository(private val context: Context) {
    val dailyGoalOz: Flow<Int> = context.dataStore.data.map { it[DAILY_GOAL_OZ] ?: DEFAULT_GOAL_OZ }

    val reminderSettings: Flow<ReminderSettings> = context.dataStore.data.map { prefs ->
        val defaults = ReminderSettings()
        ReminderSettings(
            enabled = prefs[REMINDERS_ENABLED] ?: defaults.enabled,
            wakeTime = prefs[WAKE_MINUTE]?.let(::timeOfMinute) ?: defaults.wakeTime,
            bedTime = prefs[BED_MINUTE]?.let(::timeOfMinute) ?: defaults.bedTime,
        )
    }

    val widgetButtons: Flow<WidgetButtons> = context.dataStore.data.map { prefs ->
        val defaults = WidgetButtons()
        WidgetButtons(
            firstOz = prefs[WIDGET_FIRST_OZ] ?: defaults.firstOz,
            secondOz = prefs[WIDGET_SECOND_OZ] ?: defaults.secondOz,
        )
    }

    suspend fun setDailyGoalOz(oz: Int) {
        context.dataStore.edit { it[DAILY_GOAL_OZ] = oz }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[REMINDERS_ENABLED] = enabled }
    }

    suspend fun setWakeTime(time: LocalTime) {
        context.dataStore.edit { it[WAKE_MINUTE] = minuteOfDay(time) }
    }

    suspend fun setBedTime(time: LocalTime) {
        context.dataStore.edit { it[BED_MINUTE] = minuteOfDay(time) }
    }

    suspend fun setWidgetFirstOz(oz: Int) {
        context.dataStore.edit { it[WIDGET_FIRST_OZ] = oz }
    }

    suspend fun setWidgetSecondOz(oz: Int) {
        context.dataStore.edit { it[WIDGET_SECOND_OZ] = oz }
    }

    companion object {
        const val DEFAULT_GOAL_OZ = 64
        private val DAILY_GOAL_OZ = intPreferencesKey("daily_goal_oz")
        private val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        // Times are stored as minutes after midnight.
        private val WAKE_MINUTE = intPreferencesKey("wake_minute")
        private val BED_MINUTE = intPreferencesKey("bed_minute")
        private val WIDGET_FIRST_OZ = intPreferencesKey("widget_first_oz")
        private val WIDGET_SECOND_OZ = intPreferencesKey("widget_second_oz")
    }
}

private fun minuteOfDay(time: LocalTime) = time.hour * 60 + time.minute

private fun timeOfMinute(minute: Int): LocalTime = LocalTime.of(minute / 60, minute % 60)
