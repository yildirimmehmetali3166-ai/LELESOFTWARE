package com.lelesoftware.app.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lelesoftware.app.dsp.Bno085KalmanFusion
import com.lelesoftware.app.dsp.DspMineralFilter
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.DeviceStatus
import com.lelesoftware.app.model.PaletteType
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ScanPoint
import com.lelesoftware.app.model.SoilType
import com.lelesoftware.app.model.TargetClassification
import com.lelesoftware.app.util.AudioSynthesizer
import com.lelesoftware.app.util.ScanGenerators
import com.lelesoftware.app.util.SignalDepthCalculator
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
import java.util.Locale
import kotlin.math.PI
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
 * real-time waveform buffer, discovered anomalies, and the 2D survey matrix.
 */
data class ScanSimulationUiState(
    val isScanning: Boolean = true,
    val autoLoop: Boolean = true,
    val scanSpeedMultiplier: Float = 1.0f,
    val sectorNumber: Int = 1,
    val isBleConnected: Boolean = true,
    val isAudioEnabled: Boolean = false,
    val deviceStatus: DeviceStatus = DeviceStatus(),
    val gridWidth: Int = 12,
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
    val imuOutput: Bno085KalmanFusion.FusionResult? = null,
    val activePalette: PaletteType = PaletteType.LELE_SPECTRUM,
    val showCellValues: Boolean = false,
    val smoothGrid: Boolean = false,
    val selectedCell: Pair<Int, Int>? = null,
    val visitedCount: Int = 0,
    val minSignalPct: Float = 0f,
    val maxSignalPct: Float = 0f,
    val avgAbsSignalPct: Float = 0f,
    val estimatedTargetDepthMeters: Float = 0f,
    val discoveredAnomalies: List<DetectedAnomaly> = emptyList(),
    val trailCoordinates: List<Pair<Int, Int>> = emptyList()
)

/**
 * ScanSimulationViewModel
 *
 * Manages the simulated 50 Hz BLE sensor stream, DSP Z-Score filtering,
 * BNO085 IMU fusion, and real-time geophysical grid sweep on Dispatchers.Default.
 */
class ScanSimulationViewModel : ViewModel() {

    private val gridW = 12
    private val gridH = 10
    private val stepDist = 0.5f

    private val dspFilter = DspMineralFilter(windowSize = 11)
    private val imuFusion = Bno085KalmanFusion()

    private val hiddenTargets = mutableMapOf<String, Float>()

    private val _uiState = MutableStateFlow(createSeededInitialState())
    val uiState: StateFlow<ScanSimulationUiState> = _uiState.asStateFlow()

    private var scanningLoopJob: Job? = null
    private var bleSensorStreamJob: Job? = null

    init {
        startBle50HzSensorStream()
        startAutoStepLoop()
    }

    private fun generateTargetsForSector(sector: Int) {
        hiddenTargets.clear()
        val shiftX = ((sector - 1) * 2) % 5 - 2
        val shiftY = ((sector - 1) * 3) % 3 - 1

        val metalX = (7 + shiftX).coerceIn(2, gridW - 3)
        val metalY = (6 + shiftY).coerceIn(2, gridH - 3)
        val voidX = (3 - (shiftX / 2)).coerceIn(2, gridW - 4)
        val voidY = (3 - shiftY).coerceIn(2, gridH - 3)
        val ferrousX = (9 - abs(shiftX)).coerceIn(1, gridW - 2)
        val ferrousY = (2 + abs(shiftY)).coerceIn(1, gridH - 2)

        for (y in 0 until gridH) {
            for (x in 0 until gridW) {
                val dMetal = sqrt((x - metalX).toFloat().pow(2) + (y - metalY).toFloat().pow(2))
                val dVoid = sqrt((x - voidX).toFloat().pow(2) + (y - voidY).toFloat().pow(2))
                val dFerrous = sqrt((x - ferrousX).toFloat().pow(2) + (y - ferrousY).toFloat().pow(2))

                var s = ((x * 7 + y * 13 + sector * 5) % 9 - 4f)
                if (dMetal < 2.3f) {
                    s += 94f * exp(-dMetal * 1.35f)
                }
                if (dVoid < 2.8f) {
                    s -= 86f * exp(-dVoid * 1.1f)
                }
                if (dFerrous < 1.6f) {
                    s += 52f * exp(-dFerrous * 1.9f)
                }
                hiddenTargets["$x,$y"] = s.coerceIn(-100f, 100f)
            }
        }
    }

    private fun createSeededInitialState(): ScanSimulationUiState {
        generateTargetsForSector(1)
        val emptyGrid = ScanGenerators.generateEmptyLiveGrid(gridW, gridH, stepDist)
        val mutableRows = emptyGrid.points.map { it.toMutableList() }.toMutableList()
        val initialTrail = mutableListOf<Pair<Int, Int>>()
        val now = System.currentTimeMillis()

        // Seed the first 18 cells (row 0 full + 6 cells of row 1) so the grid has immediate live texture on open
        var sx = 0
        var sy = 0
        val seededCount = 18
        var minSig = 0f
        var maxSig = 0f
        var sumAbs = 0f

        for (i in 0 until seededCount) {
            val sig = hiddenTargets["$sx,$sy"] ?: 0f
            val raw = (sig * 12.0f + 48100f).roundToInt()
            val absSig = abs(sig)
            val estDepth = if (absSig > 20f) (1.25f * (100f / absSig).pow(0.35f)).coerceIn(0.4f, 5.0f) else 0f

            mutableRows[sy][sx] = mutableRows[sy][sx].copy(
                rawSignal = raw,
                filteredSignal = sig,
                depth = estDepth,
                classification = ScanGenerators.classifySignal(sig),
                confidence = (55 + absSig * 0.42f).roundToInt().coerceAtMost(98),
                timestamp = now - (seededCount - i) * 280L
            )
            initialTrail.add(sx to sy)
            if (sig < minSig) minSig = sig
            if (sig > maxSig) maxSig = sig
            sumAbs += absSig

            // Advance zigzag step
            if (sy % 2 == 0) {
                if (sx + 1 < gridW) sx++ else sy++
            } else {
                if (sx - 1 >= 0) sx-- else sy++
            }
        }

        val initialSamples = (0 until 24).map { idx ->
            val wave = (sin(idx * 0.45) * 14.0).toFloat()
            SignalSample(
                timestampMs = now - (24 - idx) * 60L,
                sensorA = 48100f + wave * 0.5f,
                sensorB = 48100f - wave * 0.5f,
                gradientDelta = wave
            )
        }

        val finalPoints = mutableRows.map { it.toList() }
        val previewGrid = emptyGrid.copy(width = gridW, height = gridH, stepDistanceMeters = stepDist, points = finalPoints)
        val initialAnomalies = detectVisitedAnomalies(previewGrid)

        return ScanSimulationUiState(
            isScanning = true,
            autoLoop = true,
            scanSpeedMultiplier = 1.0f,
            sectorNumber = 1,
            gridWidth = gridW,
            gridHeight = gridH,
            stepDistanceMeters = stepDist,
            currentStepX = sx,
            currentStepY = sy,
            points = finalPoints,
            signalSamples = initialSamples,
            visitedCount = seededCount,
            minSignalPct = minSig,
            maxSignalPct = maxSig,
            avgAbsSignalPct = sumAbs / seededCount,
            estimatedTargetDepthMeters = initialAnomalies.firstOrNull()?.depthMeters ?: 1.85f,
            discoveredAnomalies = initialAnomalies,
            trailCoordinates = initialTrail
        )
    }

    /**
     * BLE 50 Hz Hardware Telemetry Coroutine on Dispatchers.Default
     */
    private fun startBle50HzSensorStream() {
        bleSensorStreamJob?.cancel()
        bleSensorStreamJob = viewModelScope.launch(Dispatchers.Default) {
            var tick = 0
            while (isActive) {
                delay(20) // 50 Hz telemetry stream
                tick++

                val currentState = _uiState.value
                val gradBase = currentState.deviceStatus.gradientDelta.toFloat()
                val noise = (sin(tick * 0.4) * 2.2 + (Math.random() - 0.5) * 3.0).toFloat()
                val currentDelta = gradBase + noise

                val sensorAVal = (currentState.groundBalance + currentDelta * 0.5f).roundToInt()
                val sensorBVal = (currentState.groundBalance - currentDelta * 0.5f).roundToInt()

                val dspResult = dspFilter.processReading(
                    rawDeltaNt = currentDelta,
                    zThreshold = currentState.zThreshold
                )

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

    fun toggleScanning() {
        val current = _uiState.value
        if (!current.isScanning && current.currentStepY >= gridH) {
            advanceToNextSector(startImmediately = true)
            return
        }
        val willScan = !current.isScanning
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
                val speed = _uiState.value.scanSpeedMultiplier.coerceIn(0.5f, 4.0f)
                val stepDelayMs = (320f / speed).toLong().coerceIn(80L, 700L)
                delay(stepDelayMs)

                val state = _uiState.value
                if (!state.isScanning) break

                if (state.currentStepY >= gridH) {
                    if (state.autoLoop) {
                        delay(1400)
                        if (isActive && _uiState.value.isScanning) {
                            advanceToNextSector(startImmediately = true)
                        }
                    } else {
                        _uiState.update { it.copy(isScanning = false) }
                        break
                    }
                } else {
                    takeSingleStepInternal()
                }
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

        if (y >= gridH || x !in 0 until gridW) return@withContext

        val targetSignal = hiddenTargets["$x,$y"] ?: 0f
        val raw = (targetSignal * 12.0f + state.groundBalance).roundToInt()
        val absSig = abs(targetSignal)
        val estimatedDepth = if (absSig > 20f) {
            (1.25f * (100f / absSig).pow(0.35f)).coerceIn(0.4f, 5.0f)
        } else 0f

        if (state.isAudioEnabled) {
            AudioSynthesizer.playSignalTone(targetSignal)
        }

        val updatedPoints = state.points.mapIndexed { py, row ->
            row.mapIndexed { px, p ->
                if (px == x && py == y) {
                    p.copy(
                        rawSignal = raw,
                        filteredSignal = targetSignal,
                        depth = estimatedDepth,
                        classification = ScanGenerators.classifySignal(targetSignal),
                        confidence = (55 + absSig * 0.42f).roundToInt().coerceAtMost(98),
                        timestamp = System.currentTimeMillis()
                    )
                } else p
            }
        }

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

        // Compute aggregate telemetry & real-time anomaly detections across visited cells
        var visited = 0
        var minSig = 0f
        var maxSig = 0f
        var absSum = 0f
        for (row in updatedPoints) {
            for (pt in row) {
                if (pt.timestamp > 0L) {
                    visited++
                    if (pt.filteredSignal < minSig) minSig = pt.filteredSignal
                    if (pt.filteredSignal > maxSig) maxSig = pt.filteredSignal
                    absSum += abs(pt.filteredSignal)
                }
            }
        }

        val snapshotGrid = ScanGrid(
            id = "live-sector-${state.sectorNumber}",
            title = "Canlı Saha Sektör #${state.sectorNumber}",
            date = "Canlı Akış",
            width = gridW,
            height = gridH,
            stepDistanceMeters = stepDist,
            points = updatedPoints,
            groundBalance = state.groundBalance,
            sensorType = "Dual FGM Fluxgate",
            locationName = "Aktif Saha Sektörü #${state.sectorNumber}",
            notes = ""
        )
        val liveAnomalies = detectVisitedAnomalies(snapshotGrid)
        val estTargetDepth = liveAnomalies.maxByOrNull { it.signalStrength }?.depthMeters
            ?: if (estimatedDepth > 0f) estimatedDepth else state.estimatedTargetDepthMeters

        _uiState.update { s ->
            s.copy(
                points = updatedPoints,
                currentStepX = nextX,
                currentStepY = nextY,
                isScanning = if (scanEnded && !s.autoLoop) false else s.isScanning,
                visitedCount = visited,
                minSignalPct = minSig,
                maxSignalPct = maxSig,
                avgAbsSignalPct = if (visited > 0) absSum / visited else 0f,
                estimatedTargetDepthMeters = estTargetDepth,
                discoveredAnomalies = liveAnomalies,
                trailCoordinates = (s.trailCoordinates + (x to y)).takeLast(32),
                deviceStatus = s.deviceStatus.copy(
                    gradientDelta = (targetSignal * 12f).roundToInt(),
                    currentPitch = 1.0f + (nextX % 3 - 1) * 0.4f,
                    currentRoll = -0.3f + (nextY % 3 - 1) * 0.3f
                )
            )
        }
    }

    private fun detectVisitedAnomalies(grid: ScanGrid): List<DetectedAnomaly> {
        val anomalies = mutableListOf<DetectedAnomaly>()
        val visitedSet = mutableSetOf<String>()
        val w = grid.width
        val h = grid.height

        for (y in 0 until h) {
            for (x in 0 until w) {
                val pt = grid.points.getOrNull(y)?.getOrNull(x) ?: continue
                if (pt.timestamp <= 0L) continue
                val key = "$x,$y"
                if (key in visitedSet) continue

                val sig = pt.filteredSignal
                if (abs(sig) > 38f) {
                    var isLocalPeak = true
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nPt = grid.points.getOrNull(y + dy)?.getOrNull(x + dx) ?: continue
                            if (nPt.timestamp <= 0L) continue
                            if (sig > 0f && nPt.filteredSignal > sig) isLocalPeak = false
                            if (sig < 0f && nPt.filteredSignal < sig) isLocalPeak = false
                        }
                    }

                    if (isLocalPeak) {
                        for (dy in -2..2) {
                            for (dx in -2..2) {
                                visitedSet.add("${x + dx},${y + dy}")
                            }
                        }

                        val isCavity = sig < 0f
                        val depthResult = SignalDepthCalculator.calculateAnomalyDepth(grid, x, y, SoilType.STANDARD)
                        val title = when {
                            isCavity -> "Yeraltı Boşluğu / Yapı"
                            sig > 65f -> "Değerli Metal Anomalisi"
                            else -> "Ferromanyetik Kütle"
                        }
                        val desc = String.format(
                            Locale.US,
                            "Derinlik ~%.2fm • Genlik %%%d • FWHM %.2fm",
                            depthResult.estimatedDepthMeters,
                            abs(sig).roundToInt(),
                            depthResult.fwhmMeters
                        )

                        anomalies.add(
                            DetectedAnomaly(
                                id = "live-anom-$x-$y",
                                title = title,
                                classification = pt.classification,
                                xMeters = x * grid.stepDistanceMeters,
                                yMeters = y * grid.stepYMeters,
                                gridX = x,
                                gridY = y,
                                depthMeters = depthResult.estimatedDepthMeters,
                                volumeM3 = depthResult.estimatedVolumeM3,
                                signalStrength = abs(sig).roundToInt(),
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
        return anomalies.sortedByDescending { it.signalStrength }
    }

    private fun advanceToNextSector(startImmediately: Boolean) {
        val nextSector = _uiState.value.sectorNumber + 1
        generateTargetsForSector(nextSector)
        dspFilter.reset()
        imuFusion.reset()
        _uiState.update { s ->
            s.copy(
                sectorNumber = nextSector,
                isScanning = startImmediately,
                currentStepX = 0,
                currentStepY = 0,
                visitedCount = 0,
                minSignalPct = 0f,
                maxSignalPct = 0f,
                avgAbsSignalPct = 0f,
                selectedCell = null,
                discoveredAnomalies = emptyList(),
                trailCoordinates = emptyList(),
                points = ScanGenerators.generateEmptyLiveGrid(gridW, gridH, stepDist).points
            )
        }
    }

    fun setWalkPattern(pattern: String) {
        _uiState.update { it.copy(walkPattern = pattern) }
    }

    fun setPalette(palette: PaletteType) {
        _uiState.update { it.copy(activePalette = palette) }
    }

    fun toggleShowValues() {
        _uiState.update { it.copy(showCellValues = !it.showCellValues) }
    }

    fun toggleSmoothGrid() {
        _uiState.update { it.copy(smoothGrid = !it.smoothGrid) }
    }

    fun toggleSpeed() {
        _uiState.update { state ->
            val nextSpeed = when (state.scanSpeedMultiplier) {
                1.0f -> 2.0f
                2.0f -> 0.5f
                else -> 1.0f
            }
            state.copy(scanSpeedMultiplier = nextSpeed)
        }
    }

    fun toggleAutoLoop() {
        _uiState.update { it.copy(autoLoop = !it.autoLoop) }
    }

    fun toggleAudio() {
        _uiState.update { it.copy(isAudioEnabled = !it.isAudioEnabled) }
    }

    fun selectCell(x: Int, y: Int) {
        val safeX = x.coerceIn(0, gridW - 1)
        val safeY = y.coerceIn(0, gridH - 1)
        _uiState.update { it.copy(selectedCell = safeX to safeY) }
    }

    fun clearSelectedCell() {
        _uiState.update { it.copy(selectedCell = null) }
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
        val shouldRestart = _uiState.value.isScanning
        scanningLoopJob?.cancel()
        advanceToNextSector(startImmediately = shouldRestart)
        if (shouldRestart) {
            startAutoStepLoop()
        }
    }

    fun buildCompletedGrid(): ScanGrid {
        val s = _uiState.value
        return ScanGrid(
            id = "scan-live-sector-${s.sectorNumber}-${System.currentTimeMillis()}",
            title = "Canlı Saha Taraması • Sektör #${s.sectorNumber}",
            date = "Canlı Oturum",
            width = s.gridWidth,
            height = s.gridHeight,
            stepDistanceMeters = s.stepDistanceMeters,
            points = s.points,
            groundBalance = s.groundBalance,
            sensorType = "Dual FGM Fluxgate (50Hz BLE)",
            locationName = "Aktif Tarama Sahası • Sektör #${s.sectorNumber}",
            notes = "${s.visitedCount}/${s.gridWidth * s.gridHeight} hücre canlı sensör akışından kaydedildi."
        )
    }

    override fun onCleared() {
        super.onCleared()
        scanningLoopJob?.cancel()
        bleSensorStreamJob?.cancel()
    }
}
