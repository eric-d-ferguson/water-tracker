package dev.ericferguson.watertracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DailyTotalTest {
    private val zone = ZoneId.of("America/New_York")
    private val mon = LocalDate.of(2026, 9, 21)
    private val tue = mon.plusDays(1)
    private val wed = mon.plusDays(2)

    private fun drinkAt(time: LocalDateTime, oz: Int) =
        Drink(amountOz = oz, timestampMillis = time.atZone(zone).toInstant().toEpochMilli())

    @Test
    fun sumsDrinksPerLocalDay() {
        val drinks = listOf(
            drinkAt(mon.atTime(9, 0), 8),
            drinkAt(mon.atTime(23, 59), 12),
            drinkAt(tue.atStartOfDay(), 16),
        )
        assertEquals(
            listOf(DailyTotal(mon, 20), DailyTotal(tue, 16)),
            dailyTotals(drinks, listOf(mon, tue), zone),
        )
    }

    @Test
    fun fillsEmptyDaysWithZero() {
        val drinks = listOf(drinkAt(wed.atTime(12, 0), 20))
        assertEquals(
            listOf(DailyTotal(mon, 0), DailyTotal(tue, 0), DailyTotal(wed, 20)),
            dailyTotals(drinks, listOf(mon, tue, wed), zone),
        )
    }

    @Test
    fun usesTheGivenTimeZone() {
        // 11 PM Monday in New York is already Tuesday in UTC.
        val drinks = listOf(drinkAt(mon.atTime(23, 0), 8))
        assertEquals(listOf(DailyTotal(mon, 0), DailyTotal(tue, 8)), dailyTotals(drinks, listOf(mon, tue), ZoneId.of("UTC")))
    }
}
