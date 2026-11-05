package com.lelesoftware.app.data.local

import android.content.Context
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.ScanGrid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * AnomalyRepository
 *
 * Repository providing access to persisted geophysical anomaly data
 * and full compressed survey grids.
 */
class AnomalyRepository(
    private val anomalyDao: AnomalyDao,
    private val scanDao: ScanDao? = null
) {

    val allAnomalies: Flow<List<AnomalyEntity>> = anomalyDao.getAllAnomalies()
    val anomalyCount: Flow<Int> = anomalyDao.getAnomalyCount()
    val allSurveys: Flow<List<ScanEntity>> = scanDao?.getAllSurveys() ?: emptyFlow()

    fun getAnomaliesByScan(scanId: String): Flow<List<AnomalyEntity>> =
        anomalyDao.getAnomaliesByScanId(scanId)

    fun searchAnomalies(query: String): Flow<List<AnomalyEntity>> =
        anomalyDao.searchAnomalies(query)

    fun filterByClassification(classification: String): Flow<List<AnomalyEntity>> =
        anomalyDao.getAnomaliesByClassification(classification)

    suspend fun getAnomalyById(id: String): AnomalyEntity? =
        anomalyDao.getAnomalyById(id)

    suspend fun saveAnomaly(
        anomaly: DetectedAnomaly,
        scanId: String = "scan-${System.currentTimeMillis()}",
        soilDensity: Float = 1.85f,
        soilType: String = "Standart Alüvyon",
        locationName: String = "Saha Ölçüm Alanı",
        notes: String = ""
    ) {
        val entity = AnomalyEntity.fromDetectedAnomaly(
            anomaly = anomaly,
            scanId = scanId,
            soilDensity = soilDensity,
            soilType = soilType,
            locationName = locationName,
            notes = notes
        )
        anomalyDao.insertAnomaly(entity)
    }

    suspend fun saveAllAnomalies(
        anomalies: List<DetectedAnomaly>,
        scanId: String = "scan-${System.currentTimeMillis()}",
        soilDensity: Float = 1.85f,
        soilType: String = "Standart Alüvyon",
        locationName: String = "Saha Ölçüm Alanı"
    ) {
        val entities = anomalies.map {
            AnomalyEntity.fromDetectedAnomaly(
                anomaly = it,
                scanId = scanId,
                soilDensity = soilDensity,
                soilType = soilType,
                locationName = locationName
            )
        }
        anomalyDao.insertAnomalies(entities)
    }

    suspend fun saveScanGrid(grid: ScanGrid) {
        val scanEntity = ScanEntity.fromScanGrid(grid)
        scanDao?.insertSurvey(scanEntity)
    }

    suspend fun loadScanGrid(id: String): ScanGrid? {
        val entity = scanDao?.getSurveyById(id) ?: return null
        return entity.toScanGrid()
    }

    suspend fun deleteSurvey(id: String) {
        scanDao?.deleteSurveyById(id)
    }

    suspend fun deleteAnomaly(id: String) {
        anomalyDao.deleteAnomalyById(id)
    }

    suspend fun clearAll() {
        anomalyDao.deleteAllAnomalies()
        scanDao?.clearAllSurveys()
    }

    companion object {
        @Volatile
        private var INSTANCE: AnomalyRepository? = null

        fun getInstance(context: Context): AnomalyRepository {
            return INSTANCE ?: synchronized(this) {
                val db = LeleDatabase.getDatabase(context)
                val repository = AnomalyRepository(db.anomalyDao(), db.scanDao())
                INSTANCE = repository
                repository
            }
        }
    }
}
