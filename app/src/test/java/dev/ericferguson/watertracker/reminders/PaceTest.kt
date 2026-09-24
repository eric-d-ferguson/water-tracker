package dev.ericferguson.watertracker.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class PaceTest {
    // 7 AM to 10 PM, so the pace finishes at 9 PM: a 14-hour ramp.
    private val wake = LocalTime.of(7, 0)
    private val bed = LocalTime.of(22, 0)
    private val goal = 80

    private fun target(hour: Int, minute: Int = 0) = Pace.targetOz(LocalTime.of(hour, minute), wake, bed, goal)

    private fun remind(hour: Int, totalOz: Int) = Pace.shouldRemind(LocalTime.of(hour, 0), wake, bed, goal, totalOz)

    @Test
    fun targetIsZeroAtWakeUp() {
        assertEquals(0, target(7))
    }

    @Test
    fun targetIsHalfHalfwayToFinish() {
        assertEquals(40, target(14))
    }

    @Test
    fun targetIsFullGoalFromAnHourBeforeBed() {
        assertEquals(80, target(21))
        assertEquals(80, target(21, 30))
    }

    @Test
    fun thresholdIsTenPercentRoundedUp() {
        assertEquals(8, Pace.behindThresholdOz(80))
        assertEquals(7, Pace.behindThresholdOz(64))
    }

    @Test
    fun remindsWhenAtLeastTenPercentBehind() {
        // Target at 2 PM is 40 oz; 8 oz behind is exactly the threshold.
        assertTrue(remind(14, totalOz = 32))
    }

    @Test
    fun staysQuietWhenLessThanTenPercentBehind() {
        assertFalse(remind(14, totalOz = 33))
    }

    @Test
    fun staysQuietOnceGoalIsMet() {
        assertFalse(remind(21, totalOz = 80))
    }

    @Test
    fun staysQuietOutsideWakingHours() {
        assertFalse(remind(6, totalOz = 0))
        assertFalse(remind(22, totalOz = 0))
    }
}
