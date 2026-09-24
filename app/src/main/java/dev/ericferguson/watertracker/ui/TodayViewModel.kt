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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val drinks: List<Drink> = emptyList(),
    val goalOz: Int = SettingsRepository.DEFAULT_GOAL_OZ,
) {
    val totalOz: Int get() = drinks.sumOf { it.amountOz }
    val progress: Float get() = if (goalOz > 0) totalOz.toFloat() / goalOz else 0f
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
        settingsRepository.dailyGoalOz,
    ) { drinks, goal -> TodayUiState(drinks, goal) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Called whenever the screen resumes, so the total resets after midnight. */
    fun refreshDate() {
        today.value = LocalDate.now()
    }

    /** Suspends until saved and returns the id, so the UI can offer Undo. */
    suspend fun addDrink(amountOz: Int): Long = drinkRepository.add(amountOz)

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
