package dev.ericferguson.watertracker.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.ericferguson.watertracker.WaterTrackerApp
import dev.ericferguson.watertracker.data.DrinkBackup
import dev.ericferguson.watertracker.data.ReminderSettings
import dev.ericferguson.watertracker.data.SettingsRepository
import dev.ericferguson.watertracker.data.VolumeUnit
import dev.ericferguson.watertracker.data.WidgetButtons
import dev.ericferguson.watertracker.reminders.ReminderManager
import dev.ericferguson.watertracker.reminders.ReminderSchedule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Amounts are in ml; [unit] says how to show and enter them. */
data class SettingsUiState(
    val unit: VolumeUnit = VolumeUnit.OZ,
    val goalMl: Int = VolumeUnit.OZ.defaultGoalMl,
    val reminders: ReminderSettings = ReminderSettings(),
    val widgetButtons: WidgetButtons = VolumeUnit.OZ.defaultWidgetButtonsMl,
) {
    val checkTimes: List<LocalTime>
        get() = ReminderSchedule.checkTimes(reminders.wakeTime, reminders.bedTime)
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val reminderManager: ReminderManager,
    private val backup: DrinkBackup,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.unit,
        settingsRepository.dailyGoalMl,
        settingsRepository.reminderSettings,
        settingsRepository.widgetButtons,
        ::SettingsUiState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setUnit(unit: VolumeUnit) {
        viewModelScope.launch { settingsRepository.setUnit(unit) }
    }

    fun setGoalMl(ml: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoalMl(ml) }
    }

    fun setWidgetFirstMl(ml: Int) {
        viewModelScope.launch { settingsRepository.setWidgetFirstMl(ml) }
    }

    fun setWidgetSecondMl(ml: Int) {
        viewModelScope.launch { settingsRepository.setWidgetSecondMl(ml) }
    }

    fun setRemindersEnabled(enabled: Boolean) = updateReminders { settingsRepository.setRemindersEnabled(enabled) }

    fun setWakeTime(time: LocalTime) = updateReminders { settingsRepository.setWakeTime(time) }

    fun setBedTime(time: LocalTime) = updateReminders { settingsRepository.setBedTime(time) }

    /** Saves a reminder setting, then moves the pending alarm to match. */
    private fun updateReminders(save: suspend () -> Unit) {
        viewModelScope.launch {
            save()
            reminderManager.reschedule()
        }
    }

    /** Writes every drink to [uri] and returns a message for the user. */
    suspend fun exportTo(uri: Uri): String = userMessage("Export failed") {
        val count = backup.export(uri)
        "Exported $count ${drinksWord(count)}."
    }

    /** Merges drinks from [uri] and returns a message for the user. */
    suspend fun importFrom(uri: Uri): String = userMessage("Import failed") {
        val result = backup.import(uri)
        buildString {
            append("Imported ${result.added} ${drinksWord(result.added)}.")
            if (result.duplicates > 0) append(" Skipped ${result.duplicates} already in the app.")
            if (result.invalidRows > 0) append(" ${result.invalidRows} unreadable ${if (result.invalidRows == 1) "row" else "rows"}.")
        }
    }

    private suspend fun userMessage(failure: String, block: suspend () -> String): String = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        "$failure: ${e.message ?: e.javaClass.simpleName}"
    }

    private fun drinksWord(count: Int) = if (count == 1) "drink" else "drinks"

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WaterTrackerApp
                SettingsViewModel(app.settingsRepository, app.reminderManager, app.drinkBackup)
            }
        }
    }
}
