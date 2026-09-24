package dev.ericferguson.watertracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.ericferguson.watertracker.WaterTrackerApp
import dev.ericferguson.watertracker.data.ReminderSettings
import dev.ericferguson.watertracker.data.SettingsRepository
import dev.ericferguson.watertracker.reminders.ReminderManager
import dev.ericferguson.watertracker.reminders.ReminderSchedule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class SettingsUiState(
    val goalOz: Int = SettingsRepository.DEFAULT_GOAL_OZ,
    val reminders: ReminderSettings = ReminderSettings(),
) {
    val checkTimes: List<LocalTime>
        get() = ReminderSchedule.checkTimes(reminders.wakeTime, reminders.bedTime)
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val reminderManager: ReminderManager,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.dailyGoalOz,
        settingsRepository.reminderSettings,
        ::SettingsUiState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setGoal(oz: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoalOz(oz) }
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WaterTrackerApp
                SettingsViewModel(app.settingsRepository, app.reminderManager)
            }
        }
    }
}
