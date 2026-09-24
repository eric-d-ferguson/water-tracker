package dev.ericferguson.watertracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    val dailyGoalOz: Flow<Int> = context.dataStore.data.map { it[DAILY_GOAL_OZ] ?: DEFAULT_GOAL_OZ }

    suspend fun setDailyGoalOz(oz: Int) {
        context.dataStore.edit { it[DAILY_GOAL_OZ] = oz }
    }

    companion object {
        const val DEFAULT_GOAL_OZ = 64
        private val DAILY_GOAL_OZ = intPreferencesKey("daily_goal_oz")
    }
}
