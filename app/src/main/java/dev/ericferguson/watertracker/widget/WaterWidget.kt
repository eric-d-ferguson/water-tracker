package dev.ericferguson.watertracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.ericferguson.watertracker.MainActivity

/** The 4x1 "progress bar" widget: today's total, a progress bar, and two quick-add buttons. */
class WaterWidget : GlanceAppWidget() {
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
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = state.progress,
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.primaryContainer,
                modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
            )
        }
        Spacer(GlanceModifier.width(12.dp))
        AddButton(state.buttons.firstMl, unit, GlanceModifier.size(width = 56.dp, height = 40.dp))
        Spacer(GlanceModifier.width(8.dp))
        AddButton(state.buttons.secondMl, unit, GlanceModifier.size(width = 56.dp, height = 40.dp))
    }
}
