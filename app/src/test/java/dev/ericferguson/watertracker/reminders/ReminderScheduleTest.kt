package dev.ericferguson.watertracker.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReminderScheduleTest {
    private val wake = LocalTime.of(7, 0)
    private val bed = LocalTime.of(22, 0)
    private val day = LocalDate.of(2026, 9, 24)

    @Test
    fun checksEveryTwoHoursAfterWakeUpUntilBedtime() {
        assertEquals(
            listOf(9, 11, 13, 15, 17, 19, 21).map { LocalTime.of(it, 0) },
            ReminderSchedule.checkTimes(wake, bed),
        )
    }

    @Test
    fun noCheckAtBedtimeItself() {
        // 9 PM would be the next slot, but it lands exactly on bedtime.
        assertEquals(LocalTime.of(19, 0), ReminderSchedule.checkTimes(wake, LocalTime.of(21, 0)).last())
    }

    @Test
    fun nextCheckIsLaterToday() {
        assertEquals(day.atTime(11, 0), ReminderSchedule.nextCheck(day.atTime(9, 0), wake, bed))
    }

    @Test
    fun nextCheckBeforeWakeUpIsTodaysFirst() {
        assertEquals(day.atTime(9, 0), ReminderSchedule.nextCheck(day.atTime(5, 30), wake, bed))
    }

    @Test
    fun nextCheckAfterLastIsTomorrowsFirst() {
        assertEquals(day.plusDays(1).atTime(9, 0), ReminderSchedule.nextCheck(day.atTime(21, 0), wake, bed))
    }

    @Test
    fun windowMustBeThreeHoursAndSameDay() {
        assertTrue(ReminderSchedule.isValidWindow(wake, LocalTime.of(10, 0)))
        assertFalse(ReminderSchedule.isValidWindow(wake, LocalTime.of(9, 59)))
        assertFalse(ReminderSchedule.isValidWindow(LocalTime.of(22, 0), LocalTime.of(2, 0)))
    }

    @Test
    fun invalidWindowHasNoChecks() {
        assertNull(ReminderSchedule.nextCheck(day.atTime(9, 0), LocalTime.of(22, 0), LocalTime.of(2, 0)))
    }
}
