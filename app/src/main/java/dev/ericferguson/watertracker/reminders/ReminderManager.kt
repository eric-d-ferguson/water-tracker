package dev.ericferguson.watertracker.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.ericferguson.watertracker.R
import dev.ericferguson.watertracker.data.DrinkRepository
import dev.ericferguson.watertracker.data.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

/**
 * Schedules the next pace check with AlarmManager and posts the reminder notification.
 * Only one alarm is ever pending; each check schedules the one after it.
 */
class ReminderManager(
    private val context: Context,
    private val settings: SettingsRepository,
    private val drinks: DrinkRepository,
    private val clock: () -> Clock = { Clock.systemDefaultZone() },
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val notifications = NotificationManagerCompat.from(context)

    fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("Reminders")
            .setDescription("Reminders when you fall behind your daily goal")
            .build()
        notifications.createNotificationChannel(channel)
    }

    /** Sets the alarm for the next check, or cancels everything if reminders are off. */
    suspend fun reschedule() {
        val reminder = settings.reminderSettings.first()
        val next = if (reminder.enabled) {
            ReminderSchedule.nextCheck(LocalDateTime.now(clock()), reminder.wakeTime, reminder.bedTime)
        } else {
            null
        }

        if (next == null) {
            alarmManager.cancel(checkIntent())
            notifications.cancel(NOTIFICATION_ID)
            return
        }

        // An inexact window needs no special permission. Plain set() could fire up to 90 minutes
        // late for an alarm 2 hours out; a window keeps it within 10 minutes unless the phone is dozing.
        val triggerMillis = next.atZone(clock().zone).toInstant().toEpochMilli()
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerMillis, ALARM_WINDOW.toMillis(), checkIntent())
    }

    /** Runs at each scheduled check and notifies if you're far enough behind. */
    suspend fun checkPace() {
        val reminder = settings.reminderSettings.first()
        if (!reminder.enabled) return

        val now = LocalDateTime.now(clock())
        val goalOz = settings.dailyGoalOz.first()
        val totalOz = drinks.totalOn(now.toLocalDate())
        val time = now.toLocalTime()
        if (Pace.shouldRemind(time, reminder.wakeTime, reminder.bedTime, goalOz, totalOz)) {
            showReminder(totalOz, Pace.targetOz(time, reminder.wakeTime, reminder.bedTime, goalOz))
        }
    }

    private fun showReminder(totalOz: Int, targetOz: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("Time for some water")
            .setContentText("You're at $totalOz oz. Aim for $targetOz oz by now to stay on pace.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        // A fixed id means a new reminder replaces an unread one instead of stacking.
        notifications.notify(NOTIFICATION_ID, notification)
    }

    private fun checkIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_CHECK_PACE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val ACTION_CHECK_PACE = "dev.ericferguson.watertracker.action.CHECK_PACE"
        private const val CHANNEL_ID = "reminders"
        private const val NOTIFICATION_ID = 1
        private val ALARM_WINDOW = Duration.ofMinutes(10)
    }
}
