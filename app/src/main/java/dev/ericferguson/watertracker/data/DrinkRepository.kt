package dev.ericferguson.watertracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

class DrinkRepository(
    private val dao: DrinkDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun drinksOn(date: LocalDate): Flow<List<Drink>> {
        val range = DayRange.of(date, clock.zone)
        return dao.observeBetween(range.startMillis, range.endMillis)
    }

    /** One total per day from [from] to [to] inclusive, oldest first. */
    fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<DailyTotal>> {
        val days = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
        val start = DayRange.of(from, clock.zone).startMillis
        val end = DayRange.of(to, clock.zone).endMillis
        return dao.observeBetween(start, end).map { dailyTotals(it, days, clock.zone) }
    }

    /** Returns the new row's id so the caller can undo it. */
    suspend fun add(amountOz: Int): Long =
        dao.insert(Drink(amountOz = amountOz, timestampMillis = clock.millis()))

    suspend fun remove(id: Long) = dao.deleteById(id)
}
