package dev.ericferguson.watertracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.ericferguson.watertracker.MainActivity
import dev.ericferguson.watertracker.WaterTrackerApp
import dev.ericferguson.watertracker.data.WidgetButtons
import dev.ericferguson.watertracker.ui.theme.DarkColors
import dev.ericferguson.watertracker.ui.theme.LightColors
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

private data class WidgetState(val totalOz: Int, val goalOz: Int, val buttons: WidgetButtons)

/**
 * A 4x1 home-screen widget: today's total, a progress bar, and two quick-add buttons.
 *
 * Glance keeps a session running for a short while after the widget is shown or tapped. During
 * a session, updates recompose the existing content rather than calling provideGlance again, so
 * the content collects live data. Outside a session, WidgetUpdater starts a new one on every
 * change, and the system refreshes it every 30 minutes to catch the new day.
 */
class WaterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as WaterTrackerApp
        val state = combine(
            app.drinkRepository.drinksOn(LocalDate.now()).map { drinks -> drinks.sumOf { it.amountOz } },
            app.settingsRepository.dailyGoalOz,
            app.settingsRepository.widgetButtons,
            ::WidgetState,
        )
        // Load the first value up front so the widget never flashes placeholder numbers.
        val initial = state.first()

        provideContent {
            val current by state.collectAsState(initial)
            GlanceTheme(colors = WidgetColors) {
                WidgetContent(current.totalOz, current.goalOz, current.buttons)
            }
        }
    }
}

class WaterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterWidget()
}

@Composable
private fun WidgetContent(totalOz: Int, goalOz: Int, buttons: WidgetButtons) {
    val progress = if (goalOz > 0) (totalOz.toFloat() / goalOz).coerceIn(0f, 1f) else 0f
    val label = if (totalOz >= goalOz) "$totalOz oz · Goal reached!" else "$totalOz / $goalOz oz"

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
                progress = progress,
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.primaryContainer,
                modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
            )
        }
        Spacer(GlanceModifier.width(12.dp))
        AddButton(buttons.firstOz)
        Spacer(GlanceModifier.width(8.dp))
        AddButton(buttons.secondOz)
    }
}

@Composable
private fun AddButton(oz: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(width = 56.dp, height = 40.dp)
            .background(GlanceTheme.colors.primary)
            .cornerRadius(20.dp)
            .clickable(actionRunCallback<AddDrinkAction>(actionParametersOf(AddDrinkAction.AmountKey to oz)))
            .semantics { contentDescription = "Log $oz ounces" },
    ) {
        Text(
            text = "+$oz",
            style = TextStyle(
                color = GlanceTheme.colors.onPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
