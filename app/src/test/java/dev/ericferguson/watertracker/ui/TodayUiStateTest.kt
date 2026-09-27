package dev.ericferguson.watertracker.ui

import dev.ericferguson.watertracker.data.Drink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayUiStateTest {
    private fun drinks(vararg oz: Int) = oz.mapIndexed { i, amount ->
        Drink(id = i.toLong(), amountMl = amount, timestampMillis = 0)
    }

    @Test
    fun totalsAllDrinks() {
        assertEquals(36, TodayUiState(drinks(8, 12, 16), goalMl = 64).totalMl)
    }

    @Test
    fun progressIsFractionOfGoal() {
        assertEquals(0.5f, TodayUiState(drinks(16, 16), goalMl = 64).progress, 0.0001f)
    }

    @Test
    fun progressCanExceedGoal() {
        assertEquals(1.25f, TodayUiState(drinks(40, 40), goalMl = 64).progress, 0.0001f)
    }

    @Test
    fun zeroGoalDoesNotDivideByZero() {
        assertEquals(0f, TodayUiState(drinks(8), goalMl = 0).progress)
    }

    @Test
    fun reachesGoalWhenADrinkCrossesIt() {
        assertTrue(TodayUiState(drinks(40, 16), goalMl = 64).reachesGoalWith(8))
        assertTrue(TodayUiState(drinks(60), goalMl = 64).reachesGoalWith(20))
    }

    @Test
    fun doesNotReachGoalWhenStillShort() {
        assertFalse(TodayUiState(drinks(40), goalMl = 64).reachesGoalWith(8))
    }

    @Test
    fun doesNotCelebrateAgainOnceGoalIsMet() {
        assertFalse(TodayUiState(drinks(64), goalMl = 64).reachesGoalWith(8))
    }
}
