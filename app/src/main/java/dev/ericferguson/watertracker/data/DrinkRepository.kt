package dev.ericferguson.watertracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

class DrinkRepository(
    private val dao: DrinkDao,
    // A function rather than a value so a time zone change is picked up without restarting.
    private val clock: () -> Clock = { Clock.systemDefaultZone() },
) {
    fun drinksOn(date: LocalDate): Flow<List<Drink>> {
        val range = DayRange.of(date, clock().zone)
        return dao.observeBetween(range.startMillis, range.endMillis)
    }

    /** Today's (or any day's) total in ml. */
    suspend fun totalOn(date: LocalDate): Int = drinksOn(date).first().sumOf { it.amountMl }

    /** One total per day from [from] to [to] inclusive, oldest first. */
    fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<DailyTotal>> {
        val zone = clock().zone
        val days = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
        val start = DayRange.of(from, zone).startMillis
        val end = DayRange.of(to, zone).endMillis
        return dao.observeBetween(start, end).map { dailyTotals(it, days, zone) }
    }

    /** Returns the new row's id so the caller can undo it. */
    suspend fun add(amountMl: Int): Long =
        dao.insert(Drink(amountMl = amountMl, timestampMillis = clock().millis()))

    suspend fun remove(id: Long) = dao.deleteById(id)

    suspend fun all(): List<Drink> = dao.getAll()

    /** Adds [incoming] drinks that aren't already stored. Returns how many were added. */
    suspend fun import(incoming: List<Drink>): Int {
        val toAdd = drinksToImport(existing = dao.getAll(), incoming = incoming)
        dao.insertAll(toAdd)
        return toAdd.size
    }
}
