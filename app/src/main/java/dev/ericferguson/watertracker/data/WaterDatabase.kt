package dev.ericferguson.watertracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Drink::class], version = 1)
abstract class WaterDatabase : RoomDatabase() {
    abstract fun drinkDao(): DrinkDao

    companion object {
        fun build(context: Context): WaterDatabase =
            Room.databaseBuilder(context, WaterDatabase::class.java, "water.db").build()
    }
}
