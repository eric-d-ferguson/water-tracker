package dev.ericferguson.watertracker.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "settings")

data class ReminderSettings(
    val enabled: Boolean = false,
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    val bedTime: LocalTime = LocalTime.of(22, 0),
)

/** The amounts, in ml, logged by the widget's two buttons. */
data class WidgetButtons(val firstMl: Int, val secondMl: Int)

class SettingsRepository(private val context: Context) {
    val unit: Flow<VolumeUnit> = context.dataStore.data.map(::unitOf)

    val dailyGoalMl: Flow<Int> = context.dataStore.data.map(::goalMlOf)

    val widgetButtons: Flow<WidgetButtons> = context.dataStore.data.map(::widgetButtonsOf)

    val reminderSettings: Flow<ReminderSettings> = context.dataStore.data.map { prefs ->
        val defaults = ReminderSettings()
        ReminderSettings(
            enabled = prefs[REMINDERS_ENABLED] ?: defaults.enabled,
            wakeTime = prefs[WAKE_MINUTE]?.let(::timeOfMinute) ?: defaults.wakeTime,
            bedTime = prefs[BED_MINUTE]?.let(::timeOfMinute) ?: defaults.bedTime,
        )
    }

    /** Switches units, rounding the goal to a tidy amount and resetting the widget buttons. */
    suspend fun setUnit(unit: VolumeUnit) {
        context.dataStore.edit { prefs ->
            val goalMl = goalMlOf(prefs)
            prefs[UNIT] = unit.name
            prefs[DAILY_GOAL_ML] = unit.roundGoal(goalMl)
            prefs[WIDGET_FIRST_ML] = unit.defaultWidgetButtonsMl.firstMl
            prefs[WIDGET_SECOND_ML] = unit.defaultWidgetButtonsMl.secondMl
        }
    }

    suspend fun setDailyGoalMl(ml: Int) {
        context.dataStore.edit { it[DAILY_GOAL_ML] = ml }
    }

    suspend fun setWidgetFirstMl(ml: Int) {
        context.dataStore.edit { it[WIDGET_FIRST_ML] = ml }
    }

    suspend fun setWidgetSecondMl(ml: Int) {
        context.dataStore.edit { it[WIDGET_SECOND_ML] = ml }
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

    // Before ml support, amounts were saved in oz under the LEGACY_ keys. They're read as a
    // fallback, so upgrading keeps your settings; the first change saves them in ml.

    private fun unitOf(prefs: Preferences): VolumeUnit =
        prefs[UNIT]?.let { name -> VolumeUnit.entries.firstOrNull { it.name == name } }
            ?: if (prefs[LEGACY_GOAL_OZ] != null) VolumeUnit.OZ else VolumeUnit.defaultFor(Locale.getDefault())

    private fun goalMlOf(prefs: Preferences): Int =
        prefs[DAILY_GOAL_ML]
            ?: prefs[LEGACY_GOAL_OZ]?.let(VolumeUnit.OZ::toMl)
            ?: unitOf(prefs).defaultGoalMl

    private fun widgetButtonsOf(prefs: Preferences): WidgetButtons {
        val defaults = unitOf(prefs).defaultWidgetButtonsMl
        return WidgetButtons(
            firstMl = prefs[WIDGET_FIRST_ML] ?: prefs[LEGACY_WIDGET_FIRST_OZ]?.let(VolumeUnit.OZ::toMl) ?: defaults.firstMl,
            secondMl = prefs[WIDGET_SECOND_ML] ?: prefs[LEGACY_WIDGET_SECOND_OZ]?.let(VolumeUnit.OZ::toMl) ?: defaults.secondMl,
        )
    }

    private companion object {
        val UNIT = stringPreferencesKey("unit")
        val DAILY_GOAL_ML = intPreferencesKey("daily_goal_ml")
        val WIDGET_FIRST_ML = intPreferencesKey("widget_first_ml")
        val WIDGET_SECOND_ML = intPreferencesKey("widget_second_ml")
        val LEGACY_GOAL_OZ = intPreferencesKey("daily_goal_oz")
        val LEGACY_WIDGET_FIRST_OZ = intPreferencesKey("widget_first_oz")
        val LEGACY_WIDGET_SECOND_OZ = intPreferencesKey("widget_second_oz")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        // Times are stored as minutes after midnight.
        val WAKE_MINUTE = intPreferencesKey("wake_minute")
        val BED_MINUTE = intPreferencesKey("bed_minute")
    }
}

private fun minuteOfDay(time: LocalTime) = time.hour * 60 + time.minute

private fun timeOfMinute(minute: Int): LocalTime = LocalTime.of(minute / 60, minute % 60)
