package dev.ericferguson.watertracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.ericferguson.watertracker.WaterTrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the pace-check alarm, plus reboots, app updates and clock changes, which all
 * clear or invalidate the pending alarm and so need a reschedule.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminders = (context.applicationContext as WaterTrackerApp).reminderManager
        // goAsync keeps the receiver alive while the database and settings are read off the main thread.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == ReminderManager.ACTION_CHECK_PACE) reminders.checkPace()
                reminders.reschedule()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
