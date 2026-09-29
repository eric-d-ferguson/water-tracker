package dev.ericferguson.watertracker.ui

import dev.ericferguson.watertracker.data.DailyTotal
import dev.ericferguson.watertracker.data.VolumeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HistoryUiStateTest {
    private val today = LocalDate.of(2026, 9, 23)

    /** Totals are oldest first; the last one is today. */
    private fun state(vararg totals: Int, goalMl: Int = 64, unit: VolumeUnit = VolumeUnit.ML) = HistoryUiState(
        days = totals.mapIndexed { i, ml -> DailyTotal(today.minusDays((totals.size - 1 - i).toLong()), ml) },
        goalMl = goalMl,
        unit = unit,
    )

    @Test
    fun averageLeavesOutTodayAndEmptyDays() {
        // (60 + 70) / 2; the 0 day and today's 8 are ignored.
        assertEquals(65, state(0, 60, 70, 8).averageMl)
    }

    @Test
    fun averageIsNullWithNoFinishedDays() {
        assertNull(state(0, 0, 32).averageMl)
    }

    @Test
    fun countsDaysAtOrOverGoalIncludingToday() {
        assertEquals(3, state(64, 63, 80, 0, 64).daysGoalMet)
    }

    @Test
    fun aDayAtExactlyTheGoalInOzCountsAsMet() {
        // 5 x 16 oz = 2365 ml against an 80 oz goal of 2366 ml.
        val fiveSixteens = 5 * VolumeUnit.OZ.toMl(16)
        assertEquals(1, state(fiveSixteens, goalMl = VolumeUnit.OZ.toMl(80), unit = VolumeUnit.OZ).daysGoalMet)
    }
}
