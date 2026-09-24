package dev.ericferguson.watertracker

import android.app.Application
import dev.ericferguson.watertracker.data.DrinkRepository
import dev.ericferguson.watertracker.data.SettingsRepository
import dev.ericferguson.watertracker.data.WaterDatabase

/** App-wide singletons. Small enough that a DI framework isn't worth it. */
class WaterTrackerApp : Application() {
    private val database by lazy { WaterDatabase.build(this) }
    val drinkRepository by lazy { DrinkRepository(database.drinkDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
}
