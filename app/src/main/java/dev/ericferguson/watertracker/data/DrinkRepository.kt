package dev.ericferguson.watertracker.data

import kotlinx.coroutines.flow.Flow
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

    /** Returns the new row's id so the caller can undo it. */
    suspend fun add(amountOz: Int): Long =
        dao.insert(Drink(amountOz = amountOz, timestampMillis = clock.millis()))

    suspend fun remove(id: Long) = dao.deleteById(id)
}
