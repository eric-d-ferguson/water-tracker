package dev.ericferguson.watertracker.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyTotal(val date: LocalDate, val totalMl: Int)

/**
 * Sums [drinks] by local calendar day. Returns one entry per date in [days], in the same order,
 * with 0 for days that have no drinks.
 */
fun dailyTotals(drinks: List<Drink>, days: List<LocalDate>, zone: ZoneId): List<DailyTotal> {
    val mlByDate = drinks
        .groupingBy { Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() }
        .fold(0) { sum, drink -> sum + drink.amountMl }
    return days.map { DailyTotal(it, mlByDate[it] ?: 0) }
}
