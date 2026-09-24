package dev.ericferguson.watertracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class DayRangeTest {
    private val newYork = ZoneId.of("America/New_York")

    private fun hoursIn(range: DayRange) = Duration.ofMillis(range.endMillis - range.startMillis).toHours()

    @Test
    fun normalDayIs24Hours() {
        assertEquals(24, hoursIn(DayRange.of(LocalDate.of(2026, 6, 1), newYork)))
    }

    @Test
    fun springForwardDayIs23Hours() {
        assertEquals(23, hoursIn(DayRange.of(LocalDate.of(2026, 3, 8), newYork)))
    }

    @Test
    fun fallBackDayIs25Hours() {
        assertEquals(25, hoursIn(DayRange.of(LocalDate.of(2026, 11, 1), newYork)))
    }

    @Test
    fun consecutiveDaysShareABoundary() {
        val today = DayRange.of(LocalDate.of(2026, 9, 23), newYork)
        val tomorrow = DayRange.of(LocalDate.of(2026, 9, 24), newYork)
        assertEquals(today.endMillis, tomorrow.startMillis)
    }
}
