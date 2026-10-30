package dev.ericferguson.watertracker.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MidnightRefreshTest {
    private val chicago = ZoneId.of("America/Chicago")

    private fun at(text: String, zone: ZoneId = chicago) = ZonedDateTime.of(LocalDateTime.parse(text), zone)

    @Test
    fun refreshesJustAfterTheComingMidnight() {
        assertEquals(at("2026-10-31T00:01"), nextMidnightRefresh(at("2026-10-30T15:00")))
    }

    @Test
    fun aRefreshFromJustAfterMidnightWaitsForTheNextOne() {
        // The receiver reschedules from inside the refresh itself, at about 00:01.
        assertEquals(at("2026-10-31T00:01"), nextMidnightRefresh(at("2026-10-30T00:01:03")))
    }

    @Test
    fun handlesDaylightSavingEnding() {
        // Clocks go back at 2 AM on Nov 1, 2026, which doesn't touch midnight.
        assertEquals(at("2026-11-02T00:01"), nextMidnightRefresh(at("2026-11-01T12:00")))
    }

    @Test
    fun handlesAZoneWhereDaylightSavingSkipsMidnight() {
        // In Santiago, clocks jump from midnight to 1 AM on Sep 6, 2026, so that day starts at 1 AM.
        val santiago = ZoneId.of("America/Santiago")
        assertEquals(at("2026-09-06T01:01", santiago), nextMidnightRefresh(at("2026-09-05T20:00", santiago)))
    }
}
