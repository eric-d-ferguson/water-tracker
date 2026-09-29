package dev.ericferguson.watertracker.data

import java.util.Locale
import kotlin.math.roundToInt

/**
 * How amounts are shown and entered. Everything is stored in whole ml; oz is only a display
 * unit, so switching units never loses precision (8 oz is stored as 237 ml and shows as 8 oz).
 */
enum class VolumeUnit(
    val label: String,
    /** The four quick-add buttons on Today, in this unit. */
    val quickAdd: List<Int>,
    val drinkRange: IntRange,
    val goalRange: IntRange,
    private val defaultGoal: Int,
    private val defaultWidgetButtons: Pair<Int, Int>,
) {
    OZ("oz", listOf(8, 12, 16, 20), 1..128, 1..999, 64, 8 to 16),
    ML("ml", listOf(250, 350, 500, 750), 1..4000, 1..30000, 2000, 250 to 500);

    fun toMl(amount: Int): Int = if (this == OZ) (amount * ML_PER_OZ).roundToInt() else amount

    fun fromMl(ml: Int): Int = if (this == OZ) (ml / ML_PER_OZ).roundToInt() else ml

    fun format(ml: Int): String = "${fromMl(ml)} $label"

    /**
     * Whether [totalMl] meets [goalMl], judged in this unit, the way the numbers are shown.
     * Each drink is rounded to whole ml on its own, so in oz the sum can land a hair under the
     * goal: five 16 oz drinks are 2365 ml against an 80 oz goal of 2366 ml, yet both show as 80 oz.
     */
    fun isGoalMet(totalMl: Int, goalMl: Int): Boolean = fromMl(totalMl) >= fromMl(goalMl)

    /** [totalMl] as a whole percentage of [goalMl], in this unit, so exactly 80 of 80 oz is 100%. */
    fun percentOfGoal(totalMl: Int, goalMl: Int): Int {
        val goal = fromMl(goalMl)
        return if (goal > 0) fromMl(totalMl) * 100 / goal else 0
    }

    val defaultGoalMl: Int get() = toMl(defaultGoal)
    val defaultWidgetButtonsMl: WidgetButtons
        get() = WidgetButtons(toMl(defaultWidgetButtons.first), toMl(defaultWidgetButtons.second))

    /** A tidy goal after switching units: whole ounces, or the nearest 50 ml. */
    fun roundGoal(ml: Int): Int = when (this) {
        OZ -> toMl(fromMl(ml).coerceAtLeast(1))
        ML -> ((ml + 25) / 50 * 50).coerceAtLeast(50)
    }

    companion object {
        const val ML_PER_OZ = 29.5735

        /** Ounces for the few countries that use them for drinks; ml everywhere else. */
        fun defaultFor(locale: Locale): VolumeUnit = if (locale.country in setOf("US", "LR", "MM")) OZ else ML
    }
}
