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
import androidx.glance.appwidget.LinearProgressIndicator
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.ericferguson.watertracker.MainActivity

/**
 * The 4x1 "progress bar" widget: today's total, a progress bar with a behind-pace marker, and two
 * quick-add buttons. Placing the marker needs the bar's real width, so this uses SizeMode.Exact.
 */
class WaterWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    // Roughly a 4x1 cell on a phone, so the picker preview has realistic proportions.
    override val previewSizeMode = SizeMode.Responsive(setOf(DpSize(320.dp, 72.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWaterContent(context) { state -> BarContent(state) }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideWaterPreview(context) { state -> BarContent(state) }
    }
}

class WaterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterWidget()
}

@Composable
private fun BarContent(state: WidgetState) {
    val unit = state.unit
    val label = if (state.goalMet) {
        "${unit.format(state.totalMl)} · Goal reached!"
    } else {
        "${unit.fromMl(state.totalMl)} / ${unit.format(state.goalMl)}"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Tapping the text or bar opens the app.
        Column(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(GlanceModifier.height(3.dp))
            ProgressBar(state, barWidth = LocalSize.current.width - BAR_SURROUNDINGS)
        }
        Spacer(GlanceModifier.width(12.dp))
        AddButton(state.buttons.firstMl, unit, GlanceModifier.size(width = 56.dp, height = 40.dp))
        Spacer(GlanceModifier.width(8.dp))
        AddButton(state.buttons.secondMl, unit, GlanceModifier.size(width = 56.dp, height = 40.dp))
    }
}

// Everything in the widget's row besides the bar: 16 dp padding each side, then the 12 dp gap,
// two 56 dp buttons and the 8 dp gap between them.
private val BAR_SURROUNDINGS = 16.dp * 2 + 12.dp + 56.dp * 2 + 8.dp
private val MARKER_WIDTH = 4.dp
private val MARKER_HEIGHT = 14.dp

/** The progress bar, with a notch at the pace target when you're behind (like Today's ring). */
@Composable
private fun ProgressBar(state: WidgetState, barWidth: Dp) {
    val paceFraction = state.paceFraction
    val target = state.paceTargetMl
    var modifier = GlanceModifier.fillMaxWidth().height(MARKER_HEIGHT)
    if (target != null) {
        modifier = modifier.semantics { contentDescription = "Aim for ${state.unit.format(target)} by now" }
    }
    // A Box stacks its children, so the marker sits on top of the bar, pushed in from the start.
    Box(contentAlignment = Alignment.CenterStart, modifier = modifier) {
        LinearProgressIndicator(
            progress = state.progress,
            color = GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.primaryContainer,
            modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
        )
        if (paceFraction != null && barWidth > MARKER_WIDTH) {
            val offset = (barWidth * paceFraction - MARKER_WIDTH / 2).coerceIn(0.dp, barWidth - MARKER_WIDTH)
            Box(modifier = GlanceModifier.padding(start = offset)) {
                Box(
                    modifier = GlanceModifier
                        .size(width = MARKER_WIDTH, height = MARKER_HEIGHT)
                        .background(GlanceTheme.colors.onSurface)
                        .cornerRadius(2.dp),
                ) {}
            }
        }
    }
}
