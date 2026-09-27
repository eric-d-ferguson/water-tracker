package dev.ericferguson.watertracker.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dev.ericferguson.watertracker.data.SettingsRepository
import dev.ericferguson.watertracker.data.WaterDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Asks Glance to update the widget whenever the drinks table or the goal/button settings
 * change, from the app, the widget or anywhere else. That starts a fresh session when none is
 * running; a running session already follows the data itself. Runs for as long as the app's
 * process is alive, which is whenever any of those changes can happen.
 */
fun CoroutineScope.launchWidgetUpdates(context: Context, database: WaterDatabase, settings: SettingsRepository) =
    launch {
        merge(
            database.invalidationTracker.createFlow("drinks").map { },
            settings.dailyGoalMl.map { },
            settings.widgetButtons.map { },
            settings.unit.map { },
        ).collectLatest {
            WaterWidget().updateAll(context)
        }
    }
