package com.lelesoftware.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.TargetClassification

/**
 * Room SQLite Entity for persisting detected geophysical anomalies.
 * Stores spatial, volumetric, depth, signal amplitude, and soil density parameters for offline review.
 */
@Entity(tableName = "geophysical_anomalies")
data class AnomalyEntity(
    @PrimaryKey
    val id: String,
    val scanId: String,
    val title: String,
    val classification: String,
    val colorHex: Long,
    val xMeters: Float,
    val yMeters: Float,
    val gridX: Int,
    val gridY: Int,
    val depthMeters: Float,
    val minDepthMeters: Float,
    val maxDepthMeters: Float,
    val volumeM3: Float,
    val signalStrength: Int,
    val peakAmplitude: Float,
    val fwhmMeters: Float,
    val depthConfidence: Int,
    val soilDensityGcm3: Float = 1.85f,
    val soilType: String = "Standart Alüvyon",
    val description: String,
    val calculationFormula: String,
    val locationName: String = "Saha Ölçüm Noktası",
    val recordedTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    fun toDetectedAnomaly(): DetectedAnomaly {
        val targetClass = try {
            TargetClassification.valueOf(classification)
        } catch (_: Exception) {
            TargetClassification.PRECIOUS
        }
        return DetectedAnomaly(
            id = id,
            title = title,
            classification = targetClass,
            xMeters = xMeters,
            yMeters = yMeters,
            gridX = gridX,
            gridY = gridY,
            depthMeters = depthMeters,
            volumeM3 = volumeM3,
            signalStrength = signalStrength,
            description = description,
            peakAmplitude = peakAmplitude,
            fwhmMeters = fwhmMeters,
            minDepthMeters = minDepthMeters,
            maxDepthMeters = maxDepthMeters,
            depthConfidence = depthConfidence,
            calculationFormula = calculationFormula
        )
    }

    companion object {
        fun fromDetectedAnomaly(
            anomaly: DetectedAnomaly,
            scanId: String = "scan-${System.currentTimeMillis()}",
            soilDensity: Float = 1.85f,
            soilType: String = "Standart Alüvyon",
            locationName: String = "Saha Ölçüm Noktası",
            notes: String = ""
        ): AnomalyEntity {
            return AnomalyEntity(
                id = anomaly.id,
                scanId = scanId,
                title = anomaly.title,
                classification = anomaly.classification.name,
                colorHex = anomaly.classification.colorHex,
                xMeters = anomaly.xMeters,
                yMeters = anomaly.yMeters,
                gridX = anomaly.gridX,
                gridY = anomaly.gridY,
                depthMeters = anomaly.depthMeters,
                minDepthMeters = anomaly.minDepthMeters,
                maxDepthMeters = anomaly.maxDepthMeters,
                volumeM3 = anomaly.volumeM3,
                signalStrength = anomaly.signalStrength,
                peakAmplitude = anomaly.peakAmplitude,
                fwhmMeters = anomaly.fwhmMeters,
                depthConfidence = anomaly.depthConfidence,
                soilDensityGcm3 = soilDensity,
                soilType = soilType,
                description = anomaly.description,
                calculationFormula = anomaly.calculationFormula,
                locationName = locationName,
                recordedTimestamp = System.currentTimeMillis(),
                notes = notes
            )
        }
    }
}
