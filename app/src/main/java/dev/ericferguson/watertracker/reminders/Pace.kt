package dev.ericferguson.watertracker.reminders

import java.time.Duration
import java.time.LocalTime
import kotlin.math.ceil

/**
 * Where you "should" be during the day. The target rises in a straight line from 0 oz at
 * wake-up to the full goal an hour before bedtime, so you're not drinking right before bed.
 */
object Pace {
    val FINISH_BEFORE_BED: Duration = Duration.ofHours(1)

    /** How far behind, as a fraction of the goal, before a reminder is worth sending. */
    const val BEHIND_FRACTION = 0.10

    fun targetOz(now: LocalTime, wake: LocalTime, bed: LocalTime, goalOz: Int): Int {
        val finish = bed.minus(FINISH_BEFORE_BED)
        if (!now.isAfter(wake)) return 0
        if (!now.isBefore(finish)) return goalOz
        val elapsed = Duration.between(wake, now).toMinutes()
        val span = Duration.between(wake, finish).toMinutes()
        return (goalOz * elapsed / span).toInt()
    }

    fun shouldRemind(now: LocalTime, wake: LocalTime, bed: LocalTime, goalOz: Int, totalOz: Int): Boolean {
        if (now.isBefore(wake) || !now.isBefore(bed)) return false
        if (totalOz >= goalOz) return false
        val behindOz = targetOz(now, wake, bed, goalOz) - totalOz
        return behindOz >= behindThresholdOz(goalOz)
    }

    fun behindThresholdOz(goalOz: Int): Int = ceil(goalOz * BEHIND_FRACTION).toInt()
}
