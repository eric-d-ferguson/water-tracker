package dev.ericferguson.watertracker.ui

import dev.ericferguson.watertracker.data.Drink
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayUiStateTest {
    private fun drinks(vararg oz: Int) = oz.mapIndexed { i, amount ->
        Drink(id = i.toLong(), amountOz = amount, timestampMillis = 0)
    }

    @Test
    fun totalsAllDrinks() {
        assertEquals(36, TodayUiState(drinks(8, 12, 16), goalOz = 64).totalOz)
    }

    @Test
    fun progressIsFractionOfGoal() {
        assertEquals(0.5f, TodayUiState(drinks(16, 16), goalOz = 64).progress, 0.0001f)
    }

    @Test
    fun progressCanExceedGoal() {
        assertEquals(1.25f, TodayUiState(drinks(40, 40), goalOz = 64).progress, 0.0001f)
    }

    @Test
    fun zeroGoalDoesNotDivideByZero() {
        assertEquals(0f, TodayUiState(drinks(8), goalOz = 0).progress)
    }
}
