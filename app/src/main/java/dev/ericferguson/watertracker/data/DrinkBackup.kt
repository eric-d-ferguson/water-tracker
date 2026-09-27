package dev.ericferguson.watertracker.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.ZoneId

/**
 * Exports all drinks to, and imports them from, a CSV file the user picks. Files are opened
 * through the system file picker, so the app needs no storage permission and no internet.
 */
class DrinkBackup(
    private val contentResolver: ContentResolver,
    private val drinks: DrinkRepository,
) {
    /** Returns how many drinks were written. */
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val all = drinks.all()
        val stream = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Couldn't open the file")
        stream.bufferedWriter().use { it.write(DrinkCsv.write(all, ZoneId.systemDefault())) }
        all.size
    }

    data class ImportResult(val added: Int, val duplicates: Int, val invalidRows: Int)

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val stream = contentResolver.openInputStream(uri) ?: throw IOException("Couldn't open the file")
        val parsed = DrinkCsv.parse(stream.bufferedReader().use { it.readText() })
        val added = drinks.import(parsed.drinks)
        ImportResult(added = added, duplicates = parsed.drinks.size - added, invalidRows = parsed.invalidRows)
    }
}
