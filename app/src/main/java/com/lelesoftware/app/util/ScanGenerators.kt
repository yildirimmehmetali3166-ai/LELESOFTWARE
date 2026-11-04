package com.lelesoftware.app.util

import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ScanPoint
import com.lelesoftware.app.model.SoilType
import com.lelesoftware.app.model.TargetClassification
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object ScanGenerators {

    fun classifySignal(value: Float): TargetClassification {
        return when {
            value < -40f -> TargetClassification.VOID
            value > 65f -> TargetClassification.PRECIOUS
            value > 25f -> TargetClassification.FERROUS
            value > 10f || value < -15f -> TargetClassification.MINERAL
            else -> TargetClassification.NEUTRAL
        }
    }

    fun generateAntikMezarOdasi(): ScanGrid {
        val width = 14
        val height = 14
        val step = 0.5f
        val points = mutableListOf<List<ScanPoint>>()
        val random = Random(42)

        for (y in 0 until height) {
            val row = mutableListOf<ScanPoint>()
            for (x in 0 until width) {
                val noise = (sin(x * 1.5) * cos(y * 1.2) * 4.0 + (random.nextDouble() - 0.5) * 5.0).toFloat()
                var signal = noise
                var depth = 0f

                // Void room centered around (6.5, 6.5)
                val dxRoom = x - 6.5f
                val dyRoom = y - 6.5f
                val distRoom = sqrt(dxRoom * dxRoom + dyRoom * dyRoom)

                if (distRoom < 3.2f) {
                    val roomStrength = (1f - distRoom / 3.2f)
                    signal = -80f * roomStrength + noise
                    depth = 2.8f + roomStrength * 0.8f
                }

                // Precious target inside room at (7, 6)
                val dxMetal = x - 7f
                val dyMetal = y - 6f
                val distMetal = sqrt(dxMetal * dxMetal + dyMetal * dyMetal)
                if (distMetal < 1.6f) {
                    val metalStrength = exp(-distMetal * 2.2f)
                    signal += metalStrength * 165f
                    if (metalStrength > 0.4f) {
                        depth = 2.95f
                    }
                }

                // Secondary ferrous nail at (4, 5)
                val dxIron = x - 4f
                val dyIron = y - 5f
                val distIron = sqrt(dxIron * dxIron + dyIron * dyIron)
                if (distIron < 1.0f) {
                    signal += (exp(-distIron * 3f) * 60f)
                }

                val filtered = signal.coerceIn(-100f, 100f)
                val raw = (filtered * 12.5f + 48200f).roundToInt()

                row.add(
                    ScanPoint(
                        x = x,
                        y = y,
                        rawSignal = raw,
                        filteredSignal = filtered,
                        depth = if (depth > 0f) depth else if (abs(filtered) > 15f) 1.5f else 0f,
                        classification = classifySignal(filtered),
                        confidence = (50 + abs(filtered) * 0.48f).roundToInt().coerceAtMost(98),
                        timestamp = System.currentTimeMillis() - (width * height - (y * width + x)) * 1000L
                    )
                )
            }
            points.add(row)
        }

        return ScanGrid(
            id = "scan-lahit-01",
            title = "LELE Jeofizik Sahası - Lahit & Mezar Odası",
            date = "2026-10-24 14:35",
            width = width,
            height = height,
            stepDistanceMeters = step,
            points = points,
            groundBalance = 48200,
            sensorType = "LELE Gradiometer 2-Sensör",
            locationName = "LELE SOFTWARE Araştırma Sahası A-1 (Doğu Sektörü)",
            notes = "2.8m derinlikte 2.5m x 2.5m mezar odası boşluğu ve oda içerisinde yüksek iletkenlikli metalik anomali."
        )
    }

    fun generateAltinKupu(): ScanGrid {
        val width = 12
        val height = 12
        val step = 0.5f
        val points = mutableListOf<List<ScanPoint>>()
        val random = Random(101)

        for (y in 0 until height) {
            val row = mutableListOf<ScanPoint>()
            for (x in 0 until width) {
                val noise = ((random.nextDouble() - 0.5) * 6.0 + sin(x * 0.8) * 3.0).toFloat()
                var signal = noise

                // Magnetic dipole at (6, 5)
                val dx = x - 6f
                val dy = y - 5f
                val dist = sqrt(dx * dx + dy * dy)

                if (dist < 3.0f) {
                    val peak = (94.0 * exp(-dist * 1.5)).toFloat()
                    val shadow = if (dy > 0.8f) (-18.0 * exp(-(dist - 1.8).pow(2.0))).toFloat() else 0f
                    signal = peak + shadow + noise
                }

                val filtered = signal.coerceIn(-100f, 100f)
                val raw = (filtered * 11.8f + 48150f).roundToInt()

                row.add(
                    ScanPoint(
                        x = x,
                        y = y,
                        rawSignal = raw,
                        filteredSignal = filtered,
                        depth = if (dist < 1.8f) 1.45f else 0f,
                        classification = classifySignal(filtered),
                        confidence = if (dist < 1.8f) 96 else 55,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            points.add(row)
        }

        return ScanGrid(
            id = "scan-gold-02",
            title = "Hedef Alan - Değerli Metal & Sikke Küpü",
            date = "2026-10-26 11:20",
            width = width,
            height = height,
            stepDistanceMeters = step,
            points = points,
            groundBalance = 48150,
            sensorType = "Gradiometer 2-Sensör",
            locationName = "Saha Parsel B-4 (Tarihi Yerleşim Sınırı)",
            notes = "1.45m derinlikte noktasal yüksek iletkenlikli altın/bronz hedef anomalisi. Tipik dipol tepkisi."
        )
    }

    fun generateTunelVeMagara(): ScanGrid {
        val width = 16
        val height = 12
        val step = 0.6f
        val points = mutableListOf<List<ScanPoint>>()
        val random = Random(202)

        for (y in 0 until height) {
            val row = mutableListOf<ScanPoint>()
            for (x in 0 until width) {
                val noise = ((random.nextDouble() - 0.5) * 7.0).toFloat()
                var signal = noise

                // Tunnel diagonal from (2, 2) to (14, 9)
                val distToLine = (abs(7f * x - 12f * y + 10f) / sqrt(49f + 144f))
                if (distToLine < 1.4f && x in 2..14) {
                    val tunnelFactor = 1f - distToLine / 1.4f
                    signal = -75f * tunnelFactor + noise
                }

                // Chamber at (13, 9)
                val distChamber = sqrt((x - 13f).pow(2) + (y - 9f).pow(2))
                if (distChamber < 2.5f) {
                    val chamberSignal = -88f * (1f - distChamber / 2.5f) + noise
                    if (chamberSignal < signal) signal = chamberSignal
                }

                val filtered = signal.coerceIn(-100f, 100f)
                val raw = (filtered * 10f + 48300f).roundToInt()

                row.add(
                    ScanPoint(
                        x = x,
                        y = y,
                        rawSignal = raw,
                        filteredSignal = filtered,
                        depth = if (distToLine < 1.4f) 3.4f else if (distChamber < 2.5f) 4.2f else 0f,
                        classification = classifySignal(filtered),
                        confidence = if (distToLine < 1.4f || distChamber < 2.5f) 92 else 60,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            points.add(row)
        }

        return ScanGrid(
            id = "scan-tunel-03",
            title = "Mahzen & Yeraltı Tüneli Hattı",
            date = "2026-10-27 16:50",
            width = width,
            height = height,
            stepDistanceMeters = step,
            points = points,
            groundBalance = 48300,
            sensorType = "Gradiometer 2-Sensör",
            locationName = "Eski Kale Etekleri - Galeri Girişi",
            notes = "3.4m derinlikte 12 metre uzunluğunda galeri/tünel ve tünelin bağlandığı mahzen boşluğu."
        )
    }

    fun generateEmptyLiveGrid(width: Int = 10, height: Int = 10, step: Float = 0.5f): ScanGrid {
        val points = mutableListOf<List<ScanPoint>>()
        for (y in 0 until height) {
            val row = mutableListOf<ScanPoint>()
            for (x in 0 until width) {
                row.add(
                    ScanPoint(
                        x = x,
                        y = y,
                        rawSignal = 48000,
                        filteredSignal = 0f,
                        depth = 0f,
                        classification = TargetClassification.NEUTRAL,
                        confidence = 0,
                        timestamp = 0L
                    )
                )
            }
            points.add(row)
        }

        return ScanGrid(
            id = "scan-live-${System.currentTimeMillis()}",
            title = "Canlı Saha Taraması (Yeni Oturum)",
            date = "2026-11-01 12:00",
            width = width,
            height = height,
            stepDistanceMeters = step,
            points = points,
            groundBalance = 48000,
            sensorType = "Gradiometer 2-Sensör",
            locationName = "Canlı Tarama Sahası",
            notes = "Sensör probu ile adım adım veri toplama modu."
        )
    }

    fun detectAnomalies(grid: ScanGrid, soilType: SoilType = SoilType.STANDARD): List<DetectedAnomaly> {
        val anomalies = mutableListOf<DetectedAnomaly>()
        val visited = mutableSetOf<String>()
        val width = grid.width
        val height = grid.height
        val step = grid.stepDistanceMeters

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pt = grid.points[y][x]
                val valSignal = pt.filteredSignal
                val key = "$x,$y"

                if (key in visited) continue

                if (abs(valSignal) > 40f) {
                    var isMax = true
                    var isMin = true

                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until width && ny in 0 until height) {
                                val nVal = grid.points[ny][nx].filteredSignal
                                if (nVal >= valSignal) isMax = false
                                if (nVal <= valSignal) isMin = false
                            }
                        }
                    }

                    if ((valSignal > 40f && isMax) || (valSignal < -40f && isMin)) {
                        for (dy in -2..2) {
                            for (dx in -2..2) {
                                visited.add("${x + dx},${y + dy}")
                            }
                        }

                        val isCavity = valSignal < 0f
                        val depthResult = SignalDepthCalculator.calculateAnomalyDepth(grid, x, y, soilType)
                        val depth = depthResult.estimatedDepthMeters
                        val volume = if (isCavity) {
                            (PI * (depthResult.fwhmMeters * 0.5).pow(2.0) * 1.8).toFloat()
                        } else {
                            (0.08f + abs(valSignal) * 0.003f)
                        }

                        val title = when {
                            isCavity -> "Yeraltı Boşluğu / Oda Anomalisi"
                            valSignal > 65f -> "Değerli Metal / Yüksek İletkenlik"
                            else -> "Metalik Kütle / Manyetik Dipol"
                        }

                        val desc = if (isCavity) {
                            String.format(
                                Locale.US,
                                "%.2fm [±%.2fm] tavan derinliği (Genlik: %.1f%%, FWHM: %.2fm).",
                                depth,
                                (depthResult.maxDepthMeters - depthResult.minDepthMeters) / 2f,
                                abs(valSignal),
                                depthResult.fwhmMeters
                            )
                        } else {
                            String.format(
                                Locale.US,
                                "%.2fm [±%.2fm] derinlikte %s yanıtı (Genlik: %.1f%%, %d nT).",
                                depth,
                                (depthResult.maxDepthMeters - depthResult.minDepthMeters) / 2f,
                                if (valSignal > 65f) "Altın/Bronz/Gümüş" else "Demir/Ferrous",
                                abs(valSignal),
                                depthResult.peakAmplitudeNanoTesla.roundToInt()
                            )
                        }

                        anomalies.add(
                            DetectedAnomaly(
                                id = "anom-$x-$y",
                                title = title,
                                classification = pt.classification,
                                xMeters = (x * step),
                                yMeters = (y * step),
                                gridX = x,
                                gridY = y,
                                depthMeters = depth,
                                volumeM3 = volume,
                                signalStrength = abs(valSignal).roundToInt(),
                                description = desc,
                                peakAmplitude = depthResult.peakAmplitudePercent,
                                fwhmMeters = depthResult.fwhmMeters,
                                minDepthMeters = depthResult.minDepthMeters,
                                maxDepthMeters = depthResult.maxDepthMeters,
                                depthConfidence = depthResult.confidencePercent,
                                calculationFormula = depthResult.method
                            )
                        )
                    }
                }
            }
        }

        return anomalies
    }
}
