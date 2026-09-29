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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Amounts are in ml; [unit] says how to show them. */
data class TodayUiState(
    val drinks: List<Drink> = emptyList(),
    val goalMl: Int = VolumeUnit.OZ.defaultGoalMl,
    val unit: VolumeUnit = VolumeUnit.OZ,
) {
    val totalMl: Int get() = drinks.sumOf { it.amountMl }
    val goalMet: Boolean get() = unit.isGoalMet(totalMl, goalMl)
    /** Can pass 1 once over the goal; never below 1 once the goal counts as met. */
    val progress: Float get() {
        val raw = if (goalMl > 0) totalMl.toFloat() / goalMl else 0f
        return if (goalMet) maxOf(raw, 1f) else raw
    }

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
        ::TodayUiState,
    )
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WaterTrackerApp
                TodayViewModel(app.drinkRepository, app.settingsRepository)
            }
        }
    }
}
