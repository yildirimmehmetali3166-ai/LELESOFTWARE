package com.lelesoftware.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lelesoftware.app.model.ScanGrid

/**
 * ScanEntity
 *
 * Full survey grid SQLite persistence model.
 * Uses compact binary GZIP compressed matrix storage to prevent exceeding the 1 MB SQLite limit.
 */
@Entity(tableName = "surveys")
data class ScanEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val date: String,
    val width: Int,
    val height: Int,
    val stepDistanceMeters: Float,
    val groundBalance: Int,
    val sensorType: String,
    val locationName: String,
    val notes: String,
    val totalPoints: Int,
    val recordedTimestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val compressedMatrixBytes: ByteArray
) {
    fun toScanGrid(converter: ScanTypeConverters = ScanTypeConverters()): ScanGrid {
        val points = converter.fromCompressedBytesToMatrix(compressedMatrixBytes)
        return ScanGrid(
            id = id,
            title = title,
            date = date,
            width = width,
            height = height,
            stepDistanceMeters = stepDistanceMeters,
            points = points,
            groundBalance = groundBalance,
            sensorType = sensorType,
            locationName = locationName,
            notes = notes
        )
    }

    companion object {
        fun fromScanGrid(
            grid: ScanGrid,
            converter: ScanTypeConverters = ScanTypeConverters()
        ): ScanEntity {
            val bytes = converter.fromMatrixToCompressedBytes(grid.points)
            return ScanEntity(
                id = grid.id,
                title = grid.title,
                date = grid.date,
                width = grid.width,
                height = grid.height,
                stepDistanceMeters = grid.stepDistanceMeters,
                groundBalance = grid.groundBalance,
                sensorType = grid.sensorType,
                locationName = grid.locationName,
                notes = grid.notes,
                totalPoints = grid.width * grid.height,
                compressedMatrixBytes = bytes
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScanEntity

        if (id != other.id) return false
        if (!compressedMatrixBytes.contentEquals(other.compressedMatrixBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + compressedMatrixBytes.contentHashCode()
        return result
    }
}
