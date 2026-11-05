package com.lelesoftware.app.dsp

import kotlin.math.cos
import kotlin.math.sin

/**
 * BNO085 9-Axis IMU Sensor Fusion & Kalman Drift Correction Engine.
 *
 * Responsibilities:
 * 1. Filters raw Pitch, Roll and Yaw (heading) data using discrete Kalman Filters to eliminate
 *    jitter caused by human walking steps.
 * 2. Trigonometric Tilt Compensation: Corrects artificial magnetic gradient deltas created when
 *    the vertical probe tilts away from the normal vector.
 * 3. 2D Dead-Reckoning Matrix Projection: Projects step distance and heading angle into
 *    accurate (X, Y) grid survey coordinates.
 */
class Bno085KalmanFusion(
    processNoise: Float = 0.008f,
    measurementNoise: Float = 0.06f
) {
    // Independent 1D Kalman Filters for Pitch, Roll, and Yaw angles
    private val pitchFilter = SingleAxisKalman(processNoise, measurementNoise)
    private val rollFilter = SingleAxisKalman(processNoise, measurementNoise)
    private val yawFilter = SingleAxisKalman(processNoise, measurementNoise)

    data class FusionResult(
        val rawPitch: Float,
        val rawRoll: Float,
        val rawYaw: Float,
        val filteredPitch: Float,
        val filteredRoll: Float,
        val filteredYaw: Float,
        val tiltCompensationFactor: Float,
        val compensatedDeltaNt: Float,
        val projectedStepX: Float,
        val projectedStepY: Float,
        val tiltWarning: Boolean
    )

    /**
     * Updates IMU attitude and computes tilt-compensated magnetic gradient and projected coordinates.
     *
     * @param pitchDeg Pitch angle in degrees from BNO085
     * @param rollDeg Roll angle in degrees from BNO085
     * @param yawDeg Yaw (azimuth heading) angle in degrees from BNO085
     * @param rawDeltaNt Raw magnetic gradient delta from dual fluxgate
     * @param stepDistanceMeters Nominal physical walking step size (e.g. 0.5m)
     * @param currentX Current 2D survey X coordinate in meters
     * @param currentY Current 2D survey Y coordinate in meters
     */
    fun update(
        pitchDeg: Float,
        rollDeg: Float,
        yawDeg: Float,
        rawDeltaNt: Float,
        stepDistanceMeters: Float = 0.5f,
        currentX: Float = 0f,
        currentY: Float = 0f
    ): FusionResult {
        // 1. Kalman state estimation
        val cleanPitch = pitchFilter.update(pitchDeg)
        val cleanRoll = rollFilter.update(rollDeg)
        val cleanYaw = yawFilter.update(yawDeg)

        // 2. Tilt Compensation
        // When vertical probe tilts by angle theta, B_z = B_vertical * cos(pitch) * cos(roll)
        // Inverse compensation recovers true vertical gradient
        val pitchRad = Math.toRadians(cleanPitch.toDouble()).toFloat()
        val rollRad = Math.toRadians(cleanRoll.toDouble()).toFloat()
        val cosFactor = (cos(pitchRad) * cos(rollRad)).coerceIn(0.5f, 1.0f)

        val compensatedDelta = rawDeltaNt / cosFactor
        val tiltWarning = (kotlin.math.abs(cleanPitch) > 12f || kotlin.math.abs(cleanRoll) > 12f)

        // 3. 2D Dead Reckoning Step Projection
        // Yaw 0° is Magnetic North (Y+), 90° is East (X+), 180° is South (Y-), 270° is West (X-)
        val yawRad = Math.toRadians(cleanYaw.toDouble()).toFloat()
        val stepDx = stepDistanceMeters * sin(yawRad)
        val stepDy = stepDistanceMeters * cos(yawRad)

        val nextX = currentX + stepDx
        val nextY = currentY + stepDy

        return FusionResult(
            rawPitch = pitchDeg,
            rawRoll = rollDeg,
            rawYaw = yawDeg,
            filteredPitch = cleanPitch,
            filteredRoll = cleanRoll,
            filteredYaw = cleanYaw,
            tiltCompensationFactor = cosFactor,
            compensatedDeltaNt = compensatedDelta,
            projectedStepX = nextX,
            projectedStepY = nextY,
            tiltWarning = tiltWarning
        )
    }

    fun reset() {
        pitchFilter.reset()
        rollFilter.reset()
        yawFilter.reset()
    }

    /**
     * Discrete Linear 1D Kalman Filter
     */
    private class SingleAxisKalman(
        private val q: Float, // Process noise covariance
        private val r: Float  // Measurement noise covariance
    ) {
        private var x = 0f  // Estimated state
        private var p = 1f  // Estimation error covariance
        private var initialized = false

        fun update(measurement: Float): Float {
            if (!initialized) {
                x = measurement
                initialized = true
                return x
            }

            // 1. Prediction update
            val pPredict = p + q

            // 2. Measurement update
            val k = pPredict / (pPredict + r) // Kalman Gain
            x += k * (measurement - x)
            p = (1f - k) * pPredict

            return x
        }

        fun reset() {
            x = 0f
            p = 1f
            initialized = false
        }
    }

    companion object {
        const val PYTHON_REFERENCE_CODE = """
import numpy as np

class Bno085KalmanFusionPython:
    '''
    Python BNO085 IMU Kalman Fusion & Drift Compensation for Nicea/LELE Probes
    '''
    def __init__(self, process_noise=0.008, measurement_noise=0.06):
        self.q = process_noise
        self.r = measurement_noise
        self.states = {'pitch': 0.0, 'roll': 0.0, 'yaw': 0.0}
        self.covariances = {'pitch': 1.0, 'roll': 1.0, 'yaw': 1.0}
        self.init = False

    def update_single_kalman(self, axis, measurement):
        p_pred = self.covariances[axis] + self.q
        k_gain = p_pred / (p_pred + self.r)
        self.states[axis] += k_gain * (measurement - self.states[axis])
        self.covariances[axis] = (1.0 - k_gain) * p_pred
        return self.states[axis]

    def process(self, pitch_deg, roll_deg, yaw_deg, raw_delta, step_dist=0.5, curr_x=0.0, curr_y=0.0):
        c_pitch = self.update_single_kalman('pitch', pitch_deg)
        c_roll = self.update_single_kalman('roll', roll_deg)
        c_yaw = self.update_single_kalman('yaw', yaw_deg)

        # Tilt compensation
        p_rad = np.radians(c_pitch)
        r_rad = np.radians(c_roll)
        cos_factor = max(np.cos(p_rad) * np.cos(r_rad), 0.5)
        compensated_delta = raw_delta / cos_factor

        # 2D projection
        y_rad = np.radians(c_yaw)
        dx = step_dist * np.sin(y_rad)
        dy = step_dist * np.cos(y_rad)

        return {
            "filtered_pitch": c_pitch,
            "filtered_roll": c_roll,
            "filtered_yaw": c_yaw,
            "compensated_delta": compensated_delta,
            "next_x": curr_x + dx,
            "next_y": curr_y + dy,
            "tilt_warning": (abs(c_pitch) > 12.0 or abs(c_roll) > 12.0)
        }
"""
    }
}
