package com.lelesoftware.app.util

import android.content.Context
import android.net.Uri
import com.lelesoftware.app.model.CsvBandSummary
import com.lelesoftware.app.model.CsvClusterFinding
import com.lelesoftware.app.model.CsvDeepAnalysisReport
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ScanPoint
import com.lelesoftware.app.model.SoilType
import com.lelesoftware.app.model.TargetClassification
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class CsvRawSample(
    val x: Float,
    val y: Float,
    val z: Float,
    val value: Float
)

object CsvSurveyAnalyzer {

    fun loadAssetCsv(
        context: Context,
        assetFileName: String,
        id: String,
        displayTitle: String,
        locationName: String
    ): ScanGrid? {
        return try {
            val text = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
            parseCsvText(
                csvText = text,
                id = id,
                displayTitle = displayTitle,
                fileName = assetFileName,
                locationName = locationName
            )
        } catch (_: Exception) {
            null
        }
    }

    fun loadUriCsv(
        context: Context,
        uri: Uri,
        fileName: String
    ): ScanGrid? {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return null
            val cleanName = fileName.substringAfterLast('/').ifBlank { "harici_tarama.csv" }
            parseCsvText(
                csvText = text,
                id = "csv-${System.currentTimeMillis()}",
                displayTitle = "CSV Tarama: $cleanName",
                fileName = cleanName,
                locationName = "İçe Aktarılan Saha Dosyası ($cleanName)"
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseCsvText(
        csvText: String,
        id: String,
        displayTitle: String,
        fileName: String,
        locationName: String = "Saha CSV Taraması ($fileName)"
    ): ScanGrid? {
        val samples = mutableListOf<CsvRawSample>()
        val lines = csvText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        for (line in lines) {
            val parts = line.split(',', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (parts.size < 4) continue

            val x = parts[0].toFloatOrNull() ?: continue
            val y = parts[1].toFloatOrNull() ?: continue
            val z = parts[2].toFloatOrNull() ?: continue
            val v = parts[3].toFloatOrNull() ?: continue
            samples.add(CsvRawSample(x = x, y = y, z = z, value = v))
        }

        if (samples.isEmpty()) return null

        val xs = samples.map { it.x }.distinct().sorted()
        val ys = samples.map { it.y }.distinct().sorted()
        val width = xs.size.coerceAtLeast(1)
        val height = ys.size.coerceAtLeast(1)

        val xIndexMap = xs.withIndex().associate { it.value to it.index }
        val yIndexMap = ys.withIndex().associate { it.value to it.index }

        val rawMatrix = Array(height) { arrayOfNulls<CsvRawSample>(width) }
        for (s in samples) {
            val xi = xIndexMap[s.x] ?: continue
            val yi = yIndexMap[s.y] ?: continue
            rawMatrix[yi][xi] = s
        }

        val stepXRaw = if (xs.size >= 2) {
            (xs.last() - xs.first()) / (xs.size - 1).toFloat()
        } else 10f
        val stepYRaw = if (ys.size >= 2) {
            (ys.last() - ys.first()) / (ys.size - 1).toFloat()
        } else stepXRaw

        // Convert scanner coordinate units (e.g., 0..320 x 0..300 cm) to meters if span >= 50
        val unitScaleToMeters = if ((xs.lastOrNull() ?: 0f) >= 50f || (ys.lastOrNull() ?: 0f) >= 50f) {
            0.01f
        } else {
            1.0f
        }
        val stepXMeters = (stepXRaw * unitScaleToMeters).coerceAtLeast(0.05f)
        val stepYMeters = (stepYRaw * unitScaleToMeters).coerceAtLeast(0.05f)

        val validSamples = samples.filter { !(it.z == 0f && it.value == 0f) }
        val deadCount = samples.size - validSamples.size

        val values = validSamples.map { it.value }
        val zs = validSamples.map { it.z }
        val maxAbsVal = (values.maxOfOrNull { abs(it) } ?: 100f).coerceAtLeast(1f)
        val signalScale = if (maxAbsVal > 100f) 98f / maxAbsVal else (90f / maxAbsVal).coerceIn(1f, 2f)

        val valueMean = if (values.isNotEmpty()) values.average().toFloat() else 0f
        val valueSorted = values.sorted()
        val valueMedian = medianOfSorted(valueSorted)
        val valueStdDev = stdDevOf(values, valueMean)
        val valueMad = medianOfSorted(values.map { abs(it - valueMedian) }.sorted())

        val zMean = if (zs.isNotEmpty()) zs.average().toFloat() else 0f
        val zSorted = zs.sorted()
        val zMedian = medianOfSorted(zSorted)
        val zMin = zs.minOrNull() ?: 0f
        val zMax = zs.maxOrNull() ?: 0f

        val prods = validSamples
            .filter { it.z > 0.1f && abs(it.value) > 0.1f }
            .map { abs(it.z * it.value) }
            .sorted()
        val calProdMedian = medianOfSorted(prods)

        val positiveCount = validSamples.count { it.value > 0f }
        val negativeCount = validSamples.count { it.value < 0f }
        val strongVoidCount = validSamples.count { it.value < -40f }
        val saturatedFloorCount = validSamples.count { it.value <= -172f || (it.z in 5.4f..5.65f && it.value < -150f) }
        val highImpedanceCount = validSamples.count { it.z > 150f }

        // Polarity sign flips across adjacent cells
        var polarityFlipCount = 0
        for (yi in 0 until height) {
            for (xi in 0 until width) {
                val cur = rawMatrix[yi][xi] ?: continue
                if (xi + 1 < width) {
                    val right = rawMatrix[yi][xi + 1]
                    if (right != null && cur.value * right.value < 0f &&
                        (abs(cur.value) > 4f || abs(right.value) > 4f || cur.z > 100f || right.z > 100f)
                    ) {
                        polarityFlipCount++
                    }
                }
                if (yi + 1 < height) {
                    val down = rawMatrix[yi + 1][xi]
                    if (down != null && cur.value * down.value < 0f &&
                        (abs(cur.value) > 4f || abs(down.value) > 4f || cur.z > 100f || down.z > 100f)
                    ) {
                        polarityFlipCount++
                    }
                }
            }
        }

        // Build ScanPoint 2D matrix
        val now = System.currentTimeMillis()
        val points = List(height) { yi ->
            List(width) { xi ->
                val sample = rawMatrix[yi][xi]
                if (sample == null || (sample.z == 0f && sample.value == 0f)) {
                    ScanPoint(
                        x = xi,
                        y = yi,
                        rawSignal = 48000,
                        filteredSignal = 0f,
                        depth = 0f,
                        classification = TargetClassification.NEUTRAL,
                        confidence = 0,
                        timestamp = now
                    )
                } else {
                    val normSignal = (sample.value * signalScale).coerceIn(-100f, 100f)
                    val zScore = if (valueStdDev > 0.01f) (sample.value - valueMean) / valueStdDev else 0f
                    val classification = classifyCsvCell(sample.value, sample.z, zScore)
                    val depthProxy = (ln(1f + sample.z.coerceAtLeast(0f)) * 0.78f).coerceIn(0.35f, 6.0f)
                    val conf = (62 + min(36, (abs(zScore) * 14f).roundToInt() + if (sample.z > 150f) 12 else 0))
                        .coerceIn(50, 98)

                    ScanPoint(
                        x = xi,
                        y = yi,
                        rawSignal = (48000f + sample.value * 11.8f).roundToInt(),
                        filteredSignal = normSignal,
                        depth = depthProxy,
                        classification = classification,
                        confidence = conf,
                        timestamp = now
                    )
                }
            }
        }

        // 6-Band Longitudinal Profile (dividing width into up to 6 equal bands, matching 8-col scan strips)
        val bandCount = if (width >= 12) 6 else 1
        val bandSize = max(1, width / bandCount)
        val bandSummaries = mutableListOf<CsvBandSummary>()
        for (b in 0 until bandCount) {
            val startIdx = b * bandSize
            val endIdx = if (b == bandCount - 1) width - 1 else min(width - 1, startIdx + bandSize - 1)
            val bandSamples = mutableListOf<CsvRawSample>()
            for (yi in 0 until height) {
                for (xi in startIdx..endIdx) {
                    val s = rawMatrix[yi][xi] ?: continue
                    if (!(s.z == 0f && s.value == 0f)) bandSamples.add(s)
                }
            }
            if (bandSamples.isEmpty()) continue
            val bz = bandSamples.map { it.z }.sorted()
            val bv = bandSamples.map { it.value }.sorted()
            val bMedZ = medianOfSorted(bz)
            val bMedV = medianOfSorted(bv)
            val bMaxZ = bz.last()
            val satCount = bandSamples.count { it.value <= -172f }

            val behavior = when {
                satCount >= bandSamples.size / 2 -> "Taban Doyumu / Yoğun Boşluk (-172.8)"
                bMaxZ >= 300f -> "Yüksek Z Geçiş & Dipol Duvarı (Z≤${bMaxZ.toInt()})"
                bMedV >= 35f -> "Yüksek İletkenlik / Metalik-Mineral Baskın"
                bMedV <= -40f -> "Negatif Boşluk / Kavitasyon Zonu"
                bMedV in -10f..15f -> "Düşük Genlik / Geçiş Koridoru"
                else -> "Dengeli Pozitif Zemin Yanıtı"
            }

            bandSummaries.add(
                CsvBandSummary(
                    bandIndex = b + 1,
                    xRangeLabel = String.format(Locale.US, "X %.1f–%.1f", xs[startIdx], xs[endIdx]),
                    minZ = bz.first(),
                    maxZ = bMaxZ,
                    medianZ = bMedZ,
                    minValue = bv.first(),
                    maxValue = bv.last(),
                    medianValue = bMedV,
                    dominantBehavior = behavior
                )
            )
        }

        // Deep Anomaly & Cluster Findings
        val findings = extractDeepFindings(
            rawMatrix = rawMatrix,
            xs = xs,
            ys = ys,
            valueMean = valueMean,
            valueStdDev = valueStdDev,
            stepXMeters = stepXMeters,
            stepYMeters = stepYMeters
        )

        // Executive summary & recommendations
        val dominantPolar = if (negativeCount > positiveCount) {
            "Negatif/Boşluk ağırlıklı (%${(negativeCount * 100f / validSamples.size.coerceAtLeast(1)).roundToInt()} negatif)"
        } else {
            "Pozitif/İletken ağırlıklı (%${(positiveCount * 100f / validSamples.size.coerceAtLeast(1)).roundToInt()} pozitif)"
        }

        val summary = buildString {
            append("${width}×${height} matris (${samples.size} toplam, ${validSamples.size} geçerli okuma, $deadCount başlangıç sıfır hücresi). ")
            append("Sinyal dağılımı $dominantPolar; Value medyanı ${format2(valueMedian)} [${format2(values.minOrNull() ?: 0f)} .. ${format2(values.maxOrNull() ?: 0f)}], ")
            append("Z medyanı ${format2(zMedian)} (tepe Z = ${format2(zMax)}). ")
            if (saturatedFloorCount > 0) {
                append("$saturatedFloorCount hücrede sensör taban doyumu (Value = -172.80, Z = 5.56) gözlendi. ")
            }
            if (highImpedanceCount > 0) {
                append("$highImpedanceCount noktada yüksek empedans/kutuplanma kırılması (Z > 150) ve $polarityFlipCount polarite geçiş sınırı tespit edildi.")
            }
        }

        val recommendations = buildList {
            if (deadCount > 0) {
                add("X=0..47.7, Y=0 hattındaki $deadCount adet (Z=0, Value=0) kalibrasyon-başlangıç hücresi analizde gürültüden izole edildi.")
            }
            if (calProdMedian > 900f) {
                add("Matris |Z × Value| ≈ ${calProdMedian.roundToInt()} ters orantı yasasını izliyor; Z > 150 tepeleri polarite sıfır-geçiş duvarlarını işaret eder.")
            }
            if (saturatedFloorCount > 20) {
                add("Bantlarda $saturatedFloorCount hücre -172.80 taban sınırına kilitlenmiş; saha kazancı (gain) düşürülerek veya zemin balansı yenilenerek tekrar geçilmesi önerilir.")
            }
            findings.firstOrNull { it.category.contains("Empedans") }?.let { topZ ->
                add("En keskin yapısal kırılma ${topZ.xRange}, ${topZ.yRange} bölgesinde (tepe Z=${format2(topZ.peakZ)}, (${format1(topZ.peakRawX)}, ${format1(topZ.peakRawY)})) doğrulama taraması gerektirir.")
            }
            findings.firstOrNull { it.classification == TargetClassification.PRECIOUS || it.classification == TargetClassification.FERROUS }?.let { topPos ->
                add("En yüksek iletkenlik kümesi (${format1(topPos.peakRawX)}, ${format1(topPos.peakRawY)}) koordinatında Value=${format2(topPos.peakValue)} (Z-Score ${String.format(Locale.US, "%+.2f", topPos.zScore)}) ile ölçüldü.")
            }
        }

        val report = CsvDeepAnalysisReport(
            fileName = fileName,
            totalRows = samples.size,
            validRows = validSamples.size,
            deadCalibrationCells = deadCount,
            width = width,
            height = height,
            rawXRange = Pair(xs.first(), xs.last()),
            rawYRange = Pair(ys.first(), ys.last()),
            stepXRaw = stepXRaw,
            stepYRaw = stepYRaw,
            valueMin = values.minOrNull() ?: 0f,
            valueMax = values.maxOrNull() ?: 0f,
            valueMean = valueMean,
            valueMedian = valueMedian,
            valueStdDev = valueStdDev,
            valueMad = valueMad,
            zMin = zMin,
            zMax = zMax,
            zMean = zMean,
            zMedian = zMedian,
            positiveCount = positiveCount,
            negativeCount = negativeCount,
            strongVoidCount = strongVoidCount,
            saturatedFloorCount = saturatedFloorCount,
            highImpedanceCount = highImpedanceCount,
            polarityFlipCount = polarityFlipCount,
            calibrationProductMedian = calProdMedian,
            bandSummaries = bandSummaries,
            findings = findings,
            executiveSummary = summary,
            fieldRecommendations = recommendations
        )

        return ScanGrid(
            id = id,
            title = displayTitle,
            date = "CSV Saha Kaydı",
            width = width,
            height = height,
            stepDistanceMeters = stepXMeters,
            stepYMeters = stepYMeters,
            points = points,
            groundBalance = 48000,
            sensorType = "CSV 3D Gradiometre / Empedans Matrisi ($fileName)",
            locationName = locationName,
            notes = summary,
            isCsvImport = true,
            sourceFileName = fileName,
            rawXMin = xs.first(),
            rawXMax = xs.last(),
            rawYMin = ys.first(),
            rawYMax = ys.last(),
            csvReport = report
        )
    }

    fun detectCsvAnomalies(grid: ScanGrid, soilType: SoilType = SoilType.STANDARD): List<DetectedAnomaly> {
        val report = grid.csvReport ?: return ScanGenerators.detectAnomalies(grid, soilType)
        val step = (grid.stepDistanceMeters + grid.stepYMeters) * 0.5f

        return report.findings.take(12).mapIndexed { index, f ->
            val depthCalc = SignalDepthCalculator.calculateAnomalyDepth(
                grid = grid,
                centerX = f.peakGridX,
                centerY = f.peakGridY,
                soilType = soilType
            )
            val estDepth = ((depthCalc.estimatedDepthMeters + f.estimatedDepthMeters) * 0.5f).coerceIn(0.35f, 7.5f)
            val vol = (PI * (depthCalc.fwhmMeters * 0.5).pow(2.0) * 1.4).toFloat().coerceIn(0.12f, 25f)
            val sigPct = ((abs(f.peakValue).coerceAtMost(100f))
                .coerceAtLeast(if (f.peakZ > 150f) min(95f, f.peakZ / 10f) else 25f)).roundToInt()

            DetectedAnomaly(
                id = "csv-anom-$index-${f.peakGridX}-${f.peakGridY}",
                title = f.title,
                classification = f.classification,
                xMeters = f.peakGridX * grid.stepDistanceMeters,
                yMeters = f.peakGridY * grid.stepYMeters,
                gridX = f.peakGridX,
                gridY = f.peakGridY,
                depthMeters = estDepth,
                volumeM3 = vol,
                signalStrength = sigPct,
                description = "${f.interpretation} [Ham Konum: (${format1(f.peakRawX)}, ${format1(f.peakRawY)}), Z=${format2(f.peakZ)}, Val=${format2(f.peakValue)}]",
                peakAmplitude = f.peakValue,
                fwhmMeters = max(depthCalc.fwhmMeters, step * 1.2f),
                minDepthMeters = estDepth * 0.84f,
                maxDepthMeters = estDepth * 1.16f,
                depthConfidence = (74 + min(22, (abs(f.zScore) * 6f).roundToInt())).coerceIn(68, 96),
                calculationFormula = "CSV Z-Score & Empedans Geçiş Analizi"
            )
        }
    }

    private fun extractDeepFindings(
        rawMatrix: Array<Array<CsvRawSample?>>,
        xs: List<Float>,
        ys: List<Float>,
        valueMean: Float,
        valueStdDev: Float,
        stepXMeters: Float,
        stepYMeters: Float
    ): List<CsvClusterFinding> {
        val height = ys.size
        val width = xs.size
        val findings = mutableListOf<CsvClusterFinding>()

        fun cluster(
            category: String,
            predicate: (CsvRawSample) -> Boolean,
            rankByZ: Boolean,
            classificationFor: (List<CsvRawSample>, CsvRawSample) -> TargetClassification,
            titleFor: (List<CsvRawSample>, CsvRawSample) -> String
        ) {
            val visited = Array(height) { BooleanArray(width) }
            val clusters = mutableListOf<List<Pair<Pair<Int, Int>, CsvRawSample>>>()

            for (yi in 0 until height) {
                for (xi in 0 until width) {
                    if (visited[yi][xi]) continue
                    val s = rawMatrix[yi][xi] ?: continue
                    if (s.z == 0f && s.value == 0f) continue
                    if (!predicate(s)) continue

                    val stack = ArrayDeque<Pair<Int, Int>>()
                    val comp = mutableListOf<Pair<Pair<Int, Int>, CsvRawSample>>()
                    visited[yi][xi] = true
                    stack.addLast(xi to yi)

                    while (stack.isNotEmpty()) {
                        val (cx, cy) = stack.removeLast()
                        val cs = rawMatrix[cy][cx] ?: continue
                        comp.add((cx to cy) to cs)

                        for (dy in -1..1) {
                            for (dx in -1..1) {
                                if (dx == 0 && dy == 0) continue
                                val nx = cx + dx
                                val ny = cy + dy
                                if (nx in 0 until width && ny in 0 until height && !visited[ny][nx]) {
                                    val ns = rawMatrix[ny][nx] ?: continue
                                    if (!(ns.z == 0f && ns.value == 0f) && predicate(ns)) {
                                        visited[ny][nx] = true
                                        stack.addLast(nx to ny)
                                    }
                                }
                            }
                        }
                    }
                    clusters.add(comp)
                }
            }

            val sortedClusters = if (rankByZ) {
                clusters.sortedByDescending { comp -> comp.maxOf { it.second.z } }
            } else {
                clusters.sortedByDescending { comp -> comp.size * 2f + comp.maxOf { abs(it.second.value) } }
            }

            for ((idx, comp) in sortedClusters.take(4).withIndex()) {
                val samplesOnly = comp.map { it.second }
                val peakPair = if (rankByZ) {
                    comp.maxBy { it.second.z }
                } else {
                    comp.maxBy { abs(it.second.value) }
                }
                val (gx, gy) = peakPair.first
                val peak = peakPair.second
                val zScore = if (valueStdDev > 0.01f) (peak.value - valueMean) / valueStdDev else 0f

                val minX = samplesOnly.minOf { it.x }
                val maxX = samplesOnly.maxOf { it.x }
                val minY = samplesOnly.minOf { it.y }
                val maxY = samplesOnly.maxOf { it.y }

                val spanMeters = max((maxX - minX) * 0.01f, stepXMeters * 2f)
                val spanYMeters = max((maxY - minY) * 0.01f, stepYMeters)
                val geomDepth = ((spanMeters + spanYMeters) * 0.35f + ln(1f + peak.z) * 0.25f).coerceIn(0.6f, 6.2f)

                val cls = classificationFor(samplesOnly, peak)
                val title = titleFor(samplesOnly, peak)
                val interp = buildString {
                    append("${comp.size} hücreli küme • Tepe (${format1(peak.x)}, ${format1(peak.y)}), ")
                    append("Z=${format2(peak.z)}, Value=${format2(peak.value)} (Z-Score: ${String.format(Locale.US, "%+.2f", zScore)})")
                }

                findings.add(
                    CsvClusterFinding(
                        id = "$category-$idx-$gx-$gy",
                        category = category,
                        title = title,
                        classification = cls,
                        cellCount = comp.size,
                        xRange = "${format1(minX)}–${format1(maxX)}",
                        yRange = "${format1(minY)}–${format1(maxY)}",
                        peakRawX = peak.x,
                        peakRawY = peak.y,
                        peakGridX = gx,
                        peakGridY = gy,
                        peakZ = peak.z,
                        peakValue = peak.value,
                        zScore = zScore,
                        estimatedDepthMeters = geomDepth,
                        interpretation = interp
                    )
                )
            }
        }

        // 1. High-Impedance / Zero-Crossing Dipole Boundaries (Z > 150)
        cluster(
            category = "Yüksek Empedans / Kutuplanma Duvarı",
            predicate = { it.z > 150f },
            rankByZ = true,
            classificationFor = { _, _ -> TargetClassification.MINERAL },
            titleFor = { comp, peak ->
                if (peak.z >= 500f) "Kritik Empedans & Polarite Kırılması (Z=${peak.z.toInt()})"
                else "Yüksek Empedans Geçiş Zonu (${comp.size} hücre)"
            }
        )

        // 2. High-Conductivity / Positive Metallic Peaks (Value > 38)
        cluster(
            category = "Pozitif İletken / Metalik Odak",
            predicate = { it.value >= 38f },
            rankByZ = false,
            classificationFor = { _, peak ->
                if (peak.value >= 50f) TargetClassification.PRECIOUS else TargetClassification.FERROUS
            },
            titleFor = { comp, peak ->
                if (peak.value >= 50f) "Güçlü İletken / Değerli Hedef Adayı (${comp.size} hücre)"
                else "Pozitif İletken / Ferrous Kümesi (${comp.size} hücre)"
            }
        )

        // 3. Strong Negative / Void or Saturation Zones (Value < -40)
        cluster(
            category = "Negatif Boşluk / Doyum Zonu",
            predicate = { it.value < -40f },
            rankByZ = false,
            classificationFor = { _, _ -> TargetClassification.VOID },
            titleFor = { comp, peak ->
                if (peak.value <= -170f) "Geniş Negatif Doyum / Boşluk Alanı (${comp.size} hücre)"
                else "Negatif Boşluk / Kavitasyon Anomalisi (${comp.size} hücre)"
            }
        )

        return findings
    }

    private fun classifyCsvCell(value: Float, z: Float, zScore: Float): TargetClassification {
        return when {
            value < -40f -> TargetClassification.VOID
            value >= 50f || (value >= 38f && zScore >= 1.5f) -> TargetClassification.PRECIOUS
            value >= 25f -> TargetClassification.FERROUS
            z > 150f || value < -10f || value > 10f -> TargetClassification.MINERAL
            else -> TargetClassification.NEUTRAL
        }
    }

    private fun medianOfSorted(sorted: List<Float>): Float {
        if (sorted.isEmpty()) return 0f
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) * 0.5f
    }

    private fun stdDevOf(values: List<Float>, mean: Float): Float {
        if (values.size < 2) return 0f
        var sumSq = 0.0
        for (v in values) {
            val d = (v - mean).toDouble()
            sumSq += d * d
        }
        return sqrt(sumSq / (values.size - 1)).toFloat()
    }

    private fun format1(v: Float): String = String.format(Locale.US, "%.1f", v)
    private fun format2(v: Float): String = String.format(Locale.US, "%.2f", v)
}
