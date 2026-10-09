package dev.ericferguson.watertracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.ericferguson.watertracker.WaterTrackerApp
import dev.ericferguson.watertracker.data.Drink
import dev.ericferguson.watertracker.data.DrinkRepository
import dev.ericferguson.watertracker.data.SettingsRepository
import dev.ericferguson.watertracker.data.VolumeUnit
import dev.ericferguson.watertracker.reminders.Pace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** Amounts are in ml; [unit] says how to show them. */
data class TodayUiState(
    val drinks: List<Drink> = emptyList(),
    val goalMl: Int = VolumeUnit.OZ.defaultGoalMl,
    val unit: VolumeUnit = VolumeUnit.OZ,
    /** Where the pace says you should be by now (see [Pace.target]). */
    val paceMl: Int = 0,
) {
    val totalMl: Int get() = drinks.sumOf { it.amountMl }
    val goalMet: Boolean get() = unit.isGoalMet(totalMl, goalMl)
    /** Can pass 1 once over the goal; never below 1 once the goal counts as met. */
    val progress: Float get() {
        val raw = if (goalMl > 0) totalMl.toFloat() / goalMl else 0f
        return if (goalMet) maxOf(raw, 1f) else raw
    }

    /** The pace as a fraction of the goal, from 0 to 1. */
    val paceProgress: Float get() = if (goalMl > 0) (paceMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f

    /** True if adding [ml] takes the total from under the goal to at or over it. */
    fun reachesGoalWith(ml: Int): Boolean = !goalMet && unit.isGoalMet(totalMl + ml, goalMl)
}

class TodayViewModel(
    private val drinkRepository: DrinkRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    // flatMapLatest swaps to the new day's query whenever `today` changes.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = combine(
        today.flatMapLatest { drinkRepository.drinksOn(it) },
        settingsRepository.dailyGoalMl,
        settingsRepository.unit,
        settingsRepository.reminderSettings,
        minuteTicks(),
    ) { drinks, goalMl, unit, day, now ->
        TodayUiState(drinks, goalMl, unit, paceMl = Pace.target(now, day.wakeTime, day.bedTime, goalMl))
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Called whenever the screen resumes, so the total resets after midnight. */
    fun refreshDate() {
        today.value = LocalDate.now()
    }

    /** Suspends until saved and returns the id, so the UI can offer Undo. */
    suspend fun addDrink(amountMl: Int): Long = drinkRepository.add(amountMl)

    fun removeDrink(id: Long) {
        viewModelScope.launch { drinkRepository.remove(id) }
    }

    /** The current time, once now and then at the start of every minute, so the pace keeps moving. */
    private fun minuteTicks(): Flow<LocalTime> = flow {
        while (true) {
            val now = LocalTime.now()
            emit(now)
            delay(60_000L - now.second * 1_000L - now.nano / 1_000_000)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WaterTrackerApp
                TodayViewModel(app.drinkRepository, app.settingsRepository)
            }
        }
    }
}
