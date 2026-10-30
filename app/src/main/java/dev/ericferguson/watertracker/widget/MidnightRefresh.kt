package dev.ericferguson.watertracker.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Refreshes both widgets just after each local midnight, so they show the new day without
 * waiting for a tap. The widgets' 30-minute updatePeriodMillis refresh isn't enough on its own:
 * Android can hold it back for hours overnight, and a widget only works out "today" when it's
 * refreshed. One alarm is pending at a time; each refresh sets the next. Reboots, app updates
 * and clock or time-zone changes also land here, since they clear the alarm or change the date.
 */
class MidnightRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // goAsync keeps the receiver alive while Glance is asked to update.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WaterWidget().updateAll(context)
                GlassWidget().updateAll(context)
                scheduleMidnightRefresh(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Sets (or replaces) the alarm for the next refresh. */
fun scheduleMidnightRefresh(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val intent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, MidnightRefreshReceiver::class.java).setAction(ACTION_MIDNIGHT_REFRESH),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val triggerMillis = nextMidnightRefresh(ZonedDateTime.now()).toInstant().toEpochMilli()
    // Inexact, so no special permission, but allowed to fire while the phone is dozing: one wake-up a day.
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
}

/**
 * The next refresh after [now]: a minute past the coming local midnight, so the clock has safely
 * reached the new day. atStartOfDay handles days where daylight saving skips midnight.
 */
internal fun nextMidnightRefresh(now: ZonedDateTime): ZonedDateTime =
    now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plus(AFTER_MIDNIGHT)

private val AFTER_MIDNIGHT = Duration.ofMinutes(1)
private const val ACTION_MIDNIGHT_REFRESH = "dev.ericferguson.watertracker.action.MIDNIGHT_REFRESH"
