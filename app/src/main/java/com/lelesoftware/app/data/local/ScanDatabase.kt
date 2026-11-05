package com.lelesoftware.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * ScanDatabase (Room Database for Surveys & Anomalies)
 *
 * Büyük tarama matrislerinin SQLite 1 MB hücre limitini aşmaması için
 * TypeConverter ve GZIP binary serileştirme ile donatılmıştır.
 */
@Database(
    entities = [AnomalyEntity::class, ScanEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(ScanTypeConverters::class)
abstract class ScanDatabase : RoomDatabase() {

    abstract fun anomalyDao(): AnomalyDao
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: ScanDatabase? = null

        fun getDatabase(context: Context): ScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanDatabase::class.java,
                    "lele_geophysical_scans.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
