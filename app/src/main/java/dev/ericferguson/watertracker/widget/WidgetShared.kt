package dev.ericferguson.watertracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.ericferguson.watertracker.WaterTrackerApp
import dev.ericferguson.watertracker.data.VolumeUnit
import dev.ericferguson.watertracker.data.WidgetButtons
import dev.ericferguson.watertracker.reminders.paceTargetIfBehind
import dev.ericferguson.watertracker.ui.theme.DarkColors
import dev.ericferguson.watertracker.ui.theme.LightColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Everything both widgets show. Amounts are in ml; [unit] says how to show them. */
internal data class WidgetState(
    val totalMl: Int,
    val goalMl: Int,
    val buttons: WidgetButtons,
    val unit: VolumeUnit,
    /** Where the pace says you should be, only when you're far enough behind to show it. */
    val paceTargetMl: Int? = null,
) {
    val goalMet: Boolean get() = unit.isGoalMet(totalMl, goalMl)

    /** Where the pace marker goes, 0 to 1 along the bar; null hides it. */
    val paceFraction: Float?
        get() = paceTargetMl?.let { if (goalMl > 0) (it.toFloat() / goalMl).coerceIn(0f, 1f) else null }

    /** 0 to 1 for drawing; full once the goal counts as met. */
    val progress: Float
        get() = when {
            goalMet -> 1f
            goalMl > 0 -> (totalMl.toFloat() / goalMl).coerceIn(0f, 1f)
            else -> 0f
        }
}

/**
 * Shows [content] with live data in the app's colors.
 *
 * Glance keeps a session running for a short while after a widget is shown or tapped. During a
 * session, updates recompose the existing content rather than calling provideGlance again, so
 * the content collects live data. Outside a session, WidgetUpdater starts a new one on every
 * change, and the system refreshes widgets every 30 minutes to catch the new day and move the
 * pace marker along.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun GlanceAppWidget.provideWaterContent(
    context: Context,
    content: @Composable (WidgetState) -> Unit,
) {
    val app = context.applicationContext as WaterTrackerApp
    val state = combine(
        // Follow the date, so a session that's still running at midnight resets to the new day.
        currentDay().flatMapLatest { day -> app.drinkRepository.drinksOn(day) }
            .map { drinks -> drinks.sumOf { it.amountMl } },
        app.settingsRepository.dailyGoalMl,
        app.settingsRepository.widgetButtons,
        app.settingsRepository.unit,
        app.settingsRepository.reminderSettings,
    ) { totalMl, goalMl, buttons, unit, reminders ->
        // Same rule as the Today screen, as of when the widget is updated.
        val paceTargetMl = paceTargetIfBehind(LocalTime.now(), reminders, goalMl, totalMl, unit)
        WidgetState(totalMl, goalMl, buttons, unit, paceTargetMl)
    }
    // Load the first value up front so the widget never flashes placeholder numbers.
    val initial = state.first()

    provideContent {
        val current by state.collectAsState(initial)
        GlanceTheme(colors = WidgetColors) {
            content(current)
        }
    }
}

/** Shows [content] with sample data, for the launcher's widget picker (see WidgetPreviews). */
internal suspend fun GlanceAppWidget.provideWaterPreview(
    context: Context,
    content: @Composable (WidgetState) -> Unit,
) {
    val state = previewState(context)
    provideContent {
        GlanceTheme(colors = WidgetColors) {
            content(state)
        }
    }
}

private val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

/** Today's date, emitted again at each local midnight. */
private fun currentDay(): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now()
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000))
    }
}

/** A pill-shaped quick-add button that logs [ml]; the caller sets its size with [modifier]. */
@Composable
internal fun AddButton(ml: Int, unit: VolumeUnit, modifier: GlanceModifier, fontSize: TextUnit = 15.sp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(GlanceTheme.colors.primary)
            .cornerRadius(20.dp)
            .clickable(actionRunCallback<AddDrinkAction>(actionParametersOf(AddDrinkAction.AmountMlKey to ml)))
            .semantics { contentDescription = "Log ${unit.format(ml)}" },
    ) {
        Text(
            text = "+${unit.fromMl(ml)}",
            style = TextStyle(
                color = GlanceTheme.colors.onPrimary,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
