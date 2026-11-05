package com.lelesoftware.app.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lelesoftware.app.dsp.Bno085KalmanFusion
import com.lelesoftware.app.dsp.DspMineralFilter
import com.lelesoftware.app.model.DeviceStatus
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ScanPoint
import com.lelesoftware.app.util.AudioSynthesizer
import com.lelesoftware.app.util.ScanGenerators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ScanSimulationUiState
 *
 * Immutable state representing sensor hardware telemetry, DSP filtering,
 * real-time waveform buffer, and the 2D survey matrix.
 */
data class ScanSimulationUiState(
    val isScanning: Boolean = false,
    val isBleConnected: Boolean = true,
    val deviceStatus: DeviceStatus = DeviceStatus(),
    val gridWidth: Int = 10,
    val gridHeight: Int = 10,
    val stepDistanceMeters: Float = 0.5f,
    val currentStepX: Int = 0,
    val currentStepY: Int = 0,
    val points: List<List<ScanPoint>> = emptyList(),
    val signalSamples: List<SignalSample> = emptyList(),
    val isGraphStreaming: Boolean = true,
    val walkPattern: String = "zigzag",
    val groundBalance: Int = 48100,
    val zThreshold: Float = 2.5f,
    val isImuEnabled: Boolean = true,
    val dspOutput: DspMineralFilter.FilterOutput? = null,
    val imuOutput: Bno085KalmanFusion.FusionResult? = null
)

/**
 * ScanSimulationViewModel
 *
 * Thread Yönetimi & Sensör Akışı:
 * 1. BLE 50 Hz sensör örnekleme ve matris işleme adımları Main Thread yerine
 *    kesinlikle `Dispatchers.Default` CoroutineDispatcher üzerinde çalışır.
 * 2. Z-Score Dinamik Baseline filtresi ve BNO085 Kalman Sensor Fusion hesaplamaları
 *    CPU-bound thread üzerinde izole edilerek Main Thread'in 60 FPS akıcılığı korunur.
 * 3. StateFlow kullanılarak reaktif ve immutability odaklı UI senkronizasyonu sağlanır.
 */
class ScanSimulationViewModel : ViewModel() {

    private val gridW = 10
    private val gridH = 10
    private val stepDist = 0.5f

    private val dspFilter = DspMineralFilter(windowSize = 11)
    private val imuFusion = Bno085KalmanFusion()

    private val _uiState = MutableStateFlow(
        ScanSimulationUiState(
            gridWidth = gridW,
            gridHeight = gridH,
            stepDistanceMeters = stepDist,
            points = ScanGenerators.generateEmptyLiveGrid(gridW, gridH, stepDist).points
        )
    )
    val uiState: StateFlow<ScanSimulationUiState> = _uiState.asStateFlow()

    // Pre-computed target field anomalies for realistic magnetic responses
    private val hiddenTargets = mutableMapOf<String, Float>().apply {
        val cx = (gridW * 0.6f).toInt()
        val cy = (gridH * 0.6f).toInt()
        val vx = (gridW * 0.3f).toInt()
        val vy = (gridH * 0.4f).toInt()

        for (y in 0 until gridH) {
            for (x in 0 until gridW) {
                val dMetal = sqrt((x - cx).toFloat().pow(2) + (y - cy).toFloat().pow(2))
                val dVoid = sqrt((x - vx).toFloat().pow(2) + (y - vy).toFloat().pow(2))
                var s = ((x * 7 + y * 13) % 7 - 3f)
                if (dMetal < 2.0f) {
                    s += 92f * exp(-dMetal * 1.6f)
                } else if (dVoid < 2.5f) {
                    s -= 82f * exp(-dVoid * 1.2f)
                }
                this["$x,$y"] = s.coerceIn(-100f, 100f)
            }
        }
    }

    private var scanningLoopJob: Job? = null
    private var bleSensorStreamJob: Job? = null

    init {
        startBle50HzSensorStream()
    }

    /**
     * BLE 50 Hz Hardware Telemetry Coroutine on Dispatchers.Default
     */
    private fun startBle50HzSensorStream() {
        bleSensorStreamJob?.cancel()
        bleSensorStreamJob = viewModelScope.launch(Dispatchers.Default) {
            var tick = 0
            while (isActive) {
                // 50 Hz telemetry stream = 20ms period
                delay(20)
                tick++

                val currentState = _uiState.value
                val gradBase = currentState.deviceStatus.gradientDelta.toFloat()
                val noise = (sin(tick * 0.4) * 2.2 + (Math.random() - 0.5) * 3.0).toFloat()
                val currentDelta = gradBase + noise

                val sensorAVal = (currentState.groundBalance + currentDelta * 0.5f).roundToInt()
                val sensorBVal = (currentState.groundBalance - currentDelta * 0.5f).roundToInt()

                // Execute DSP Filter on background thread
                val dspResult = dspFilter.processReading(
                    rawDeltaNt = currentDelta,
                    zThreshold = currentState.zThreshold
                )

                // Execute IMU Sensor Fusion on background thread
                val rawPitch = currentState.deviceStatus.currentPitch
                val rawRoll = currentState.deviceStatus.currentRoll
                val rawYaw = (45f + (currentState.currentStepX * 3.5f)) % 360f

                val imuResult = if (currentState.isImuEnabled) {
                    imuFusion.update(
                        pitchDeg = rawPitch,
                        rollDeg = rawRoll,
                        yawDeg = rawYaw,
                        rawDeltaNt = currentDelta,
                        stepDistanceMeters = currentState.stepDistanceMeters
                    )
                } else null

                // Update signal waveform buffer every ~60ms (3 ticks)
                if (tick % 3 == 0 && currentState.isGraphStreaming) {
                    val sample = SignalSample(
                        timestampMs = System.currentTimeMillis(),
                        sensorA = sensorAVal.toFloat(),
                        sensorB = sensorBVal.toFloat(),
                        gradientDelta = dspResult.filteredDelta
                    )
                    _uiState.update { state ->
                        state.copy(
                            signalSamples = (state.signalSamples + sample).takeLast(100),
                            dspOutput = dspResult,
                            imuOutput = imuResult,
                            deviceStatus = state.deviceStatus.copy(
                                currentSensorA = sensorAVal,
                                currentSensorB = sensorBVal,
                                gradientDelta = currentDelta.roundToInt()
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Start/Pause automated step-by-step surveying
     */
    fun toggleScanning() {
        val willScan = !_uiState.value.isScanning
        _uiState.update { it.copy(isScanning = willScan) }

        if (willScan) {
            startAutoStepLoop()
        } else {
            scanningLoopJob?.cancel()
        }
    }

    private fun startAutoStepLoop() {
        scanningLoopJob?.cancel()
        scanningLoopJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive && _uiState.value.isScanning) {
                delay(380) // 380 ms per walking step
                if (_uiState.value.currentStepY >= gridH) {
                    _uiState.update { it.copy(isScanning = false) }
                    break
                }
                takeSingleStepInternal()
            }
        }
    }

    fun takeSingleStep() {
        viewModelScope.launch(Dispatchers.Default) {
            takeSingleStepInternal()
        }
    }

    private suspend fun takeSingleStepInternal() = withContext(Dispatchers.Default) {
        val state = _uiState.value
        val x = state.currentStepX
        val y = state.currentStepY

        if (y >= gridH) return@withContext

        val targetSignal = hiddenTargets["$x,$y"] ?: 0f
        val raw = (targetSignal * 12.0f + state.groundBalance).roundToInt()
        val absSig = abs(targetSignal)
        val estimatedDepth = if (absSig > 20f) {
            (1.25f * (100f / absSig).pow(0.35f)).coerceIn(0.4f, 5.0f)
        } else 0f

        // Sound effect (runs safely)
        AudioSynthesizer.playSignalTone(targetSignal)

        // Safe matrix point update
        val updatedPoints = state.points.mapIndexed { py, row ->
            row.mapIndexed { px, p ->
                if (px == x && py == y) {
                    p.copy(
                        rawSignal = raw,
                        filteredSignal = targetSignal,
                        depth = estimatedDepth,
                        classification = ScanGenerators.classifySignal(targetSignal),
                        confidence = 92,
                        timestamp = System.currentTimeMillis()
                    )
                } else p
            }
        }

        // Advance walk coordinates
        var nextX = x
        var nextY = y

        if (state.walkPattern == "zigzag") {
            if (y % 2 == 0) {
                if (x + 1 < gridW) nextX = x + 1 else nextY = y + 1
            } else {
                if (x - 1 >= 0) nextX = x - 1 else nextY = y + 1
            }
        } else {
            if (x + 1 < gridW) {
                nextX = x + 1
            } else {
                nextX = 0
                nextY = y + 1
            }
        }

        val scanEnded = nextY >= gridH

        _uiState.update { s ->
            s.copy(
                points = updatedPoints,
                currentStepX = nextX,
                currentStepY = nextY,
                isScanning = if (scanEnded) false else s.isScanning,
                deviceStatus = s.deviceStatus.copy(
                    gradientDelta = (targetSignal * 12f).roundToInt(),
                    currentPitch = 1.0f + (nextX % 3 - 1) * 0.4f,
                    currentRoll = -0.3f + (nextY % 3 - 1) * 0.3f
                )
            )
        }
    }

    fun setWalkPattern(pattern: String) {
        _uiState.update { it.copy(walkPattern = pattern) }
    }

    fun setZThreshold(threshold: Float) {
        _uiState.update { it.copy(zThreshold = threshold) }
    }

    fun setImuEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isImuEnabled = enabled) }
    }

    fun toggleGraphStreaming() {
        _uiState.update { it.copy(isGraphStreaming = !it.isGraphStreaming) }
    }

    fun clearSamples() {
        _uiState.update { it.copy(signalSamples = emptyList()) }
    }

    fun resetScan() {
        scanningLoopJob?.cancel()
        dspFilter.reset()
        imuFusion.reset()
        _uiState.update {
            it.copy(
                isScanning = false,
                currentStepX = 0,
                currentStepY = 0,
                points = ScanGenerators.generateEmptyLiveGrid(gridW, gridH, stepDist).points
            )
        }
    }

    fun buildCompletedGrid(): ScanGrid {
        val s = _uiState.value
        return ScanGrid(
            id = "scan-live-${System.currentTimeMillis()}",
            title = "Canlı Saha Taraması (Tamamlandı)",
            date = "2026-11-05 10:00",
            width = s.gridWidth,
            height = s.gridHeight,
            stepDistanceMeters = s.stepDistanceMeters,
            points = s.points,
            groundBalance = s.groundBalance,
            sensorType = "Dual FGM Fluxgate (50Hz BLE)",
            locationName = "Canlı Ölçüm Sahası",
            notes = "${s.gridWidth}x${s.gridHeight} adım canlı sensör probu ile kaydedildi."
        )
    }

    override fun onCleared() {
        super.onCleared()
        scanningLoopJob?.cancel()
        bleSensorStreamJob?.cancel()
    }
}
