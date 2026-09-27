package dev.ericferguson.watertracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One logged drink. Amounts are whole ml whatever unit is shown (see [VolumeUnit]); times are
 * epoch millis so they sort and range-query cheaply.
 */
@Entity(tableName = "drinks", indices = [Index("timestampMillis")])
data class Drink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMl: Int,
    val timestampMillis: Long,
)
