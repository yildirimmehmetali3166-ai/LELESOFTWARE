package com.lelesoftware.app.util

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.PaletteType
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.TargetClassification
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

enum class ExportFileFormat(
    val extension: String,
    val mimeType: String,
    val label: String
) {
    PDF("pdf", "application/pdf", "PDF Rapor"),
    PNG("png", "image/png", "PNG Görsel")
}

data class GridExportConfig(
    val palette: PaletteType = PaletteType.LELE_SPECTRUM,
    val smoothGrid: Boolean = false,
    val showCellValues: Boolean = false,
    val trailCoordinates: List<Pair<Int, Int>> = emptyList(),
    val activeProbeCell: Pair<Int, Int>? = null,
    val selectedCell: Pair<Int, Int>? = null,
    val sectorNumber: Int? = null
)

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
            val row = grid.points.getOrNull(y).orEmpty().joinToString(",") {
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

    fun suggestedFileName(grid: ScanGrid, format: ExportFileFormat): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val base = grid.title
            .lowercase(Locale.US)
            .replace("ç", "c")
            .replace("ğ", "g")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ş", "s")
            .replace("ü", "u")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(28)
            .ifEmpty { "lele_jeofizik_tarama" }
        return "${base}_${stamp}.${format.extension}"
    }

    /**
     * Renders an executive geophysical grid visualization bitmap ready for PNG export or preview.
     */
    fun renderGridBitmap(
        grid: ScanGrid,
        anomalies: List<DetectedAnomaly>,
        config: GridExportConfig = GridExportConfig(),
        widthPx: Int = 1600,
        heightPx: Int = 1220
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val s = widthPx / 1600f

        // Background
        canvas.drawColor(0xFF040814.toInt())

        val stats = computeGridStats(grid)
        val timestampText = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date())

        // Header Card
        val headerRect = RectF(36f * s, 32f * s, widthPx - 36f * s, 176f * s)
        val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0C1528.toInt()
            style = Paint.Style.FILL
        }
        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1F2D46.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * s
        }
        canvas.drawRoundRect(headerRect, 20f * s, 20f * s, cardFill)
        canvas.drawRoundRect(headerRect, 20f * s, 20f * s, cardBorder)

        // Amber brand accent strip
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(headerRect.left + 18f * s, headerRect.top + 22f * s, headerRect.left + 28f * s, headerRect.bottom - 22f * s),
            6f * s,
            6f * s,
            accentPaint
        )

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            textSize = 24f * s
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 36f * s
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF38BDF8.toInt()
            textSize = 22f * s
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val rightMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2E8F0.toInt()
            textSize = 21f * s
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.MONOSPACE
        }

        canvas.drawText("LELE SOFTWARE PRO • JEOFİZİK IZGARA GÖRSELİ", headerRect.left + 48f * s, headerRect.top + 52f * s, brandPaint)
        canvas.drawText(ellipsizeText(grid.title, 48), headerRect.left + 48f * s, headerRect.top + 96f * s, titlePaint)
        canvas.drawText(
            "${grid.sensorType} • ${config.palette.label} • Tarih: $timestampText",
            headerRect.left + 48f * s,
            headerRect.top + 136f * s,
            subPaint
        )

        val areaM2 = (grid.width * grid.stepDistanceMeters) * (grid.height * grid.stepYMeters)
        canvas.drawText(
            "Matris: ${grid.width}×${grid.height} (${stats.visited}/${stats.total} Hücre)",
            headerRect.right - 28f * s,
            headerRect.top + 64f * s,
            rightMetaPaint
        )
        canvas.drawText(
            String.format(Locale.US, "Alan: %.1f m² • Zemin: %d nT", areaM2, grid.groundBalance),
            headerRect.right - 28f * s,
            headerRect.top + 102f * s,
            rightMetaPaint
        )
        canvas.drawText(
            String.format(
                Locale.US,
                "Mod: %s • %d Anomali",
                if (config.smoothGrid) "Bilinear Yumuşatılmış" else "Blok Hücre",
                anomalies.size
            ),
            headerRect.right - 28f * s,
            headerRect.top + 140f * s,
            rightMetaPaint
        )

        // Hero Grid Visualization Card
        val gridCardRect = RectF(36f * s, 198f * s, widthPx - 36f * s, heightPx - 186f * s)
        canvas.drawRoundRect(gridCardRect, 20f * s, 20f * s, cardFill)
        canvas.drawRoundRect(gridCardRect, 20f * s, 20f * s, cardBorder)

        val innerBounds = RectF(
            gridCardRect.left + 28f * s,
            gridCardRect.top + 24f * s,
            gridCardRect.right - 28f * s,
            gridCardRect.bottom - 22f * s
        )
        drawGridVisualization(
            canvas = canvas,
            bounds = innerBounds,
            grid = grid,
            anomalies = anomalies,
            config = config,
            scaleFactor = 2.1f * s
        )

        // Bottom Telemetry & Top Anomalies Strip
        val footerRect = RectF(36f * s, heightPx - 164f * s, widthPx - 36f * s, heightPx - 32f * s)
        canvas.drawRoundRect(footerRect, 20f * s, 20f * s, cardFill)
        canvas.drawRoundRect(footerRect, 20f * s, 20f * s, cardBorder)

        val metricLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 19f * s
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val metricValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 27f * s
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val colW = footerRect.width() / 4f
        val yLabel = footerRect.top + 48f * s
        val yValue = footerRect.top + 92f * s

        canvas.drawText("SİNYAL ARALIĞI", footerRect.left + 26f * s, yLabel, metricLabelPaint)
        metricValuePaint.color = 0xFF22D3EE.toInt()
        canvas.drawText(
            String.format(Locale.US, "%+.0f%% .. %+.0f%%", stats.minSignal, stats.maxSignal),
            footerRect.left + 26f * s,
            yValue,
            metricValuePaint
        )

        canvas.drawText("İLERLEME", footerRect.left + colW + 20f * s, yLabel, metricLabelPaint)
        metricValuePaint.color = 0xFF10B981.toInt()
        val pct = if (stats.total > 0) (stats.visited * 100f / stats.total).roundToInt() else 0
        canvas.drawText("%$pct (${stats.visited}/${stats.total})", footerRect.left + colW + 20f * s, yValue, metricValuePaint)

        canvas.drawText("TESPİT EDİLEN HEDEF", footerRect.left + colW * 2f + 16f * s, yLabel, metricLabelPaint)
        metricValuePaint.color = 0xFFF59E0B.toInt()
        val topAnomaly = anomalies.firstOrNull()
        val targetSummary = if (topAnomaly != null) {
            "${anomalies.size} Hedef (${String.format(Locale.US, "%.2fm", topAnomaly.depthMeters)})"
        } else {
            "0 Kritik Hedef"
        }
        canvas.drawText(targetSummary, footerRect.left + colW * 2f + 16f * s, yValue, metricValuePaint)

        canvas.drawText("BASKIN SINIF / NOT", footerRect.left + colW * 3f + 12f * s, yLabel, metricLabelPaint)
        metricValuePaint.color = if (topAnomaly != null) AndroidColor.WHITE else 0xFF94A3B8.toInt()
        metricValuePaint.textSize = 21f * s
        val leadTitle = topAnomaly?.title?.let { ellipsizeText(it, 26) } ?: "Homojen Zemin Profili"
        canvas.drawText(leadTitle, footerRect.left + colW * 3f + 12f * s, yValue, metricValuePaint)

        return bitmap
    }

    fun exportGridImagePngBytes(
        grid: ScanGrid,
        anomalies: List<DetectedAnomaly>,
        config: GridExportConfig = GridExportConfig()
    ): ByteArray {
        val bitmap = renderGridBitmap(grid, anomalies, config)
        return try {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Generates a multi-page A4 corporate PDF geophysical report containing metadata,
     * KPI summary, vector-drawn grid visualization, and detected anomaly breakdown table.
     */
    fun exportPdfReportBytes(
        grid: ScanGrid,
        anomalies: List<DetectedAnomaly>,
        config: GridExportConfig = GridExportConfig()
    ): ByteArray {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 28f
        val contentWidth = pageWidth - margin * 2f

        val pdf = PdfDocument()
        val stats = computeGridStats(grid)
        val generatedAt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date())
        val totalArea = (grid.width * grid.stepDistanceMeters) * (grid.height * grid.stepYMeters)

        // Reserve space at bottom of Page 1 for anomaly rows, spill remainder to Page 2+
        val page1RowsMax = 6
        val laterPageRowsMax = 20
        val chunks: List<List<DetectedAnomaly>> = if (anomalies.isEmpty()) {
            listOf(emptyList())
        } else if (anomalies.size <= page1RowsMax) {
            listOf(anomalies)
        } else {
            val first = anomalies.take(page1RowsMax)
            val rest = anomalies.drop(page1RowsMax).chunked(laterPageRowsMax)
            listOf(first) + rest
        }
        val totalPages = chunks.size

        // Page 1
        val page1Info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = pdf.startPage(page1Info)
        val c1 = page1.canvas

        // Dark corporate background or clean light engineering report?
        // Crisp light engineering report background with dark navy header & high-contrast dark grid canvas matches both print & digital viewing.
        c1.drawColor(0xFFF8FAFC.toInt())

        // Top Corporate Header Banner
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B1221.toInt()
            style = Paint.Style.FILL
        }
        val bannerRect = RectF(0f, 0f, pageWidth.toFloat(), 84f)
        c1.drawRect(bannerRect, bannerPaint)

        val amberLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.FILL
        }
        c1.drawRect(0f, 81f, pageWidth.toFloat(), 84f, amberLinePaint)

        val hBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val hTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 17f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val hSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val hRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF38BDF8.toInt()
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.MONOSPACE
        }

        c1.drawText("LELE SOFTWARE PRO • JEOFİZİK SAHA ANALİZ RAPORU", margin, 26f, hBrandPaint)
        c1.drawText(ellipsizeText(grid.title, 48), margin, 48f, hTitlePaint)
        c1.drawText(
            "Sensör: ${grid.sensorType}  |  Zemin Dengesi: ${grid.groundBalance} nT",
            margin,
            66f,
            hSubPaint
        )
        c1.drawText("Rapor: $generatedAt", pageWidth - margin, 32f, hRightPaint)
        c1.drawText("Palet: ${config.palette.label}", pageWidth - margin, 48f, hRightPaint)
        c1.drawText("Matris: ${grid.width}×${grid.height}", pageWidth - margin, 64f, hRightPaint)

        // 4 KPI Cards Row
        var cursorY = 98f
        val kpiGap = 8f
        val kpiW = (contentWidth - kpiGap * 3f) / 4f
        val kpiH = 48f

        drawPdfKpiBox(
            canvas = c1,
            rect = RectF(margin, cursorY, margin + kpiW, cursorY + kpiH),
            label = "TARAMA ALANI",
            value = String.format(Locale.US, "%.1f m²", totalArea),
            sub = "${String.format(Locale.US, "%.1f", grid.width * grid.stepDistanceMeters)}m × ${String.format(Locale.US, "%.1f", grid.height * grid.stepYMeters)}m",
            accentColor = 0xFF0284C7.toInt()
        )
        drawPdfKpiBox(
            canvas = c1,
            rect = RectF(margin + (kpiW + kpiGap), cursorY, margin + kpiW * 2 + kpiGap, cursorY + kpiH),
            label = "VERİ KAPSAMI",
            value = "${stats.visited}/${stats.total}",
            sub = "%${if (stats.total > 0) (stats.visited * 100f / stats.total).roundToInt() else 0} Tarandı",
            accentColor = 0xFF059669.toInt()
        )
        drawPdfKpiBox(
            canvas = c1,
            rect = RectF(margin + (kpiW + kpiGap) * 2, cursorY, margin + kpiW * 3 + kpiGap * 2, cursorY + kpiH),
            label = "SİNYAL ARALIĞI",
            value = String.format(Locale.US, "%+.0f..%+.0f%%", stats.minSignal, stats.maxSignal),
            sub = String.format(Locale.US, "Ort |S|: %.1f%%", stats.avgAbsSignal),
            accentColor = 0xFF7C3AED.toInt()
        )
        drawPdfKpiBox(
            canvas = c1,
            rect = RectF(margin + (kpiW + kpiGap) * 3, cursorY, margin + contentWidth, cursorY + kpiH),
            label = "ANOMALİ SAYISI",
            value = "${anomalies.size} Hedef",
            sub = if (anomalies.isNotEmpty()) String.format(Locale.US, "Maks %.2fm", anomalies.maxOf { it.depthMeters }) else "Eşik altı",
            accentColor = 0xFFD97706.toInt()
        )

        cursorY += kpiH + 14f

        // Section Title: Grid Visualization
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val sectionMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF475569.toInt()
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }
        c1.drawText("2D IZGARA ISI HARİTASI VE ANOMALİ KONUMLARI", margin, cursorY, sectionTitlePaint)
        c1.drawText(
            if (config.smoothGrid) "Görünüm: Bilinear Yumuşatılmış" else "Görünüm: Blok Hücresel",
            pageWidth - margin,
            cursorY,
            sectionMetaPaint
        )
        cursorY += 8f

        // Embedded dark-framed high-contrast geophysical grid card
        val gridBoxHeight = 318f
        val gridContainerRect = RectF(margin, cursorY, pageWidth - margin, cursorY + gridBoxHeight)
        val darkGridFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF060C1A.toInt()
            style = Paint.Style.FILL
        }
        val darkGridStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E293B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        c1.drawRoundRect(gridContainerRect, 10f, 10f, darkGridFramePaint)
        c1.drawRoundRect(gridContainerRect, 10f, 10f, darkGridStrokePaint)

        val gridInner = RectF(
            gridContainerRect.left + 12f,
            gridContainerRect.top + 10f,
            gridContainerRect.right - 12f,
            gridContainerRect.bottom - 10f
        )
        drawGridVisualization(
            canvas = c1,
            bounds = gridInner,
            grid = grid,
            anomalies = anomalies,
            config = config,
            scaleFactor = 0.85f
        )

        cursorY += gridBoxHeight + 14f

        // Classification Summary Strip
        val voidCount = anomalies.count { it.classification == TargetClassification.VOID }
        val preciousCount = anomalies.count { it.classification == TargetClassification.PRECIOUS }
        val ferrousCount = anomalies.count { it.classification == TargetClassification.FERROUS }
        val mineralCount = anomalies.count { it.classification == TargetClassification.MINERAL }

        val summaryBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2E8F0.toInt()
            style = Paint.Style.FILL
        }
        val summaryRect = RectF(margin, cursorY, pageWidth - margin, cursorY + 24f)
        c1.drawRoundRect(summaryRect, 6f, 6f, summaryBg)
        val summaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E293B.toInt()
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        c1.drawText(
            "Sınıflandırma:  Boşluk/Oda: $voidCount   •   Değerli Metal: $preciousCount   •   Demir/Ferromanyetik: $ferrousCount   •   Mineral: $mineralCount",
            summaryRect.left + 10f,
            summaryRect.top + 15.5f,
            summaryTextPaint
        )

        cursorY += 36f

        // Anomaly Table (First Chunk)
        c1.drawText(
            "TESPİT EDİLEN ANOMALİLER (${anomalies.size} Hedef)",
            margin,
            cursorY,
            sectionTitlePaint
        )
        cursorY += 8f

        drawPdfAnomalyTable(
            canvas = c1,
            startX = margin,
            startY = cursorY,
            width = contentWidth,
            rows = chunks.first(),
            startIndex = 1
        )

        drawPdfFooter(c1, pageWidth, pageHeight, margin, 1, totalPages, grid.id)
        pdf.finishPage(page1)

        // Extra Pages if anomalies overflow page 1
        var runningIdx = 1 + chunks.first().size
        for (pageIdx in 1 until totalPages) {
            val pageNum = pageIdx + 1
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val page = pdf.startPage(pageInfo)
            val cp = page.canvas
            cp.drawColor(0xFFF8FAFC.toInt())

            // Compact continuation header
            cp.drawRect(RectF(0f, 0f, pageWidth.toFloat(), 48f), bannerPaint)
            cp.drawRect(0f, 46f, pageWidth.toFloat(), 48f, amberLinePaint)
            cp.drawText("LELE SOFTWARE PRO • ANOMALİ LİSTESİ (DEVAM)", margin, 22f, hBrandPaint)
            cp.drawText(ellipsizeText(grid.title, 54), margin, 38f, hSubPaint)
            cp.drawText("Sayfa $pageNum / $totalPages", pageWidth - margin, 30f, hRightPaint)

            drawPdfAnomalyTable(
                canvas = cp,
                startX = margin,
                startY = 66f,
                width = contentWidth,
                rows = chunks[pageIdx],
                startIndex = runningIdx
            )
            runningIdx += chunks[pageIdx].size

            drawPdfFooter(cp, pageWidth, pageHeight, margin, pageNum, totalPages, grid.id)
            pdf.finishPage(page)
        }

        val out = ByteArrayOutputStream()
        return try {
            pdf.writeTo(out)
            out.toByteArray()
        } finally {
            pdf.close()
        }
    }

    /**
     * Shared high-precision geophysical grid matrix renderer used by both PNG Bitmap and PDF Canvas exports.
     */
    fun drawGridVisualization(
        canvas: Canvas,
        bounds: RectF,
        grid: ScanGrid,
        anomalies: List<DetectedAnomaly>,
        config: GridExportConfig,
        scaleFactor: Float = 1f
    ) {
        val gridW = grid.width.coerceAtLeast(1)
        val gridH = grid.height.coerceAtLeast(1)

        val topAxisH = 18f * scaleFactor
        val leftAxisW = 36f * scaleFactor
        val legendAreaH = 38f * scaleFactor

        val availW = (bounds.width() - leftAxisW - 6f * scaleFactor).coerceAtLeast(40f)
        val availH = (bounds.height() - topAxisH - legendAreaH - 8f * scaleFactor).coerceAtLeast(40f)

        // Preserve grid cell proportions neatly within available frame
        val targetAspect = gridW.toFloat() / gridH.toFloat()
        val boxAspect = availW / availH
        val matrixW: Float
        val matrixH: Float
        if (targetAspect >= boxAspect) {
            matrixW = availW
            matrixH = (availW / targetAspect).coerceAtLeast(48f * scaleFactor)
        } else {
            matrixH = availH
            matrixW = (availH * targetAspect).coerceAtLeast(48f * scaleFactor)
        }

        val matrixLeft = bounds.left + leftAxisW + (availW - matrixW) * 0.5f
        val matrixTop = bounds.top + topAxisH + (availH - matrixH) * 0.5f
        val matrixRect = RectF(matrixLeft, matrixTop, matrixLeft + matrixW, matrixTop + matrixH)

        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = (10f * scaleFactor).coerceAtLeast(8f)
            typeface = Typeface.MONOSPACE
        }

        // X Axis Labels
        val xStartLabel = if (grid.isCsvImport) String.format(Locale.US, "%.1f", grid.rawXMin) else "0.0m"
        val xMidVal = if (grid.isCsvImport) (grid.rawXMin + grid.rawXMax) * 0.5f else (gridW * grid.stepDistanceMeters) * 0.5f
        val xEndVal = if (grid.isCsvImport) grid.rawXMax else gridW * grid.stepDistanceMeters
        val xMidLabel = if (grid.isCsvImport) String.format(Locale.US, "%.1f", xMidVal) else String.format(Locale.US, "%.1fm", xMidVal)
        val xEndLabel = if (grid.isCsvImport) String.format(Locale.US, "%.1f", xEndVal) else String.format(Locale.US, "%.1fm", xEndVal)

        axisPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(xStartLabel, matrixRect.left, matrixRect.top - 5f * scaleFactor, axisPaint)
        axisPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(xMidLabel, matrixRect.centerX(), matrixRect.top - 5f * scaleFactor, axisPaint)
        axisPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(xEndLabel, matrixRect.right, matrixRect.top - 5f * scaleFactor, axisPaint)

        // Y Axis Labels
        val yStartLabel = if (grid.isCsvImport) String.format(Locale.US, "%.1f", grid.rawYMin) else "0m"
        val yMidVal = if (grid.isCsvImport) (grid.rawYMin + grid.rawYMax) * 0.5f else (gridH * grid.stepYMeters) * 0.5f
        val yEndVal = if (grid.isCsvImport) grid.rawYMax else gridH * grid.stepYMeters
        val yMidLabel = if (grid.isCsvImport) String.format(Locale.US, "%.1f", yMidVal) else String.format(Locale.US, "%.1fm", yMidVal)
        val yEndLabel = if (grid.isCsvImport) String.format(Locale.US, "%.1f", yEndVal) else String.format(Locale.US, "%.1fm", yEndVal)

        axisPaint.textAlign = Paint.Align.RIGHT
        val labelX = matrixRect.left - 5f * scaleFactor
        canvas.drawText(yStartLabel, labelX, matrixRect.top + 9f * scaleFactor, axisPaint)
        canvas.drawText(yMidLabel, labelX, matrixRect.centerY() + 3f * scaleFactor, axisPaint)
        canvas.drawText(yEndLabel, labelX, matrixRect.bottom, axisPaint)

        // Matrix Frame Background
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        fillPaint.color = 0xFF040814.toInt()
        canvas.drawRect(matrixRect, fillPaint)

        val cellW = matrixW / gridW.toFloat()
        val cellH = matrixH / gridH.toFloat()

        // 1. Cells (Smooth 2x Bilinear or Discrete Matrix)
        if (config.smoothGrid) {
            val subX = gridW * 2
            val subY = gridH * 2
            val subW = matrixW / subX.toFloat()
            val subH = matrixH / subY.toFloat()

            for (sy in 0 until subY) {
                val gy = (sy.toFloat() / subY) * (gridH - 1)
                val y0 = gy.toInt().coerceIn(0, gridH - 1)
                val y1 = (y0 + 1).coerceIn(0, gridH - 1)
                val wy = gy - y0

                for (sx in 0 until subX) {
                    val gx = (sx.toFloat() / subX) * (gridW - 1)
                    val x0 = gx.toInt().coerceIn(0, gridW - 1)
                    val x1 = (x0 + 1).coerceIn(0, gridW - 1)
                    val wx = gx - x0

                    val p00 = grid.points.getOrNull(y0)?.getOrNull(x0)
                    val p10 = grid.points.getOrNull(y0)?.getOrNull(x1)
                    val p01 = grid.points.getOrNull(y1)?.getOrNull(x0)
                    val p11 = grid.points.getOrNull(y1)?.getOrNull(x1)

                    val anyVisited = (p00?.timestamp ?: 0L) > 0L || (p10?.timestamp ?: 0L) > 0L
                    fillPaint.color = if (anyVisited) {
                        val s00 = p00?.filteredSignal ?: 0f
                        val s10 = p10?.filteredSignal ?: s00
                        val s01 = p01?.filteredSignal ?: s00
                        val s11 = p11?.filteredSignal ?: s01
                        val interp = (s00 * (1f - wx) + s10 * wx) * (1f - wy) +
                            (s01 * (1f - wx) + s11 * wx) * wy
                        ColorScales.getPaletteColor(interp, config.palette).toArgb()
                    } else {
                        0xFF070E1D.toInt()
                    }
                    val left = matrixLeft + sx * subW
                    val top = matrixTop + sy * subH
                    canvas.drawRect(left, top, left + subW + 0.5f, top + subH + 0.5f, fillPaint)
                }
            }

            // Subtle Grid Mesh in Smooth Mode
            strokePaint.color = 0x24FFFFFF
            strokePaint.strokeWidth = 0.6f * scaleFactor
            for (x in 1 until gridW) {
                val lx = matrixLeft + x * cellW
                canvas.drawLine(lx, matrixTop, lx, matrixRect.bottom, strokePaint)
            }
            for (y in 1 until gridH) {
                val ly = matrixTop + y * cellH
                canvas.drawLine(matrixLeft, ly, matrixRect.right, ly, strokePaint)
            }
        } else {
            for (y in 0 until gridH) {
                val row = grid.points.getOrNull(y) ?: continue
                for (x in 0 until gridW) {
                    val pt = row.getOrNull(x) ?: continue
                    val left = matrixLeft + x * cellW
                    val top = matrixTop + y * cellH
                    val right = left + cellW
                    val bottom = top + cellH

                    if (pt.timestamp > 0L) {
                        fillPaint.color = ColorScales.getPaletteColor(pt.filteredSignal, config.palette).toArgb()
                        canvas.drawRect(left, top, right, bottom, fillPaint)

                        strokePaint.color = 0x33000000
                        strokePaint.strokeWidth = 0.8f * scaleFactor
                        canvas.drawRect(left, top, right, bottom, strokePaint)
                    } else {
                        fillPaint.color = 0xFF070E1D.toInt()
                        canvas.drawRect(left, top, right, bottom, fillPaint)

                        strokePaint.color = 0xFF132038.toInt()
                        strokePaint.strokeWidth = 0.7f * scaleFactor
                        canvas.drawRect(left, top, right, bottom, strokePaint)

                        fillPaint.color = 0xFF1F304D.toInt()
                        canvas.drawCircle(
                            left + cellW * 0.5f,
                            top + cellH * 0.5f,
                            (1.4f * scaleFactor).coerceAtLeast(1f),
                            fillPaint
                        )
                    }
                }
            }
        }

        // 2. Optional Cell Numeric Values
        if (config.showCellValues && min(cellW, cellH) >= 10f) {
            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textSize = (min(cellW, cellH) * 0.34f).coerceIn(6.5f, 22f * scaleFactor)
                color = AndroidColor.WHITE
                setShadowLayer(2f, 0f, 0f, AndroidColor.BLACK)
            }
            val yOffset = valPaint.textSize * 0.35f
            for (y in 0 until gridH) {
                val row = grid.points.getOrNull(y) ?: continue
                for (x in 0 until gridW) {
                    val pt = row.getOrNull(x) ?: continue
                    if (pt.timestamp > 0L) {
                        val cx = matrixLeft + x * cellW + cellW * 0.5f
                        val cy = matrixTop + y * cellH + cellH * 0.5f + yOffset
                        canvas.drawText("${pt.filteredSignal.roundToInt()}", cx, cy, valPaint)
                    }
                }
            }
        }

        // 3. Survey Traverse Trail
        if (config.trailCoordinates.size >= 2) {
            val trailPath = Path()
            config.trailCoordinates.forEachIndexed { idx, (tx, ty) ->
                val px = matrixLeft + tx.coerceIn(0, gridW - 1) * cellW + cellW * 0.5f
                val py = matrixTop + ty.coerceIn(0, gridH - 1) * cellH + cellH * 0.5f
                if (idx == 0) trailPath.moveTo(px, py) else trailPath.lineTo(px, py)
            }
            val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x88FFFFFF.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 1.6f * scaleFactor
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(trailPath, trailPaint)
        }

        // 4. Anomaly Halos
        val haloRadius = (min(cellW, cellH) * 0.44f).coerceAtLeast(4f * scaleFactor)
        for (anom in anomalies) {
            if (anom.gridX !in 0 until gridW || anom.gridY !in 0 until gridH) continue
            val cx = matrixLeft + anom.gridX * cellW + cellW * 0.5f
            val cy = matrixTop + anom.gridY * cellH + cellH * 0.5f
            val colorInt = anom.classification.colorHex.toInt()

            fillPaint.color = (colorInt and 0x00FFFFFF) or 0x44000000
            canvas.drawCircle(cx, cy, haloRadius * 1.2f, fillPaint)

            strokePaint.color = colorInt
            strokePaint.strokeWidth = 1.8f * scaleFactor
            canvas.drawCircle(cx, cy, haloRadius * 0.85f, strokePaint)
        }

        // 5. Active Probe Cell Marker
        config.activeProbeCell?.let { (px, py) ->
            if (px in 0 until gridW && py in 0 until gridH) {
                val cx = matrixLeft + px * cellW + cellW * 0.5f
                val cy = matrixTop + py * cellH + cellH * 0.5f
                val baseR = min(cellW, cellH) * 0.40f
                fillPaint.color = AndroidColor.WHITE
                canvas.drawCircle(cx, cy, (baseR * 0.38f).coerceAtLeast(3f), fillPaint)
                strokePaint.color = 0xFFF59E0B.toInt()
                strokePaint.strokeWidth = 2f * scaleFactor
                canvas.drawCircle(cx, cy, (baseR * 0.78f).coerceAtLeast(5f), strokePaint)
            }
        }

        // 6. Selected Inspection Cell
        config.selectedCell?.let { (sx, sy) ->
            if (sx in 0 until gridW && sy in 0 until gridH) {
                val left = matrixLeft + sx * cellW
                val top = matrixTop + sy * cellH
                strokePaint.color = AndroidColor.WHITE
                strokePaint.strokeWidth = 2.2f * scaleFactor
                canvas.drawRect(left, top, left + cellW, top + cellH, strokePaint)
            }
        }

        // Matrix outer border
        strokePaint.color = 0xFF334155.toInt()
        strokePaint.strokeWidth = 1.3f * scaleFactor
        canvas.drawRect(matrixRect, strokePaint)

        // Spectrum Legend Bar below matrix
        val barTop = matrixRect.bottom + 10f * scaleFactor
        val barBottom = barTop + 8f * scaleFactor
        val barRect = RectF(matrixRect.left, barTop, matrixRect.right, barBottom)

        val stops = floatArrayOf(-100f, -50f, 0f, 50f, 100f)
        val stopColors = stops.map { ColorScales.getPaletteColor(it, config.palette).toArgb() }.toIntArray()
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                barRect.left,
                barRect.top,
                barRect.right,
                barRect.top,
                stopColors,
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(barRect, 4f * scaleFactor, 4f * scaleFactor, legendPaint)

        val legTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (9.5f * scaleFactor).coerceAtLeast(8f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val textY = barBottom + 13f * scaleFactor
        legTextPaint.color = 0xFF38BDF8.toInt()
        legTextPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("-100 Boşluk", barRect.left, textY, legTextPaint)

        legTextPaint.color = 0xFF22C55E.toInt()
        legTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("0 Homojen Zemin", barRect.centerX(), textY, legTextPaint)

        legTextPaint.color = 0xFFF59E0B.toInt()
        legTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("+100 Değerli Metal", barRect.right, textY, legTextPaint)
    }

    private fun drawPdfKpiBox(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        sub: String,
        accentColor: Int
    ) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFCBD5E1.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val accentBar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 6f, 6f, bg)
        canvas.drawRoundRect(rect, 6f, 6f, border)
        canvas.drawRect(rect.left, rect.top + 6f, rect.left + 3f, rect.bottom - 6f, accentBar)

        val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 7.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF475569.toInt()
            textSize = 7.5f
        }
        canvas.drawText(label, rect.left + 9f, rect.top + 13f, lblPaint)
        canvas.drawText(value, rect.left + 9f, rect.top + 29f, valPaint)
        canvas.drawText(sub, rect.left + 9f, rect.top + 41f, subPaint)
    }

    private fun drawPdfAnomalyTable(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        width: Float,
        rows: List<DetectedAnomaly>,
        startIndex: Int
    ) {
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            style = Paint.Style.FILL
        }
        val headerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 8.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val rowEvenBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        val rowOddBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF1F5F9.toInt()
            style = Paint.Style.FILL
        }
        val cellBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFCBD5E1.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        val cellText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 8.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val cellMono = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E293B.toInt()
            textSize = 8f
            typeface = Typeface.MONOSPACE
        }

        val colOffsets = floatArrayOf(
            0f,
            width * 0.06f,
            width * 0.36f,
            width * 0.56f,
            width * 0.71f,
            width * 0.83f,
            width * 0.92f
        )

        val hdrRect = RectF(startX, startY, startX + width, startY + 20f)
        canvas.drawRect(hdrRect, headerBg)
        val hdrY = startY + 13f
        canvas.drawText("#", startX + colOffsets[0] + 5f, hdrY, headerText)
        canvas.drawText("Anomali / Sınıf", startX + colOffsets[1] + 4f, hdrY, headerText)
        canvas.drawText("Saha (X, Y)", startX + colOffsets[2] + 4f, hdrY, headerText)
        canvas.drawText("Derinlik", startX + colOffsets[3] + 4f, hdrY, headerText)
        canvas.drawText("FWHM", startX + colOffsets[4] + 4f, hdrY, headerText)
        canvas.drawText("Sinyal", startX + colOffsets[5] + 4f, hdrY, headerText)
        canvas.drawText("Güven", startX + colOffsets[6] + 4f, hdrY, headerText)

        var y = startY + 20f
        if (rows.isEmpty()) {
            val emptyRect = RectF(startX, y, startX + width, y + 28f)
            canvas.drawRect(emptyRect, rowEvenBg)
            canvas.drawRect(emptyRect, cellBorder)
            cellText.color = 0xFF64748B.toInt()
            canvas.drawText(
                "Mevcut tarama diliminde eşik değeri üzerinde kritik anomali tespit edilmedi.",
                startX + 10f,
                y + 17f,
                cellText
            )
            return
        }

        val rowH = 26f
        rows.forEachIndexed { idx, a ->
            val rect = RectF(startX, y, startX + width, y + rowH)
            canvas.drawRect(rect, if (idx % 2 == 0) rowEvenBg else rowOddBg)
            canvas.drawRect(rect, cellBorder)

            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = a.classification.colorHex.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(startX + colOffsets[1] + 7f, y + 9.5f, 3.2f, dotPaint)

            val rowIndex = startIndex + idx
            cellMono.color = 0xFF334155.toInt()
            canvas.drawText("$rowIndex", startX + colOffsets[0] + 5f, y + 16f, cellMono)

            cellText.color = 0xFF0F172A.toInt()
            cellText.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            canvas.drawText(ellipsizeText(a.title, 25), startX + colOffsets[1] + 14f, y + 11.5f, cellText)

            cellText.color = 0xFF475569.toInt()
            cellText.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            canvas.drawText(ellipsizeText(a.classification.label, 27), startX + colOffsets[1] + 14f, y + 21.5f, cellText)

            cellMono.color = 0xFF1E293B.toInt()
            canvas.drawText(
                String.format(Locale.US, "%.1fm, %.1fm", a.xMeters, a.yMeters),
                startX + colOffsets[2] + 4f,
                y + 15.5f,
                cellMono
            )
            canvas.drawText(
                String.format(Locale.US, "%.2f m", a.depthMeters),
                startX + colOffsets[3] + 4f,
                y + 15.5f,
                cellMono
            )
            canvas.drawText(
                String.format(Locale.US, "%.2fm", a.fwhmMeters),
                startX + colOffsets[4] + 4f,
                y + 15.5f,
                cellMono
            )
            canvas.drawText(
                "%${a.signalStrength}",
                startX + colOffsets[5] + 4f,
                y + 15.5f,
                cellMono
            )
            canvas.drawText(
                "%${a.depthConfidence}",
                startX + colOffsets[6] + 4f,
                y + 15.5f,
                cellMono
            )

            y += rowH
        }
    }

    private fun drawPdfFooter(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        margin: Float,
        pageNum: Int,
        totalPages: Int,
        gridId: String
    ) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFCBD5E1.toInt()
            strokeWidth = 0.7f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 8f
            typeface = Typeface.MONOSPACE
        }
        val lineY = pageHeight - 28f
        canvas.drawLine(margin, lineY, pageWidth - margin, lineY, linePaint)
        footerPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("LELE SOFTWARE Geo-Probe Pro • ID: $gridId", margin, pageHeight - 14f, footerPaint)
        footerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Sayfa $pageNum / $totalPages", pageWidth - margin, pageHeight - 14f, footerPaint)
    }

    // --- Zero-Permission File Storage & Sharing ---

    fun writeCacheFile(context: Context, fileName: String, bytes: ByteArray): File {
        val dir = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
        val file = File(dir, fileName)
        file.outputStream().use { it.write(bytes) }
        return file
    }

    fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = getShareUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun openFile(context: Context, file: File, mimeType: String): Boolean {
        return try {
            val uri = getShareUri(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Dosyayı Aç"))
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Zero-permission direct save on Android 10+ (API 29+) using MediaStore Downloads / Pictures.
     * Returns null on Android 8–9 (API 26–28) so callers can seamlessly fall back to SAF CreateDocument.
     */
    fun saveToDevicePublicCollection(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val resolver = context.contentResolver
        val isImage = mimeType.startsWith("image/")
        val collection = if (isImage) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val relativeDir = if (isImage) {
            "${Environment.DIRECTORY_PICTURES}/LELE SOFTWARE"
        } else {
            "${Environment.DIRECTORY_DOWNLOADS}/LELE SOFTWARE"
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val itemUri = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(itemUri)?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: return null

            val publish = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(itemUri, publish, null, null)
            itemUri
        } catch (_: Exception) {
            runCatching { resolver.delete(itemUri, null, null) }
            null
        }
    }

    fun writeToUri(context: Context, uri: Uri, bytes: ByteArray): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
                out.flush()
            } != null
        } catch (_: Exception) {
            false
        }
    }

    private data class GridStats(
        val total: Int,
        val visited: Int,
        val minSignal: Float,
        val maxSignal: Float,
        val avgAbsSignal: Float
    )

    private fun computeGridStats(grid: ScanGrid): GridStats {
        var total = 0
        var visited = 0
        var minSig = Float.POSITIVE_INFINITY
        var maxSig = Float.NEGATIVE_INFINITY
        var absSum = 0f

        for (y in 0 until grid.height) {
            val row = grid.points.getOrNull(y) ?: continue
            for (x in 0 until grid.width) {
                val pt = row.getOrNull(x) ?: continue
                total++
                if (pt.timestamp > 0L) {
                    visited++
                    if (pt.filteredSignal < minSig) minSig = pt.filteredSignal
                    if (pt.filteredSignal > maxSig) maxSig = pt.filteredSignal
                    absSum += abs(pt.filteredSignal)
                }
            }
        }
        return GridStats(
            total = total.coerceAtLeast(grid.width * grid.height),
            visited = visited,
            minSignal = if (visited > 0) minSig else 0f,
            maxSignal = if (visited > 0) maxSig else 0f,
            avgAbsSignal = if (visited > 0) absSum / visited else 0f
        )
    }

    private fun ellipsizeText(text: String, maxChars: Int): String {
        return if (text.length <= maxChars) text else text.take(maxChars - 1) + "…"
    }
}
