package dev.ericferguson.watertracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DrinkCsvTest {
    private val zone = ZoneId.of("America/Chicago")

    private fun drinkAt(time: LocalDateTime, ml: Int, id: Long = 0) =
        Drink(id = id, amountMl = ml, timestampMillis = time.atZone(zone).toInstant().toEpochMilli())

    private val morning = drinkAt(LocalDateTime.of(2026, 9, 27, 8, 15), 237, id = 1)
    private val noon = drinkAt(LocalDateTime.of(2026, 9, 27, 12, 0, 30, 123_000_000), 500, id = 2)

    @Test
    fun writesReadableRowsOldestFirst() {
        val csv = DrinkCsv.write(listOf(noon, morning), zone)
        assertEquals(
            listOf(
                "timestamp,amount_ml,amount_oz",
                "2026-09-27T08:15:00-05:00,237,8.0",
                "2026-09-27T12:00:30.123-05:00,500,16.9",
            ),
            csv.lines().filter { it.isNotEmpty() },
        )
    }

    @Test
    fun roundTripsExactly() {
        val parsed = DrinkCsv.parse(DrinkCsv.write(listOf(morning, noon), zone))
        assertEquals(listOf(morning, noon).map { it.copy(id = 0) }, parsed.drinks)
        assertEquals(0, parsed.invalidRows)
    }

    @Test
    fun countsBadRowsAndKeepsGoodOnes() {
        val text = """
            timestamp,amount_ml,amount_oz
            2026-09-27T08:15:00-05:00,237,8.0
            not a date,237,8.0
            2026-09-27T09:00:00-05:00,abc,
            2026-09-27T10:00:00-05:00,0,0
            2026-09-27T11:00:00Z,300
        """.trimIndent()
        val parsed = DrinkCsv.parse(text)
        assertEquals(listOf(237, 300), parsed.drinks.map { it.amountMl })
        assertEquals(3, parsed.invalidRows)
    }

    @Test
    fun handlesWindowsLineEndingsQuotesAndByteOrderMark() {
        val text = "﻿timestamp,amount_ml\r\n\"2026-09-27T08:15:00-05:00\",\"237\"\r\n"
        assertEquals(listOf(morning.copy(id = 0)), DrinkCsv.parse(text).drinks)
    }

    @Test
    fun importSkipsDrinksAlreadyStoredAndDuplicatesInTheFile() {
        val incoming = listOf(morning.copy(id = 0), noon.copy(id = 0), noon.copy(id = 0))
        val toImport = drinksToImport(existing = listOf(morning), incoming = incoming)
        assertEquals(listOf(noon.copy(id = 0)), toImport)
    }
}
