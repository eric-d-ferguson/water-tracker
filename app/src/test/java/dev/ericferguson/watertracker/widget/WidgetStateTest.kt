package dev.ericferguson.watertracker.widget

import dev.ericferguson.watertracker.data.VolumeUnit
import dev.ericferguson.watertracker.data.WidgetButtons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetStateTest {
    private fun state(goalMl: Int, paceTargetMl: Int?) =
        WidgetState(totalMl = 0, goalMl = goalMl, buttons = WidgetButtons(250, 500), unit = VolumeUnit.ML, paceTargetMl = paceTargetMl)

    @Test
    fun paceMarkerIsFractionOfGoal() {
        assertEquals(0.5f, state(goalMl = 2000, paceTargetMl = 1000).paceFraction!!, 0.0001f)
    }

    @Test
    fun noPaceMarkerWhenNotBehind() {
        assertNull(state(goalMl = 2000, paceTargetMl = null).paceFraction)
    }

    @Test
    fun paceMarkerStaysOnTheBar() {
        assertEquals(1f, state(goalMl = 2000, paceTargetMl = 2500).paceFraction)
        assertNull(state(goalMl = 0, paceTargetMl = 500).paceFraction)
    }
}
