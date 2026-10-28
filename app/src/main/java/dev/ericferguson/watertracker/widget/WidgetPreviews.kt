package dev.ericferguson.watertracker.widget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import dev.ericferguson.watertracker.WaterTrackerApp
import dev.ericferguson.watertracker.data.VolumeUnit
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * Publishes generated previews for the launcher's widget picker (Android 15+): the real widget
 * layouts showing your own goal, unit and buttons.
 *
 * They're an enhancement, not the only preview. In testing, the Pixel launcher only picked up a
 * newly published preview for widgets already placed on the home screen, so a fresh install would
 * still show the app icon. The static previewLayout in each widget's provider XML covers that case
 * (and Android 12-14). Android also rate-limits publishing, so this only republishes when the app
 * version or unit changes; a rate-limited attempt is retried on the next app start.
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal suspend fun publishWidgetPreviewsIfNeeded(context: Context, unit: VolumeUnit) {
    val prefs = context.getSharedPreferences("widget_previews", Context.MODE_PRIVATE)
    val versionCode = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    val key = "$versionCode:${unit.name}"
    if (prefs.getString(PUBLISHED_KEY, null) == key) return

    val manager = GlanceAppWidgetManager(context)
    val results = listOf(
        manager.setWidgetPreviews(WaterWidgetReceiver::class),
        manager.setWidgetPreviews(GlassWidgetReceiver::class),
    )
    Log.i(TAG, "Published widget previews for $key: bar=${results[0]} glass=${results[1]} (0 = success, 1 = rate limited)")
    if (results.all { it == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS }) {
        prefs.edit { putString(PUBLISHED_KEY, key) }
    }
}

private const val PUBLISHED_KEY = "published"
private const val TAG = "WidgetPreviews"

/** Sample data for previews: your own goal, unit and buttons, 60% of the way there. */
internal suspend fun previewState(context: Context): WidgetState {
    val settings = (context.applicationContext as WaterTrackerApp).settingsRepository
    val unit = settings.unit.first()
    val goalMl = settings.dailyGoalMl.first()
    val totalMl = unit.toMl((unit.fromMl(goalMl) * 0.6f).roundToInt())
    return WidgetState(totalMl, goalMl, settings.widgetButtons.first(), unit)
}
