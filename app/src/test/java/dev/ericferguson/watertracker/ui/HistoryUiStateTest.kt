package dev.ericferguson.watertracker.ui

import dev.ericferguson.watertracker.data.DailyTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HistoryUiStateTest {
    private val today = LocalDate.of(2026, 9, 23)

    /** Totals are oldest first; the last one is today. */
    private fun state(vararg totals: Int, goalOz: Int = 64) = HistoryUiState(
        days = totals.mapIndexed { i, oz -> DailyTotal(today.minusDays((totals.size - 1 - i).toLong()), oz) },
        goalOz = goalOz,
    )

    @Test
    fun averageLeavesOutTodayAndEmptyDays() {
        // (60 + 70) / 2; the 0 day and today's 8 are ignored.
        assertEquals(65, state(0, 60, 70, 8).averageOz)
    }

    @Test
    fun averageIsNullWithNoFinishedDays() {
        assertNull(state(0, 0, 32).averageOz)
    }

    @Test
    fun countsDaysAtOrOverGoalIncludingToday() {
        assertEquals(3, state(64, 63, 80, 0, 64).daysGoalMet)
    }
}
