package com.lelesoftware.app.dsp

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * DSP (Digital Signal Processing) Mineral & Dynamic Baseline Filter
 *
 * Designed for Dual Fluxgate Gradiometer probes.
 * Suppresses high mineral ground clutter (wet clay, iron-rich rock, basalt, ceramic tiles)
 * using a Moving Average / Median dynamic baseline filter combined with
 * a tunable Z-Score anomaly detection threshold.
 */
class DspMineralFilter(
    private val windowSize: Int = 11,
    private val epsilon: Float = 0.001f
) {
    // Sliding circular buffer of raw delta nT readings for dynamic baseline tracking
    private val buffer = mutableListOf<Float>()

    data class FilterOutput(
        val rawDelta: Float,
        val baselineMean: Float,
        val baselineStdDev: Float,
        val zScore: Float,
        val filteredDelta: Float,
        val isAnomaly: Boolean,
        val mineralNoiseSuppressed: Boolean
    )

    /**
     * Processes a single real-time sensor reading through the dynamic baseline and Z-Score filter.
     *
     * @param rawDeltaNt Raw difference between upper and lower fluxgate (Sensor B - Sensor A in nT)
     * @param zThreshold Sensitivity threshold (e.g., 1.5 to 4.5). Readings with |Z| < zThreshold
     *                   are suppressed as mineral background noise.
     * @param baselineTrackingSpeed Blending weight for fast or slow adaptive baseline adaptation
     */
    fun processReading(
        rawDeltaNt: Float,
        zThreshold: Float = 2.5f,
        baselineTrackingSpeed: Float = 0.15f
    ): FilterOutput {
        // Maintain sliding window buffer
        buffer.add(rawDeltaNt)
        if (buffer.size > windowSize) {
            buffer.removeAt(0)
        }

        // 1. Moving Median calculation (robust against spike outliers)
        val sorted = buffer.sorted()
        val median = if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2]
        } else {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
        }

        // 2. Moving Average (Mean) of the baseline
        val mean = buffer.average().toFloat()
        val baseline = mean * baselineTrackingSpeed + median * (1f - baselineTrackingSpeed)

        // 3. Moving Standard Deviation (Variance)
        var sumSquares = 0.0
        for (v in buffer) {
            val diff = (v - baseline).toDouble()
            sumSquares += diff * diff
        }
        val variance = (sumSquares / buffer.size.coerceAtLeast(1)).toFloat()
        val stdDev = sqrt(variance).coerceAtLeast(epsilon)

        // 4. Z-Score Anomaly Metric
        // Z = (x - μ) / σ
        val zScore = (rawDeltaNt - baseline) / stdDev

        // 5. Mineral Clutter Suppression based on User Threshold
        val isAnomaly = abs(zScore) >= zThreshold

        val filteredDelta = if (isAnomaly) {
            // Signal exceeds threshold: pass through signal minus background baseline
            rawDeltaNt - baseline
        } else {
            // Mineral ground clutter: aggressively suppress towards zero
            val suppressionRatio = (abs(zScore) / zThreshold).coerceIn(0f, 1f)
            (rawDeltaNt - baseline) * (suppressionRatio * suppressionRatio)
        }

        return FilterOutput(
            rawDelta = rawDeltaNt,
            baselineMean = baseline,
            baselineStdDev = stdDev,
            zScore = zScore,
            filteredDelta = filteredDelta,
            isAnomaly = isAnomaly,
            mineralNoiseSuppressed = !isAnomaly && abs(rawDeltaNt) > 0.5f
        )
    }

    /**
     * Batch filters a 2D grid matrix of gradient signals.
     */
    fun filterGrid(
        matrix: Array<FloatArray>,
        zThreshold: Float = 2.5f
    ): Array<FloatArray> {
        val rows = matrix.size
        if (rows == 0) return emptyArray()
        val cols = matrix[0].size
        val result = Array(rows) { FloatArray(cols) }

        // Compute global baseline statistics
        val allValues = mutableListOf<Float>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                allValues.add(matrix[r][c])
            }
        }
        val sorted = allValues.sorted()
        val median = sorted[sorted.size / 2]
        val mean = allValues.average().toFloat()

        var sumSq = 0.0
        for (v in allValues) {
            val d = (v - median).toDouble()
            sumSq += d * d
        }
        val stdDev = sqrt((sumSq / allValues.size).toFloat()).coerceAtLeast(0.01f)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val raw = matrix[r][c]
                val z = (raw - median) / stdDev
                result[r][c] = if (abs(z) >= zThreshold) {
                    raw - median
                } else {
                    val suppression = (abs(z) / zThreshold)
                    (raw - median) * (suppression * suppression)
                }
            }
        }

        return result
    }

    fun reset() {
        buffer.clear()
    }

    companion object {
        /**
         * Python reference implementation for offline server/desktop processing
         */
        const val PYTHON_REFERENCE_CODE = """
import numpy as np

class DspMineralFilterPython:
    '''
    Python DSP Mineral & Z-Score Filter for Nicea/LELE Fluxgate Gradiometer
    '''
    def __init__(self, window_size=11, z_threshold=2.5):
        self.window_size = window_size
        self.z_threshold = z_threshold
        self.buffer = []

    def process_sample(self, raw_delta):
        self.buffer.append(raw_delta)
        if len(self.buffer) > self.window_size:
            self.buffer.pop(0)

        data = np.array(self.buffer)
        median = np.median(data)
        std_dev = max(np.std(data), 0.001)

        z_score = (raw_delta - median) / std_dev
        is_anomaly = abs(z_score) >= self.z_threshold

        if is_anomaly:
            filtered_delta = raw_delta - median
        else:
            suppression = (abs(z_score) / self.z_threshold) ** 2
            filtered_delta = (raw_delta - median) * suppression

        return {
            "filtered_delta": filtered_delta,
            "z_score": z_score,
            "is_anomaly": is_anomaly,
            "baseline": median
        }

    def filter_2d_grid(self, grid_matrix):
        median = np.median(grid_matrix)
        std_dev = max(np.std(grid_matrix), 0.01)
        z_matrix = (grid_matrix - median) / std_dev

        mask = np.abs(z_matrix) >= self.z_threshold
        suppression_factor = (np.abs(z_matrix) / self.z_threshold) ** 2

        filtered_grid = np.where(
            mask,
            grid_matrix - median,
            (grid_matrix - median) * suppression_factor
        )
        return filtered_grid
"""
    }
}
