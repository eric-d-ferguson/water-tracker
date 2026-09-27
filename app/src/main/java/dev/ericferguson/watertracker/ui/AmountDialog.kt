package dev.ericferguson.watertracker.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import dev.ericferguson.watertracker.data.VolumeUnit

/** Asks for a whole amount in [unit] within [range]; used for custom drinks, the goal and widget buttons. */
@Composable
fun AmountDialog(
    title: String,
    initialValue: String,
    unit: VolumeUnit,
    range: IntRange,
    confirmLabel: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialValue) }
    val amount = text.toIntOrNull()?.takeIf { it in range }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> text = input.filter(Char::isDigit).take(range.last.toString().length) },
                suffix = { Text(unit.label) },
                singleLine = true,
                isError = text.isNotEmpty() && amount == null,
                supportingText = { Text("${range.first}–${range.last} ${unit.label}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { amount?.let(onConfirm) }, enabled = amount != null) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
