package com.lelesoftware.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room SQLite Database for LELE SOFTWARE.
 * Stores geophysical surveys, detected anomalies, soil density calibrations, and depth data for offline review.
 */
@Database(
    entities = [AnomalyEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LeleDatabase : RoomDatabase() {

    abstract fun anomalyDao(): AnomalyDao

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
