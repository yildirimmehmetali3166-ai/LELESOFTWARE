package com.lelesoftware.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for geophysical anomaly SQLite persistence.
 */
@Dao
interface AnomalyDao {

    @Query("SELECT * FROM geophysical_anomalies ORDER BY recordedTimestamp DESC")
    fun getAllAnomalies(): Flow<List<AnomalyEntity>>

    @Query("SELECT * FROM geophysical_anomalies WHERE scanId = :scanId ORDER BY recordedTimestamp DESC")
    fun getAnomaliesByScanId(scanId: String): Flow<List<AnomalyEntity>>

    @Query("SELECT * FROM geophysical_anomalies WHERE id = :id LIMIT 1")
    suspend fun getAnomalyById(id: String): AnomalyEntity?

    @Query("SELECT * FROM geophysical_anomalies WHERE classification = :classification ORDER BY depthMeters ASC")
    fun getAnomaliesByClassification(classification: String): Flow<List<AnomalyEntity>>

    @Query("SELECT * FROM geophysical_anomalies WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR locationName LIKE '%' || :query || '%'")
    fun searchAnomalies(query: String): Flow<List<AnomalyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnomaly(anomaly: AnomalyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnomalies(anomalies: List<AnomalyEntity>)

    @Update
    suspend fun updateAnomaly(anomaly: AnomalyEntity)

    @Delete
    suspend fun deleteAnomaly(anomaly: AnomalyEntity)

    @Query("DELETE FROM geophysical_anomalies WHERE id = :id")
    suspend fun deleteAnomalyById(id: String)

    @Query("DELETE FROM geophysical_anomalies")
    suspend fun deleteAllAnomalies()

    @Query("SELECT COUNT(*) FROM geophysical_anomalies")
    fun getAnomalyCount(): Flow<Int>
}
