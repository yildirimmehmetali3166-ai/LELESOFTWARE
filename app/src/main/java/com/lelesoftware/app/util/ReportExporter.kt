package com.lelesoftware.app.util

import android.content.Context
import android.content.Intent
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.ScanGrid
import java.util.Locale

object ReportExporter {

    fun generateCSV(grid: ScanGrid, anomalies: List<DetectedAnomaly>): String {
        val sb = StringBuilder()
        sb.append("LELE SOFTWARE - Jeofizik 3D/2D Yeraltı Tarama Raporu\n")
        sb.append("Baslik,").append(grid.title).append("\n")
        sb.append("Tarih,").append(grid.date).append("\n")
        sb.append("Sensor,").append(grid.sensorType).append("\n")
        sb.append("Zemin Referansi,").append(grid.groundBalance).append(" nT\n\n")

        sb.append("Hucresel Sinyal Matrisi (-100 ile +100):\n")
        for (y in 0 until grid.height) {
            val row = grid.points[y].joinToString(",") {
                String.format(Locale.US, "%.1f", it.filteredSignal)
            }
            sb.append(row).append("\n")
        }

        sb.append("\nTespit Edilen Anomaliler:\n")
        sb.append("ID,Baslik,Siniflandirma,Derinlik (m),Hacim (m3),X (m),Y (m),Sinyal\n")
        for (a in anomalies) {
            sb.append(
                String.format(
                    Locale.US,
                    "%s,%s,%s,%.2f,%.3f,%.1f,%.1f,%d%%\n",
                    a.id, a.title, a.classification.name, a.depthMeters, a.volumeM3, a.xMeters, a.yMeters, a.signalStrength
                )
            )
        }
        return sb.toString()
    }

    fun generateJSON(grid: ScanGrid, anomalies: List<DetectedAnomaly>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"id\": \"").append(grid.id).append("\",\n")
        sb.append("  \"title\": \"").append(grid.title).append("\",\n")
        sb.append("  \"date\": \"").append(grid.date).append("\",\n")
        sb.append("  \"width\": ").append(grid.width).append(",\n")
        sb.append("  \"height\": ").append(grid.height).append(",\n")
        sb.append("  \"stepDistanceMeters\": ").append(grid.stepDistanceMeters).append(",\n")
        sb.append("  \"groundBalance\": ").append(grid.groundBalance).append(",\n")
        sb.append("  \"sensorType\": \"").append(grid.sensorType).append("\",\n")
        sb.append("  \"locationName\": \"").append(grid.locationName).append("\",\n")
        sb.append("  \"anomaliesCount\": ").append(anomalies.size).append("\n")
        sb.append("}")
        return sb.toString()
    }

    fun shareReport(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Raporu Paylaş"))
    }
}
