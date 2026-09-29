package dev.ericferguson.watertracker.ui

import dev.ericferguson.watertracker.data.Drink
import dev.ericferguson.watertracker.data.VolumeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayUiStateTest {
    private fun drinks(vararg ml: Int) = ml.mapIndexed { i, amount ->
        Drink(id = i.toLong(), amountMl = amount, timestampMillis = 0)
    }

    private fun ozDrinks(vararg oz: Int) = drinks(*oz.map(VolumeUnit.OZ::toMl).toIntArray())

    @Test
    fun totalsAllDrinks() {
        assertEquals(36, TodayUiState(drinks(8, 12, 16), goalMl = 64, unit = VolumeUnit.ML).totalMl)
    }

    @Test
    fun progressIsFractionOfGoal() {
        assertEquals(0.5f, TodayUiState(drinks(16, 16), goalMl = 64, unit = VolumeUnit.ML).progress, 0.0001f)
    }

    @Test
    fun progressCanExceedGoal() {
        assertEquals(1.25f, TodayUiState(drinks(40, 40), goalMl = 64, unit = VolumeUnit.ML).progress, 0.0001f)
    }

    @Test
    fun zeroGoalDoesNotDivideByZero() {
        // Not reachable from the UI (the minimum goal is 1), but it mustn't crash; it counts as met.
        assertEquals(1f, TodayUiState(drinks(8), goalMl = 0, unit = VolumeUnit.ML).progress)
    }

    @Test
    fun reachesGoalWhenADrinkCrossesIt() {
        assertTrue(TodayUiState(drinks(40, 16), goalMl = 64, unit = VolumeUnit.ML).reachesGoalWith(8))
        assertTrue(TodayUiState(drinks(60), goalMl = 64, unit = VolumeUnit.ML).reachesGoalWith(20))
    }

    @Test
    fun doesNotReachGoalWhenStillShort() {
        assertFalse(TodayUiState(drinks(40), goalMl = 64, unit = VolumeUnit.ML).reachesGoalWith(8))
    }

    @Test
    fun doesNotCelebrateAgainOnceGoalIsMet() {
        assertFalse(TodayUiState(drinks(64), goalMl = 64, unit = VolumeUnit.ML).reachesGoalWith(8))
    }

    // Regression: each oz drink is rounded to whole ml, so five 16 oz drinks are 2365 ml while an
    // 80 oz goal is 2366 ml. It must still count as reaching the goal, as the screen shows 80 of 80 oz.
    private val eightyOz = VolumeUnit.OZ.toMl(80)

    @Test
    fun exactlyTheGoalInOzCountsAsMet() {
        val state = TodayUiState(ozDrinks(16, 16, 16, 16, 16), goalMl = eightyOz, unit = VolumeUnit.OZ)
        assertTrue(state.goalMet)
        assertEquals(1f, state.progress)
    }

    @Test
    fun theDrinkThatHitsTheGoalExactlyInOzCelebrates() {
        val state = TodayUiState(ozDrinks(20, 20, 20), goalMl = eightyOz, unit = VolumeUnit.OZ)
        assertTrue(state.reachesGoalWith(VolumeUnit.OZ.toMl(20)))
    }

    @Test
    fun oneOzShortIsNotMet() {
        val state = TodayUiState(ozDrinks(16, 16, 16, 16, 15), goalMl = eightyOz, unit = VolumeUnit.OZ)
        assertFalse(state.goalMet)
    }
}
