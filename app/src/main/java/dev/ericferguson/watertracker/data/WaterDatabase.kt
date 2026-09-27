package dev.ericferguson.watertracker.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [Drink::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = WaterDatabase.OzToMl::class)],
)
abstract class WaterDatabase : RoomDatabase() {
    abstract fun drinkDao(): DrinkDao

    /** Version 2 stores ml instead of oz: rename the column, then convert the values. */
    @RenameColumn(tableName = "drinks", fromColumnName = "amountOz", toColumnName = "amountMl")
    class OzToMl : AutoMigrationSpec {
        override fun onPostMigrate(connection: SQLiteConnection) {
            connection.execSQL(
                "UPDATE drinks SET amountMl = CAST(ROUND(amountMl * ${VolumeUnit.ML_PER_OZ}) AS INTEGER)",
            )
        }
    }

    companion object {
        fun build(context: Context): WaterDatabase =
            Room.databaseBuilder(context, WaterDatabase::class.java, "water.db").build()
    }
}
