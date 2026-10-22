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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dev.ericferguson.watertracker.MainActivity

// Heights of everything around the glass at full size, so the glass can take the rest.
private val PADDING = 10.dp
private val TEXT_HEIGHT = 42.dp
private val BUTTON_HEIGHT = 34.dp
private val BUTTON_GAP = 6.dp
private val GLASS_GAPS = 6.dp + 8.dp
private val MIN_GLASS_HEIGHT = 20.dp
private val MAX_GLASS_WIDTH = 56.dp

/** Below this height everything shrinks in proportion, so nothing gets pushed out of view. */
private val FULL_SIZE_HEIGHT = 200.dp

/**
 * The 1x2 "water glass" widget: today's total, a glass that fills from the bottom, and the two
 * quick-add buttons stacked. It stretches taller if resized.
 *
 * Glance can't draw custom shapes, so the glass is two rounded boxes: an empty one, and a filled
 * one inside it sized to the progress. That needs real dp heights, so this widget uses
 * [SizeMode.Exact] and lays itself out from its actual size.
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
    val scale = (size.height / FULL_SIZE_HEIGHT).coerceIn(0.4f, 1f)
    val padding = PADDING * scale
    val buttonHeight = BUTTON_HEIGHT * scale
    val buttonGap = BUTTON_GAP * scale
    val glassHeight = (size.height - padding * 2 - (TEXT_HEIGHT + GLASS_GAPS) * scale - buttonHeight * 2 - buttonGap)
        .coerceAtLeast(MIN_GLASS_HEIGHT * scale)
    val glassWidth = (size.width - padding * 2).coerceAtMost(MAX_GLASS_WIDTH)
    val summary = "${unit.fromMl(state.totalMl)} of ${unit.format(state.goalMl)}"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(padding),
    ) {
        // Tapping the numbers or the glass opens the app.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>())
                .semantics { contentDescription = if (state.goalMet) "$summary, goal reached" else summary },
        ) {
            Text(
                text = unit.fromMl(state.totalMl).toString(),
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = (20 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
            )
            Text(
                text = if (state.goalMet) "Goal met!" else "of ${unit.format(state.goalMl)}",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = (11 * scale).sp,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(GlanceModifier.height(6.dp * scale))
            Glass(progress = state.progress, width = glassWidth, height = glassHeight)
        }
        Spacer(GlanceModifier.height(8.dp * scale))
        val fontSize = (15 * scale).sp
        AddButton(state.buttons.firstMl, unit, GlanceModifier.fillMaxWidth().height(buttonHeight), fontSize)
        Spacer(GlanceModifier.height(buttonGap))
        AddButton(state.buttons.secondMl, unit, GlanceModifier.fillMaxWidth().height(buttonHeight), fontSize)
    }
}

/** An empty glass with water filled to [progress] (0 to 1) from the bottom. */
@Composable
private fun Glass(progress: Float, width: Dp, height: Dp) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = GlanceModifier
            .size(width, height)
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(12.dp),
    ) {
        val water = height * progress
        if (water > 0.dp) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(water)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(12.dp),
            ) { }
        }
    }
}
