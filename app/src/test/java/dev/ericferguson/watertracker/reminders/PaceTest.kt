package dev.ericferguson.watertracker.reminders

import dev.ericferguson.watertracker.data.ReminderSettings
import dev.ericferguson.watertracker.data.VolumeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class PaceTest {
    // 7 AM to 10 PM, so the pace finishes at 9 PM: a 14-hour ramp.
    private val wake = LocalTime.of(7, 0)
    private val bed = LocalTime.of(22, 0)
    private val goal = 80

    private fun target(hour: Int, minute: Int = 0) = Pace.target(LocalTime.of(hour, minute), wake, bed, goal)

    private fun remind(hour: Int, total: Int) = Pace.shouldRemind(LocalTime.of(hour, 0), wake, bed, goal, total)

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
        assertEquals(8, Pace.behindThreshold(80))
        assertEquals(7, Pace.behindThreshold(64))
    }

    @Test
    fun remindsWhenAtLeastTenPercentBehind() {
        // Target at 2 PM is 40 oz; 8 oz behind is exactly the threshold.
        assertTrue(remind(14, total = 32))
    }

    @Test
    fun staysQuietWhenLessThanTenPercentBehind() {
        assertFalse(remind(14, total = 33))
    }

    @Test
    fun staysQuietOnceGoalIsMet() {
        assertFalse(remind(21, total = 80))
    }

    @Test
    fun staysQuietOutsideWakingHours() {
        assertFalse(remind(6, total = 0))
        assertFalse(remind(22, total = 0))
    }

    // paceTargetIfBehind: the shared rule for the Today screen and the reminders.
    private val reminders = ReminderSettings(enabled = false, wakeTime = wake, bedTime = bed)
    private val goalMl = VolumeUnit.OZ.toMl(80)

    private fun shown(hour: Int, totalOz: Int) =
        paceTargetIfBehind(LocalTime.of(hour, 0), reminders, goalMl, VolumeUnit.OZ.toMl(totalOz), VolumeUnit.OZ)

    @Test
    fun showsTheTargetWhenTenPercentBehind() {
        // 2 PM target is 40 oz; 32 oz is 8 oz behind.
        assertEquals(40, VolumeUnit.OZ.fromMl(shown(14, totalOz = 32)!!))
    }

    @Test
    fun hiddenWhenOnlySlightlyBehind() {
        assertNull(shown(14, totalOz = 36))
    }

    @Test
    fun hiddenOnceTheGoalIsMetAsDisplayed() {
        // Five 16 oz drinks are 2365 ml against 2366 ml, which still shows as 80 of 80 oz.
        val total = 5 * VolumeUnit.OZ.toMl(16)
        assertNull(paceTargetIfBehind(LocalTime.of(21, 30), reminders, goalMl, total, VolumeUnit.OZ))
    }

    @Test
    fun hiddenOutsideWakingHours() {
        assertNull(shown(6, totalOz = 0))
        assertNull(shown(22, totalOz = 0))
    }

    @Test
    fun worksWithRemindersTurnedOff() {
        assertNotNull(shown(14, totalOz = 0))
    }
}
