package dev.ericferguson.watertracker.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassTextFitTest {
    @Test
    fun fractionKeepsFullSizeWhenItFits() {
        // "48/80" on a one-cell widget at normal text size.
        assertEquals(17f, fittingSp(chars = 5, widthDp = 66f, fontScale = 1f, maxSp = 17f, minSp = 8f))
    }

    @Test
    fun fractionShrinksToFitLargeText() {
        // "104/80" at 2x text: shrunk so its estimated width fits instead of being cut off.
        val sp = fittingSp(chars = 6, widthDp = 66f, fontScale = 2f, maxSp = 13f, minSp = 8f)
        assertTrue(sp < 13f)
        assertTrue(estimatedTextWidthDp(6, sp, 2f) <= 66f + 0.01f)
    }

    @Test
    fun fractionNeverShrinksBelowTheMinimum() {
        assertEquals(8f, fittingSp(chars = 9, widthDp = 20f, fontScale = 2f, maxSp = 13f, minSp = 8f))
    }
}
