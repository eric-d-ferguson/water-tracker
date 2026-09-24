package dev.ericferguson.watertracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ericferguson.watertracker.data.DailyTotal
import dev.ericferguson.watertracker.ui.theme.WaterTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    HistoryContent(state = state, onSelectPeriod = viewModel::selectPeriod)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(state: HistoryUiState, onSelectPeriod: (HistoryPeriod) -> Unit) {
    val today = state.days.lastOrNull()?.date

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("History") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    HistoryPeriod.entries.forEachIndexed { index, period ->
                        SegmentedButton(
                            selected = state.period == period,
                            onClick = { onSelectPeriod(period) },
                            shape = SegmentedButtonDefaults.itemShape(index, HistoryPeriod.entries.size),
                        ) {
                            Text(period.label)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Daily average",
                        value = state.averageOz?.let { "$it oz" } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Goal met",
                        value = "${state.daysGoalMet} of ${state.days.size} days",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                DailyBarChart(
                    days = state.days,
                    goalOz = state.goalOz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp),
                )
            }
            item {
                Text(
                    text = "Days",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(state.days.asReversed(), key = { it.date.toEpochDay() }) { day ->
                DayRow(day = day, goalOz = state.goalOz, today = today)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * One bar per day with a dashed goal line. Bars that reach the goal are drawn solid.
 * Labels are narrow weekday names for a week; for a month, every 7th day counting back from today.
 */
@Composable
private fun DailyBarChart(days: List<DailyTotal>, goalOz: Int, modifier: Modifier = Modifier) {
    if (days.isEmpty()) return

    val metColor = MaterialTheme.colorScheme.primary
    val unmetColor = metColor.copy(alpha = 0.4f)
    val goalLineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()
    val showWeekdays = days.size <= 7

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Bar chart of daily water intake for the last ${days.size} days"
        },
    ) {
        val labelHeight = 20.dp.toPx()
        val chartHeight = size.height - labelHeight
        // Leave headroom so a bar at the maximum doesn't touch the top edge.
        val scaleMaxOz = max(goalOz, days.maxOf { it.totalOz }).coerceAtLeast(1) * 1.1f
        val slotWidth = size.width / days.size
        val barWidth = slotWidth * 0.6f
        val corner = CornerRadius(min(barWidth / 2, 4.dp.toPx()))

        days.forEachIndexed { index, day ->
            val slotLeft = index * slotWidth
            val barHeight = chartHeight * day.totalOz / scaleMaxOz
            if (barHeight > 0f) {
                drawRoundRect(
                    color = if (day.totalOz >= goalOz) metColor else unmetColor,
                    topLeft = Offset(slotLeft + (slotWidth - barWidth) / 2, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = corner,
                )
            }

            val daysAgo = days.lastIndex - index
            val label = when {
                showWeekdays -> day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                daysAgo % 7 == 0 -> day.date.dayOfMonth.toString()
                else -> null
            }
            if (label != null) {
                val layout = textMeasurer.measure(label, labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = slotLeft + (slotWidth - layout.size.width) / 2,
                        y = chartHeight + (labelHeight - layout.size.height) / 2,
                    ),
                )
            }
        }

        drawLine(baselineColor, Offset(0f, chartHeight), Offset(size.width, chartHeight), strokeWidth = 1.dp.toPx())

        val goalY = chartHeight - chartHeight * goalOz / scaleMaxOz
        drawLine(
            color = goalLineColor,
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
    }
}

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

@Composable
private fun DayRow(day: DailyTotal, goalOz: Int, today: LocalDate?) {
    val label = when (day.date) {
        today -> "Today"
        today?.minusDays(1) -> "Yesterday"
        else -> dayFormatter.format(day.date)
    }
    val percent = if (goalOz > 0) day.totalOz * 100 / goalOz else 0
    val metGoal = day.totalOz >= goalOz

    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text("$percent% of goal") },
        trailingContent = {
            Text(
                text = "${day.totalOz} oz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (metGoal) FontWeight.Bold else FontWeight.Normal,
                color = if (metGoal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun HistoryContentPreview() {
    val today = LocalDate.of(2026, 9, 23)
    val totals = listOf(40, 72, 64, 0, 56, 80, 24)
    WaterTrackerTheme {
        HistoryContent(
            state = HistoryUiState(
                period = HistoryPeriod.WEEK,
                days = totals.mapIndexed { i, oz -> DailyTotal(today.minusDays(6L - i), oz) },
                goalOz = 64,
            ),
            onSelectPeriod = {},
        )
    }
}
