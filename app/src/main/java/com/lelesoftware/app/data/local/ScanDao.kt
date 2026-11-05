package com.lelesoftware.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * ScanDao
 *
 * Data Access Object for persisting and retrieving survey scans and compressed grid matrices.
 */
@Dao
interface ScanDao {

    @Query("SELECT * FROM surveys ORDER BY recordedTimestamp DESC")
    fun getAllSurveys(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM surveys WHERE id = :id LIMIT 1")
    suspend fun getSurveyById(id: String): ScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(scan: ScanEntity)

    @Query("DELETE FROM surveys WHERE id = :id")
    suspend fun deleteSurveyById(id: String)

    @Query("DELETE FROM surveys")
    suspend fun clearAllSurveys()

    @Query("SELECT COUNT(*) FROM surveys")
    suspend fun getSurveyCount(): Int
}
