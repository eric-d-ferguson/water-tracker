package dev.ericferguson.watertracker

import android.app.Application
import android.os.Build
import dev.ericferguson.watertracker.data.DrinkBackup
import dev.ericferguson.watertracker.data.DrinkRepository
import dev.ericferguson.watertracker.data.SettingsRepository
import dev.ericferguson.watertracker.data.WaterDatabase
import dev.ericferguson.watertracker.reminders.ReminderManager
import dev.ericferguson.watertracker.widget.launchWidgetUpdates
import dev.ericferguson.watertracker.widget.publishWidgetPreviewsIfNeeded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** App-wide singletons. Small enough that a DI framework isn't worth it. */
class WaterTrackerApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database by lazy { WaterDatabase.build(this) }
    val drinkRepository by lazy { DrinkRepository(database.drinkDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val drinkBackup by lazy { DrinkBackup(contentResolver, drinkRepository) }
    val reminderManager by lazy { ReminderManager(this, settingsRepository, drinkRepository) }

    override fun onCreate() {
        super.onCreate()
        reminderManager.createNotificationChannel()
        // Force-stopping the app clears its alarms, so make sure one is set whenever the app starts.
        appScope.launch { reminderManager.reschedule() }
        appScope.launchWidgetUpdates(this, database, settingsRepository)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Previews show your unit, so republish when it changes (and once per app version).
            appScope.launch {
                settingsRepository.unit.distinctUntilChanged().collect { publishWidgetPreviewsIfNeeded(this@WaterTrackerApp, it) }
            }
        }
    }
}
