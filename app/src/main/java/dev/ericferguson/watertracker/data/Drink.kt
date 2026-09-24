package dev.ericferguson.watertracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One logged drink. Times are stored as epoch millis so they sort and range-query cheaply. */
@Entity(tableName = "drinks", indices = [Index("timestampMillis")])
data class Drink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountOz: Int,
    val timestampMillis: Long,
)
