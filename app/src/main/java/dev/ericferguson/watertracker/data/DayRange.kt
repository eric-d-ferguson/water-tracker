package dev.ericferguson.watertracker.data

import java.time.LocalDate
import java.time.ZoneId

/** The half-open interval [startMillis, endMillis) covering one local calendar day. */
data class DayRange(val startMillis: Long, val endMillis: Long) {
    companion object {
        // atStartOfDay handles DST, so a day can be 23 or 25 hours long.
        fun of(date: LocalDate, zone: ZoneId): DayRange = DayRange(
            startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
    }
}
