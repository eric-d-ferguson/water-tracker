package dev.ericferguson.watertracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Insert
    suspend fun insert(drink: Drink): Long

    @Insert
    suspend fun insertAll(drinks: List<Drink>)

    @Query("SELECT * FROM drinks ORDER BY timestampMillis")
    suspend fun getAll(): List<Drink>

    @Query("DELETE FROM drinks WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Emits a fresh list every time the table changes. */
    @Query(
        """
        SELECT * FROM drinks
        WHERE timestampMillis >= :startMillis AND timestampMillis < :endMillis
        ORDER BY timestampMillis DESC
        """
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<Drink>>
}
