package dev.ericferguson.watertracker.data

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The export/import file format: one drink per line, with an ISO-8601 local time (including its
 * UTC offset, so it reads naturally and still means one exact moment), the amount in ml, and the
 * amount in oz for convenience in spreadsheets. Import reads only the first two columns.
 */
object DrinkCsv {
    const val HEADER = "timestamp,amount_ml,amount_oz"
    private val MAX_ML = 10_000

    fun write(drinks: List<Drink>, zone: ZoneId): String = buildString {
        appendLine(HEADER)
        drinks.sortedBy { it.timestampMillis }.forEach { drink ->
            val time = Instant.ofEpochMilli(drink.timestampMillis).atZone(zone).toOffsetDateTime()
            val oz = String.format(Locale.US, "%.1f", drink.amountMl / VolumeUnit.ML_PER_OZ)
            append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time)).append(',')
            append(drink.amountMl).append(',')
            appendLine(oz)
        }
    }

    data class ParseResult(val drinks: List<Drink>, val invalidRows: Int)

    fun parse(text: String): ParseResult {
        val rows = text.removePrefix("﻿").lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("timestamp", ignoreCase = true) }
        var invalid = 0
        val drinks = rows.mapNotNull { row ->
            val columns = row.split(',').map { it.trim().removeSurrounding("\"") }
            val millis = columns.getOrNull(0)?.let {
                runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
            }
            val ml = columns.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..MAX_ML }
            if (millis == null || ml == null) {
                invalid++
                null
            } else {
                Drink(amountMl = ml, timestampMillis = millis)
            }
        }.toList()
        return ParseResult(drinks, invalid)
    }
}

/**
 * The drinks from [incoming] that aren't already in [existing], matched on time and amount, so
 * importing the same file twice doesn't double anything. Ids are cleared so Room assigns new ones.
 */
fun drinksToImport(existing: List<Drink>, incoming: List<Drink>): List<Drink> {
    val seen = existing.mapTo(HashSet()) { it.timestampMillis to it.amountMl }
    return incoming
        .filter { seen.add(it.timestampMillis to it.amountMl) }
        .map { it.copy(id = 0) }
}
