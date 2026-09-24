package dev.ericferguson.watertracker.reminders

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** When pace checks happen: every [CHECK_INTERVAL] after wake-up, stopping before bedtime. */
object ReminderSchedule {
    val CHECK_INTERVAL: Duration = Duration.ofHours(2)

    /** Waking hours must be at least this long so there's room for a check and the pace. */
    val MIN_WAKING_HOURS: Duration = Duration.ofHours(3)

    /** Bedtime must be later on the same day than wake-up; windows crossing midnight aren't supported. */
    fun isValidWindow(wake: LocalTime, bed: LocalTime): Boolean =
        Duration.between(wake, bed) >= MIN_WAKING_HOURS

    fun checkTimes(wake: LocalTime, bed: LocalTime): List<LocalTime> {
        if (!isValidWindow(wake, bed)) return emptyList()
        // Stop if LocalTime wraps past midnight, which would make `it` smaller than `wake`.
        return generateSequence(wake.plus(CHECK_INTERVAL)) { it.plus(CHECK_INTERVAL) }
            .takeWhile { it.isAfter(wake) && it.isBefore(bed) }
            .toList()
    }

    /** The first check strictly after [now], today or tomorrow. Null if the window is invalid. */
    fun nextCheck(now: LocalDateTime, wake: LocalTime, bed: LocalTime): LocalDateTime? {
        val times = checkTimes(wake, bed)
        if (times.isEmpty()) return null
        val today = now.toLocalDate()
        return times.map { today.atTime(it) }.firstOrNull { it.isAfter(now) }
            ?: today.plusDays(1).atTime(times.first())
    }
}
