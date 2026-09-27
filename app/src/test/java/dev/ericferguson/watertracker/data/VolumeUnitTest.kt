package dev.ericferguson.watertracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class VolumeUnitTest {
    @Test
    fun ozRoundTripsThroughMl() {
        (1..128).forEach { oz -> assertEquals(oz, VolumeUnit.OZ.fromMl(VolumeUnit.OZ.toMl(oz))) }
    }

    @Test
    fun ozConvertsToNearestMl() {
        assertEquals(237, VolumeUnit.OZ.toMl(8))
        assertEquals(1893, VolumeUnit.OZ.toMl(64))
    }

    @Test
    fun totalsAreSummedInMlBeforeConverting() {
        // Ten 8 oz drinks are 2370 ml, which still shows as 80 oz.
        assertEquals("80 oz", VolumeUnit.OZ.format(10 * VolumeUnit.OZ.toMl(8)))
    }

    @Test
    fun mlIsStoredAsIs() {
        assertEquals(250, VolumeUnit.ML.toMl(250))
        assertEquals("250 ml", VolumeUnit.ML.format(250))
    }

    @Test
    fun switchingUnitsRoundsTheGoal() {
        assertEquals(2350, VolumeUnit.ML.roundGoal(VolumeUnit.OZ.toMl(80))) // 2366 ml
        assertEquals(VolumeUnit.OZ.toMl(68), VolumeUnit.OZ.roundGoal(2000)) // 67.6 oz
    }

    @Test
    fun defaultUnitFollowsCountry() {
        assertEquals(VolumeUnit.OZ, VolumeUnit.defaultFor(Locale.US))
        assertEquals(VolumeUnit.ML, VolumeUnit.defaultFor(Locale.UK))
        assertEquals(VolumeUnit.ML, VolumeUnit.defaultFor(Locale.GERMANY))
    }
}
