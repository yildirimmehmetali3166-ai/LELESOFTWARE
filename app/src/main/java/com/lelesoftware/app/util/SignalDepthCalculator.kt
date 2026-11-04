package com.lelesoftware.app.util

import com.lelesoftware.app.model.DepthCalculationResult
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ScanPoint
import com.lelesoftware.app.model.SoilType
import com.lelesoftware.app.model.TargetClassification
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * SignalDepthCalculator
 *
 * Jeofiziksel Manyetometri & Radyasyon Gradyometrisi prensiplerine dayalı
 * 2D Sinyal Genliği (Amplitude) ve FWHM (Full Width at Half Maximum)
 * derinlik hesaplama motoru.
 *
 * Formülasyon:
 * 1. Dipol Alanı Sönümleme Yasası (Inverse Cube Law): B(z) ∝ M / z³
 * 2. Peters' Half-Slope & Half-Width Kuralı: z_FWHM ≈ 0.52 - 0.60 * W_half
 * 3. Zemin Dielektrik ve Sönümleme Çarpanı (Soil Attenuation Factor)
 */
object SignalDepthCalculator {

    /**
     * Verilen anomali merkezi (gridX, gridY) için 2D genlik matrisini analiz ederek
     * derinlik, FWHM, güvenilirlik ve sönümleme profilini hesaplar.
     */
    fun calculateAnomalyDepth(
        grid: ScanGrid,
        centerX: Int,
        centerY: Int,
        soilType: SoilType = SoilType.STANDARD
    ): DepthCalculationResult {
        val width = grid.width
        val height = grid.height
        val step = grid.stepDistanceMeters

        val cx = centerX.coerceIn(0, width - 1)
        val cy = centerY.coerceIn(0, height - 1)
        val centerPoint = grid.points[cy][cx]
        val peakVal = centerPoint.filteredSignal
        val isCavity = peakVal < 0f

        // 1. Zemin gürültü seviyesi (Baseline Noise) hesaplama
        // Merkezden 2-3 hücre uzaktaki çevre noktaların medyan/ortalaması
        val perimeterValues = mutableListOf<Float>()
        for (dy in -3..3) {
            for (dx in -3..3) {
                val r = sqrt((dx * dx + dy * dy).toFloat())
                if (r in 2.0f..3.5f) {
                    val nx = cx + dx
                    val ny = cy + dy
                    if (nx in 0 until width && ny in 0 until height) {
                        perimeterValues.add(grid.points[ny][nx].filteredSignal)
                    }
                }
            }
        }

        val baseNoise = if (perimeterValues.isNotEmpty()) {
            perimeterValues.sorted()[perimeterValues.size / 2]
        } else {
            0f
        }

        // Net tepe genliği (Delta Amplitude)
        val deltaAmp = abs(peakVal - baseNoise).coerceAtLeast(4.0f)
        val halfAmpThreshold = if (isCavity) {
            baseNoise - (deltaAmp * 0.50f)
        } else {
            baseNoise + (deltaAmp * 0.50f)
        }

        // 2. FWHM (Full Width at Half Maximum) Hesaplama - X ve Y eksenlerinde
        val widthX = calculateAxisFwhm(
            points = (0 until width).map { grid.points[cy][it].filteredSignal },
            centerIndex = cx,
            threshold = halfAmpThreshold,
            isCavity = isCavity,
            stepMeters = step
        )

        val widthY = calculateAxisFwhm(
            points = (0 until height).map { grid.points[it][cx].filteredSignal },
            centerIndex = cy,
            threshold = halfAmpThreshold,
            isCavity = isCavity,
            stepMeters = step
        )

        val fwhmMeters = ((widthX + widthY) / 2.0f).coerceAtLeast(step * 0.9f)

        // 3. Peters' Half-Slope Jeofizik Kuralı ile Derinlik
        // Metalik dipollerde z ≈ 0.52 * FWHM, boşluk/oda yapılarında z ≈ 0.60 * FWHM
        val zFwhm = if (isCavity) {
            fwhmMeters * 0.62f
        } else {
            fwhmMeters * 0.52f
        }

        // 4. Sinyal Genliği Sönümleme İnversiyonu (Dipole Amplitude Inversion)
        // Manyetik dipol B ∝ 1/z³ => z ∝ (100 / Amp)^(1/3)
        // Boşluk hacimleri B ∝ 1/z² => z ∝ (100 / Amp)^(1/2.2)
        val zAmp = if (isCavity) {
            1.65f * (100.0f / deltaAmp.coerceAtLeast(8f)).pow(0.40f)
        } else {
            1.20f * (100.0f / deltaAmp.coerceAtLeast(8f)).pow(0.35f)
        }

        // 5. Ağırlıklı Ortalama ve Zemin Tipi Çarpanı
        val combinedRawDepth = (zFwhm * 0.65f + zAmp * 0.35f)
        val estimatedDepth = (combinedRawDepth * soilType.attenuationFactor).coerceIn(0.25f, 6.50f)

        // 6. Güvenilirlik (Confidence) & Hata Payı
        val symmetryRatio = (min(widthX, widthY) / max(widthX, widthY)).coerceIn(0.4f, 1.0f)
        val snrFactor = (deltaAmp / (abs(baseNoise) + 3.0f)).coerceIn(1.0f, 6.0f)
        val confidence = (68 + (symmetryRatio * 18) + (snrFactor * 2.5f)).roundToInt().coerceIn(65, 98)

        val marginPercent = ((100 - confidence) / 120.0f).coerceIn(0.08f, 0.28f)
        val minDepth = (estimatedDepth * (1.0f - marginPercent)).coerceAtLeast(0.15f)
        val maxDepth = (estimatedDepth * (1.0f + marginPercent))

        // 7. Derinlik Penetrasyon Eğrisi (0m .. 5m aralığında teorik genlik düşüşü)
        val profileCurve = mutableListOf<Pair<Float, Float>>()
        val depthSteps = 12
        for (i in 0..depthSteps) {
            val d = (i * 5.0f / depthSteps)
            // Sönümleme modeli: A(z) = A0 / (1 + (z / z_est)^2.8)
            val att = deltaAmp / (1.0f + (d / estimatedDepth).pow(2.8f))
            profileCurve.add(Pair(d, att))
        }

        val peakNanoTesla = (deltaAmp * 11.8f)

        return DepthCalculationResult(
            estimatedDepthMeters = estimatedDepth,
            minDepthMeters = minDepth,
            maxDepthMeters = maxDepth,
            peakAmplitudePercent = peakVal,
            peakAmplitudeNanoTesla = peakNanoTesla,
            baseNoisePercent = baseNoise,
            netDeltaAmplitude = deltaAmp,
            fwhmMeters = fwhmMeters,
            soilType = soilType,
            confidencePercent = confidence,
            method = if (isCavity) "Peters Half-Slope (Boşluk/Oda Modeli)" else "Dipol FWHM & Genlik İnversiyonu",
            profileCurve = profileCurve
        )
    }

    /**
     * Verilen herhangi bir tekil grid hücresi için anlık derinlik tahmini yapar.
     */
    fun estimateCellDepth(
        grid: ScanGrid,
        x: Int,
        y: Int,
        soilType: SoilType = SoilType.STANDARD
    ): DepthCalculationResult {
        return calculateAnomalyDepth(grid, x, y, soilType)
    }

    /**
     * Tek bir eksen boyunca eşik değerinin aşıldığı genişliği (FWHM) hesaplar.
     */
    private fun calculateAxisFwhm(
        points: List<Float>,
        centerIndex: Int,
        threshold: Float,
        isCavity: Boolean,
        stepMeters: Float
    ): Float {
        val size = points.size
        var leftIndex = centerIndex.toFloat()
        var rightIndex = centerIndex.toFloat()

        // Sola doğru tara
        for (i in centerIndex downTo 0) {
            val v = points[i]
            val crosses = if (isCavity) v >= threshold else v <= threshold
            if (crosses) {
                // Lineer interpolasyon
                val prev = if (i < centerIndex) points[i + 1] else v
                val frac = if (abs(v - prev) > 0.001f) {
                    abs(threshold - prev) / abs(v - prev)
                } else 0.5f
                leftIndex = (i + 1) - frac.coerceIn(0f, 1f)
                break
            }
            if (i == 0) leftIndex = 0f
        }

        // Sağa doğru tara
        for (i in centerIndex until size) {
            val v = points[i]
            val crosses = if (isCavity) v >= threshold else v <= threshold
            if (crosses) {
                val prev = if (i > centerIndex) points[i - 1] else v
                val frac = if (abs(v - prev) > 0.001f) {
                    abs(threshold - prev) / abs(v - prev)
                } else 0.5f
                rightIndex = (i - 1) + frac.coerceIn(0f, 1f)
                break
            }
            if (i == size - 1) rightIndex = (size - 1).toFloat()
        }

        val cellSpan = (rightIndex - leftIndex).coerceAtLeast(0.8f)
        return cellSpan * stepMeters
    }
}
