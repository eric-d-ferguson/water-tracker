package dev.ericferguson.watertracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.ericferguson.watertracker.WaterTrackerApp
import dev.ericferguson.watertracker.data.DailyTotal
import dev.ericferguson.watertracker.data.DrinkRepository
import dev.ericferguson.watertracker.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class HistoryPeriod(val days: Int, val label: String) {
    WEEK(7, "7 days"),
    MONTH(30, "30 days"),
}

data class HistoryUiState(
    val period: HistoryPeriod = HistoryPeriod.WEEK,
    /** Oldest first; the last entry is today. */
    val days: List<DailyTotal> = emptyList(),
    val goalOz: Int = SettingsRepository.DEFAULT_GOAL_OZ,
) {
    /**
     * Average of the finished days that have any drinks logged. Today is still in progress,
     * and days before you started using the app would pull the average down, so both are
     * left out. Null when there's nothing to average yet.
     */
    val averageOz: Int?
        get() = days.dropLast(1)
            .filter { it.totalOz > 0 }
            .takeIf { it.isNotEmpty() }
            ?.let { logged -> logged.sumOf { it.totalOz } / logged.size }

    val daysGoalMet: Int get() = days.count { it.totalOz >= goalOz }
}

class HistoryViewModel(
    private val drinkRepository: DrinkRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())
    private val period = MutableStateFlow(HistoryPeriod.WEEK)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = combine(today, period) { day, period -> day to period }
        .flatMapLatest { (day, period) ->
            combine(
                drinkRepository.dailyTotals(from = day.minusDays(period.days - 1L), to = day),
                settingsRepository.dailyGoalOz,
            ) { totals, goal -> HistoryUiState(period, totals, goal) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun refreshDate() {
        today.value = LocalDate.now()
    }

    fun selectPeriod(newPeriod: HistoryPeriod) {
        period.value = newPeriod
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WaterTrackerApp
                HistoryViewModel(app.drinkRepository, app.settingsRepository)
            }
        }
    }
}
