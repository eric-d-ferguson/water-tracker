package dev.ericferguson.watertracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.ericferguson.watertracker.MainActivity
import kotlin.math.roundToInt

private val PADDING = 8.dp
private val BUTTON_HEIGHT = 32.dp
private val MAX_GLASS_WIDTH = 56.dp

/**
 * Below this height the buttons get smaller, and sit side by side if their labels fit; otherwise
 * they stay stacked.
 */
private val COMPACT_HEIGHT = 150.dp
private val COMPACT_BUTTON_HEIGHT = 24.dp
private const val COMPACT_BUTTON_SP = 12f
private val BUTTON_GAP = 4.dp

/** Room a button label needs beside its text, so it doesn't touch the pill's rounded ends. */
private const val BUTTON_TEXT_INSET_DP = 8f

/**
 * Glance can't measure text, so widths are estimated: a bold digit is about 0.55em, and text grows
 * with the system text size ([fontScale]).
 */
private const val BOLD_DIGIT_EM = 0.55f

internal fun estimatedTextWidthDp(chars: Int, sp: Float, fontScale: Float, em: Float = BOLD_DIGIT_EM): Float =
    chars * sp * em * fontScale

/** The largest text size up to [maxSp] at which [chars] bold digits fit in [widthDp], but at least [minSp]. */
internal fun fittingSp(chars: Int, widthDp: Float, fontScale: Float, maxSp: Float, minSp: Float): Float =
    (widthDp / estimatedTextWidthDp(chars, 1f, fontScale)).coerceIn(minSp, maxSp)

/**
 * The 1x2 "water glass" widget: today's total as a fraction, a glass that fills from the bottom,
 * and the two quick-add buttons. It stretches taller if resized.
 *
 * The glass takes whatever height is left after the header and buttons (a layout weight), so
 * nothing gets pushed out of view at any size or text scale. [SizeMode.Exact] is only used to pick
 * the compact layout for short widgets, the glass width, and text sizes that fit the width.
 */
class GlassWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    // The widget picker gives previews less room than the home screen, and how much varies by
    // launcher, so offer a few heights; the launcher uses the largest that fits.
    override val previewSizeMode = SizeMode.Responsive(
        setOf(DpSize(80.dp, 90.dp), DpSize(80.dp, 140.dp), DpSize(80.dp, 200.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWaterContent(context) { state -> GlassContent(state) }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideWaterPreview(context) { state -> GlassContent(state) }
    }
}

class GlassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlassWidget()
}

@Composable
private fun GlassContent(state: WidgetState) {
    val unit = state.unit
    val size = LocalSize.current
    val fontScale = LocalContext.current.resources.configuration.fontScale
    val contentWidth = size.width - PADDING * 2
    val compact = size.height < COMPACT_HEIGHT
    // Side by side, each button gets half the width; "+16" wraps onto two lines if that's too narrow.
    val labelChars = maxOf(unit.fromMl(state.buttons.firstMl), unit.fromMl(state.buttons.secondMl)).toString().length + 1
    val sideBySide = compact &&
        estimatedTextWidthDp(labelChars, COMPACT_BUTTON_SP, fontScale) + BUTTON_TEXT_INSET_DP <=
        ((contentWidth - BUTTON_GAP) / 2).value
    val glassWidth = contentWidth.coerceAtMost(MAX_GLASS_WIDTH)
    val summary = "${unit.fromMl(state.totalMl)} of ${unit.format(state.goalMl)}"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(PADDING),
    ) {
        // Tapping the fraction or the glass opens the app.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>())
                .semantics { contentDescription = if (state.goalMet) "$summary, goal reached" else summary },
        ) {
            Fraction(state, availableWidth = contentWidth, fontScale = fontScale)
            Spacer(GlanceModifier.height(4.dp))
            Glass(progress = state.progress, modifier = GlanceModifier.defaultWeight().width(glassWidth))
        }
        Spacer(GlanceModifier.height(6.dp))
        if (sideBySide) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                val buttonModifier = GlanceModifier.defaultWeight().height(COMPACT_BUTTON_HEIGHT + 4.dp)
                AddButton(state.buttons.firstMl, unit, buttonModifier, COMPACT_BUTTON_SP.sp)
                Spacer(GlanceModifier.width(BUTTON_GAP))
                AddButton(state.buttons.secondMl, unit, buttonModifier, COMPACT_BUTTON_SP.sp)
            }
        } else if (compact) {
            val buttonModifier = GlanceModifier.fillMaxWidth().height(COMPACT_BUTTON_HEIGHT)
            AddButton(state.buttons.firstMl, unit, buttonModifier, COMPACT_BUTTON_SP.sp)
            Spacer(GlanceModifier.height(BUTTON_GAP))
            AddButton(state.buttons.secondMl, unit, buttonModifier, COMPACT_BUTTON_SP.sp)
        } else {
            AddButton(state.buttons.firstMl, unit, GlanceModifier.fillMaxWidth().height(BUTTON_HEIGHT))
            Spacer(GlanceModifier.height(6.dp))
            AddButton(state.buttons.secondMl, unit, GlanceModifier.fillMaxWidth().height(BUTTON_HEIGHT))
        }
    }
}

/**
 * "48/80 oz", in the same text color as the progress bar widget. The unit is dropped when it won't
 * fit on the line (narrow widget or large system text), leaving a clean "48/80", and the numbers
 * shrink if even they won't fit, so the goal is never cut off.
 */
@Composable
private fun Fraction(state: WidgetState, availableWidth: Dp, fontScale: Float) {
    val unit = state.unit
    val fraction = "${unit.fromMl(state.totalMl)}/${unit.fromMl(state.goalMl)}"
    // ml totals like "1450/2350" start smaller to fit one cell wide.
    val maxSp = if (fraction.length > 5) 13f else 17f
    val fractionSp = fittingSp(fraction.length, availableWidth.value, fontScale, maxSp, minSp = 8f)
    val neededWidth = estimatedTextWidthDp(fraction.length, fractionSp, fontScale) +
        estimatedTextWidthDp(unit.label.length + 1, UNIT_SP, fontScale, em = 0.5f)
    val showUnit = neededWidth <= availableWidth.value

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = fraction,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = fractionSp.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        if (showUnit) {
            Text(
                text = " ${unit.label}",
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = UNIT_SP.sp),
            )
        }
    }
}

private const val UNIT_SP = 11f

private const val GLASS_ROWS = 10
private const val SLICES_PER_ROW = 2
private const val GLASS_SLICES = GLASS_ROWS * SLICES_PER_ROW

/**
 * An empty glass with water filled to [progress] (0 to 1) from the bottom.
 *
 * Glance can't draw a partial shape or set proportional weights, so the glass is 20 equal slices
 * (10 rows of 2, because Glance allows at most 10 children per Column) and the bottom ones are
 * colored. The glass's rounded corners clip the water, which gives it a flat top like real water.
 */
@Composable
private fun Glass(progress: Float, modifier: GlanceModifier) {
    // Any water at all shows at least one slice, so a first drink is never invisible.
    val filled = (progress * GLASS_SLICES).roundToInt()
        .coerceIn(if (progress > 0f) 1 else 0, GLASS_SLICES)
    Column(
        modifier = modifier
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(12.dp),
    ) {
        repeat(GLASS_ROWS) { row ->
            Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                repeat(SLICES_PER_ROW) { slice ->
                    val isWater = row * SLICES_PER_ROW + slice >= GLASS_SLICES - filled
                    val sliceModifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    Box(modifier = if (isWater) sliceModifier.background(GlanceTheme.colors.primary) else sliceModifier) { }
                }
            }
        }
    }
}
