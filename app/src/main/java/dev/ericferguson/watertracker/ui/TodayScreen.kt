package dev.ericferguson.watertracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ericferguson.watertracker.R
import dev.ericferguson.watertracker.data.Drink
import dev.ericferguson.watertracker.ui.theme.WaterTrackerTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val QUICK_ADD_OZ = listOf(8, 12, 16, 20)
private val DRINK_RANGE = 1..128

/** Stateful entry point: wires the ViewModel to the stateless [TodayContent]. */
@Composable
fun TodayScreen(
    onOpenSettings: () -> Unit,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    TodayContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onAdd = { oz ->
            scope.launch {
                val id = viewModel.addDrink(oz)
                // Replace any snackbar still showing instead of queueing behind it.
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = "Added $oz oz",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.removeDrink(id)
            }
        },
        onRemove = viewModel::removeDrink,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayContent(
    state: TodayUiState,
    snackbarHostState: SnackbarHostState,
    onAdd: (Int) -> Unit,
    onRemove: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Water") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ProgressRing(
                    progress = state.progress,
                    totalOz = state.totalOz,
                    goalOz = state.goalOz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
            }
            item {
                QuickAddButtons(onAdd = onAdd, onCustom = { showCustomDialog = true })
            }
            item {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            if (state.drinks.isEmpty()) {
                item {
                    Text(
                        text = "Nothing logged yet today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.drinks, key = { it.id }) { drink ->
                DrinkRow(drink = drink, onRemove = { onRemove(drink.id) })
                HorizontalDivider()
            }
        }
    }

    if (showCustomDialog) {
        AmountDialog(
            title = "Custom amount",
            initialValue = "",
            range = DRINK_RANGE,
            confirmLabel = "Add",
            onConfirm = { onAdd(it); showCustomDialog = false },
            onDismiss = { showCustomDialog = false },
        )
    }
}

@Composable
private fun ProgressRing(progress: Float, totalOz: Int, goalOz: Int, modifier: Modifier = Modifier) {
    // The ring stops at full, but the number keeps counting past the goal.
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "progress")
    val trackColor = MaterialTheme.colorScheme.primaryContainer
    val fillColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val stroke = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(color = fillColor, startAngle = -90f, sweepAngle = 360f * animated, useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$totalOz oz", style = MaterialTheme.typography.displayMedium)
            Text(
                text = if (totalOz >= goalOz) "Goal reached!" else "of $goalOz oz",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickAddButtons(onAdd: (Int) -> Unit, onCustom: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_ADD_OZ.forEach { oz ->
                FilledTonalButton(
                    onClick = { onAdd(oz) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                ) {
                    Text("+$oz")
                }
            }
        }
        OutlinedButton(onClick = onCustom, modifier = Modifier.fillMaxWidth()) {
            Text("Custom amount")
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

@Composable
private fun DrinkRow(drink: Drink, onRemove: () -> Unit) {
    val time = Instant.ofEpochMilli(drink.timestampMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    ListItem(
        headlineContent = { Text("${drink.amountOz} oz") },
        supportingContent = { Text(timeFormatter.format(time)) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = "Remove")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun TodayContentPreview() {
    val now = System.currentTimeMillis()
    WaterTrackerTheme {
        TodayContent(
            state = TodayUiState(
                drinks = listOf(
                    Drink(id = 2, amountOz = 16, timestampMillis = now),
                    Drink(id = 1, amountOz = 12, timestampMillis = now - 3_600_000),
                ),
                goalOz = 64,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAdd = {},
            onRemove = {},
            onOpenSettings = {},
        )
    }
}
