package dev.ericferguson.watertracker.reminders

import java.time.Duration
import java.time.LocalTime
import kotlin.math.ceil

/**
 * Where you "should" be during the day, in any unit. The target rises in a straight line from 0 at
 * wake-up to the full goal an hour before bedtime, so you're not drinking right before bed.
 */
object Pace {
    val FINISH_BEFORE_BED: Duration = Duration.ofHours(1)

    /** How far behind, as a fraction of the goal, before a reminder is worth sending. */
    const val BEHIND_FRACTION = 0.10

    fun target(now: LocalTime, wake: LocalTime, bed: LocalTime, goal: Int): Int {
        val finish = bed.minus(FINISH_BEFORE_BED)
        if (!now.isAfter(wake)) return 0
        if (!now.isBefore(finish)) return goal
        val elapsed = Duration.between(wake, now).toMinutes()
        val span = Duration.between(wake, finish).toMinutes()
        return (goal * elapsed / span).toInt()
    }

    fun shouldRemind(now: LocalTime, wake: LocalTime, bed: LocalTime, goal: Int, total: Int): Boolean {
        if (now.isBefore(wake) || !now.isBefore(bed)) return false
        if (total >= goal) return false
        val behind = target(now, wake, bed, goal) - total
        return behind >= behindThreshold(goal)
    }

    fun behindThreshold(goal: Int): Int = ceil(goal * BEHIND_FRACTION).toInt()
}
