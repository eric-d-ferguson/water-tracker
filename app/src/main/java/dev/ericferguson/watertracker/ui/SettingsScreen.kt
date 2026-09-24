package dev.ericferguson.watertracker.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ericferguson.watertracker.R
import dev.ericferguson.watertracker.data.ReminderSettings
import dev.ericferguson.watertracker.reminders.Pace
import dev.ericferguson.watertracker.reminders.ReminderSchedule
import dev.ericferguson.watertracker.ui.theme.WaterTrackerTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val GOAL_RANGE = 1..999
private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/**
 * Stateful entry point. Handles the Android 13+ notification permission here so that
 * [SettingsContent] stays previewable.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun notificationsAllowed() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    var allowed by remember { mutableStateOf(notificationsAllowed()) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    // Re-check when returning from system settings, where the user may have changed it.
    LifecycleResumeEffect(Unit) {
        allowed = notificationsAllowed()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = notificationsAllowed()
        if (allowed) viewModel.setRemindersEnabled(true) else permissionDenied = true
    }

    SettingsContent(
        state = state,
        showNotificationWarning = !allowed && (state.reminders.enabled || permissionDenied),
        onBack = onBack,
        onSetGoal = viewModel::setGoal,
        onRemindersToggled = { enabled ->
            if (enabled && !allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setRemindersEnabled(enabled)
            }
        },
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
        onSetWakeTime = viewModel::setWakeTime,
        onSetBedTime = viewModel::setBedTime,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    showNotificationWarning: Boolean,
    onBack: () -> Unit,
    onSetGoal: (Int) -> Unit,
    onRemindersToggled: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onSetWakeTime: (LocalTime) -> Unit,
    onSetBedTime: (LocalTime) -> Unit,
) {
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showWakeDialog by rememberSaveable { mutableStateOf(false) }
    var showBedDialog by rememberSaveable { mutableStateOf(false) }
    val reminders = state.reminders

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Goal")
            ListItem(
                headlineContent = { Text("Daily goal") },
                supportingContent = { Text("${state.goalOz} oz") },
                modifier = Modifier.clickable { showGoalDialog = true },
            )
            HorizontalDivider()

            SectionHeader("Reminders")
            ListItem(
                headlineContent = { Text("Pace reminders") },
                supportingContent = { Text("Notify me when I fall behind") },
                trailingContent = { Switch(checked = reminders.enabled, onCheckedChange = null) },
                modifier = Modifier.toggleable(
                    value = reminders.enabled,
                    role = Role.Switch,
                    onValueChange = onRemindersToggled,
                ),
            )
            if (showNotificationWarning) {
                NotificationWarning(onOpenNotificationSettings)
            }
            ListItem(
                headlineContent = { Text("Wake up") },
                supportingContent = { Text(timeFormatter.format(reminders.wakeTime)) },
                modifier = Modifier.clickable { showWakeDialog = true },
            )
            ListItem(
                headlineContent = { Text("Bedtime") },
                supportingContent = { Text(timeFormatter.format(reminders.bedTime)) },
                modifier = Modifier.clickable { showBedDialog = true },
            )
            Text(
                text = reminderExplanation(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (showGoalDialog) {
        AmountDialog(
            title = "Daily goal",
            initialValue = state.goalOz.toString(),
            range = GOAL_RANGE,
            confirmLabel = "Save",
            onConfirm = { onSetGoal(it); showGoalDialog = false },
            onDismiss = { showGoalDialog = false },
        )
    }
    if (showWakeDialog) {
        TimeDialog(
            title = "Wake up",
            initialTime = reminders.wakeTime,
            errorFor = { wake ->
                if (ReminderSchedule.isValidWindow(wake, reminders.bedTime)) null
                else "Must be at least 3 hours before bedtime (${timeFormatter.format(reminders.bedTime)})."
            },
            onConfirm = { onSetWakeTime(it); showWakeDialog = false },
            onDismiss = { showWakeDialog = false },
        )
    }
    if (showBedDialog) {
        TimeDialog(
            title = "Bedtime",
            initialTime = reminders.bedTime,
            errorFor = { bed ->
                if (ReminderSchedule.isValidWindow(reminders.wakeTime, bed)) null
                else "Must be at least 3 hours after wake-up (${timeFormatter.format(reminders.wakeTime)})" +
                    " and before midnight."
            },
            onConfirm = { onSetBedTime(it); showBedDialog = false },
            onDismiss = { showBedDialog = false },
        )
    }
}

private fun reminderExplanation(state: SettingsUiState): String {
    val checks = state.checkTimes.joinToString { timeFormatter.format(it) }
    return "Checks at $checks. You'll get a reminder if you're 10% of your goal " +
        "(${Pace.behindThresholdOz(state.goalOz)} oz) or more behind pace. The pace aims for your full goal " +
        "an hour before bedtime, and reminders stop once you reach it."
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun NotificationWarning(onOpenNotificationSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                text = "Notifications are turned off for Water Tracker, so reminders can't appear.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onOpenNotificationSettings, modifier = Modifier.align(Alignment.End)) {
                Text("Open settings")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    title: String,
    initialTime: LocalTime,
    errorFor: (LocalTime) -> String?,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    val picked = LocalTime.of(pickerState.hour, pickerState.minute)
    val error = errorFor(picked)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = pickerState)
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picked) }, enabled = error == null) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    WaterTrackerTheme {
        SettingsContent(
            state = SettingsUiState(goalOz = 80, reminders = ReminderSettings(enabled = true)),
            showNotificationWarning = true,
            onBack = {},
            onSetGoal = {},
            onRemindersToggled = {},
            onOpenNotificationSettings = {},
            onSetWakeTime = {},
            onSetBedTime = {},
        )
    }
}
