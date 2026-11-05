package com.lelesoftware.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Main Room SQLite Database for LELE SOFTWARE.
 * Stores geophysical surveys, compressed scan matrices, and detected anomalies for offline review.
 */
@Database(
    entities = [AnomalyEntity::class, ScanEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(ScanTypeConverters::class)
abstract class LeleDatabase : RoomDatabase() {

    abstract fun anomalyDao(): AnomalyDao
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: LeleDatabase? = null

        fun getDatabase(context: Context): LeleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeleDatabase::class.java,
                    "lele_geophysical.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
