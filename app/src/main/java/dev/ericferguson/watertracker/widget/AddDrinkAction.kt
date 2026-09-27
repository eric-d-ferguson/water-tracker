package dev.ericferguson.watertracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dev.ericferguson.watertracker.WaterTrackerApp

/** Runs when a widget button is tapped. The widget picks up the new drink on its own. */
class AddDrinkAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val ml = parameters[AmountMlKey] ?: return
        (context.applicationContext as WaterTrackerApp).drinkRepository.add(ml)
    }

    companion object {
        val AmountMlKey = ActionParameters.Key<Int>("amountMl")
    }
}
