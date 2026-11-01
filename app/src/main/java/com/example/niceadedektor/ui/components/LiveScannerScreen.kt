package com.example.niceadedektor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.niceadedektor.model.DeviceStatus
import com.example.niceadedektor.model.PaletteType
import com.example.niceadedektor.model.ScanGrid
import com.example.niceadedektor.model.ScanPoint
import com.example.niceadedektor.model.TargetClassification
import com.example.niceadedektor.util.AudioSynthesizer
import com.example.niceadedektor.util.ColorScales
import com.example.niceadedektor.util.ScanGenerators
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun LiveScannerScreen(
    onScanCompleted: (ScanGrid) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridWidth = 10
    val gridHeight = 10
    val stepDistance = 0.5f

    var walkPattern by remember { mutableStateOf("zigzag") } // zigzag or parallel
    var isScanning by remember { mutableStateOf(false) }
    var currentStepX by remember { mutableIntStateOf(0) }
    var currentStepY by remember { mutableIntStateOf(0) }
    var points by remember { mutableStateOf(ScanGenerators.generateEmptyLiveGrid(gridWidth, gridHeight, stepDistance).points) }
    var groundBalance by remember { mutableIntStateOf(48100) }
    var isCalibrating by remember { mutableStateOf(false) }
    var deviceStatus by remember { mutableStateOf(DeviceStatus()) }

    // Seed hidden subterranean targets for the live simulation
    val hiddenTargets = remember {
        val map = mutableMapOf<String, Float>()
        val cx = (gridWidth * 0.6f).toInt()
        val cy = (gridHeight * 0.6f).toInt()
        val vx = (gridWidth * 0.3f).toInt()
        val vy = (gridHeight * 0.4f).toInt()

        for (y in 0 until gridHeight) {
            for (x in 0 until gridWidth) {
                val dMetal = sqrt((x - cx).toFloat().pow(2) + (y - cy).toFloat().pow(2))
                val dVoid = sqrt((x - vx).toFloat().pow(2) + (y - vy).toFloat().pow(2))
                var s = ((x * 7 + y * 13) % 7 - 3f)
                if (dMetal < 2.0f) {
                    s += 92f * exp(-dMetal * 1.6f)
                } else if (dVoid < 2.5f) {
                    s -= 82f * exp(-dVoid * 1.2f)
                }
                map["$x,$y"] = s.coerceIn(-100f, 100f)
            }
        }
        map
    }

    fun takeSingleStep() {
        if (currentStepY >= gridHeight) return

        val x = currentStepX
        val y = currentStepY
        val targetSignal = hiddenTargets["$x,$y"] ?: 0f

        val updatedPoints = points.mapIndexed { py, row ->
            row.mapIndexed { px, p ->
                if (px == x && py == y) {
                    val raw = (targetSignal * 12.0f + groundBalance).roundToInt()
                    p.copy(
                        rawSignal = raw,
                        filteredSignal = targetSignal,
                        depth = if (abs(targetSignal) > 30f) 1.8f else 0f,
                        classification = ScanGenerators.classifySignal(targetSignal),
                        confidence = 90,
                        timestamp = System.currentTimeMillis()
                    )
                } else p
            }
        }
        points = updatedPoints

        // Audio feedback on step
        AudioSynthesizer.playSignalTone(targetSignal)

        // Update probe telemetry
        deviceStatus = deviceStatus.copy(
            currentSensorA = (groundBalance + targetSignal * 6.5f).roundToInt(),
            currentSensorB = (groundBalance - targetSignal * 5.5f).roundToInt(),
            gradientDelta = (targetSignal * 12.0f).roundToInt(),
            currentPitch = 1.0f + (x % 3 - 1) * 0.5f,
            currentRoll = -0.2f + (y % 3 - 1) * 0.4f
        )

        // Advance to next coordinate based on pattern
        if (walkPattern == "zigzag") {
            if (y % 2 == 0) {
                // Moving right
                if (x + 1 < gridWidth) {
                    currentStepX = x + 1
                } else {
                    currentStepY = y + 1
                }
            } else {
                // Moving left
                if (x - 1 >= 0) {
                    currentStepX = x - 1
                } else {
                    currentStepY = y + 1
                }
            }
        } else {
            // Parallel: always 0 to width-1
            if (x + 1 < gridWidth) {
                currentStepX = x + 1
            } else {
                currentStepX = 0
                currentStepY = y + 1
            }
        }

        if (currentStepY >= gridHeight) {
            isScanning = false
        }
    }

    // Auto walking coroutine
    LaunchedEffect(isScanning, currentStepX, currentStepY) {
        if (isScanning && currentStepY < gridHeight) {
            delay(400)
            takeSingleStep()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Bluetooth Hardware Status Bar
        Card(
            modifier = Modifier.fillMaxWidth().testTag("hardware_status_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(0xFF38BDF8))
                        Column {
                            Text(deviceStatus.deviceName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Bağlı (BLE 5.2 - 50Hz Örnekleme)", color = Color(0xFF10B981), fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                "Pil: %${deviceStatus.batteryLevel}",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                "Sinyal: %${deviceStatus.signalQuality}",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dual Fluxgate Sensor Live Gauges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Sensör A (Üst)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text("${deviceStatus.currentSensorA} nT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column {
                        Text("Sensör B (Alt)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text("${deviceStatus.currentSensorB} nT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column {
                        Text("Gradyan Farkı (Δ)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(
                            "${deviceStatus.gradientDelta} nT",
                            color = if (deviceStatus.gradientDelta > 0) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Column {
                        Text("Eğim (P/R)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(
                            "${deviceStatus.currentPitch}° / ${deviceStatus.currentRoll}°",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Section 2: Live Scanning Walking Matrix
        Card(
            modifier = Modifier.fillMaxWidth().testTag("live_walking_grid_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color(0xFFF59E0B))
                        Text(
                            text = "Adım Adım Saha Taraması",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = walkPattern == "zigzag",
                            onClick = { walkPattern = "zigzag" },
                            label = { Text("Zikzak", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = walkPattern == "parallel",
                            onClick = { walkPattern = "parallel" },
                            label = { Text("Paralel", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                val totalSteps = gridWidth * gridHeight
                val currentStepIndex = (currentStepY * gridWidth + currentStepX).coerceAtMost(totalSteps)
                val progress = currentStepIndex.toFloat() / totalSteps.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = Color(0xFFF59E0B),
                    trackColor = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Adım: $currentStepIndex / $totalSteps", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text("Konum: (${currentStepX}, ${currentStepY.coerceAtMost(gridHeight - 1)})", color = Color.White, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grid Visualization Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val renderW = size.width
                        val renderH = size.height
                        val cellW = renderW / gridWidth
                        val cellH = renderH / gridHeight

                        for (y in 0 until gridHeight) {
                            for (x in 0 until gridWidth) {
                                val pt = points[y][x]
                                val isVisited = pt.timestamp > 0
                                val isCurrent = (x == currentStepX && y == currentStepY && isScanning)

                                val cellColor = if (isVisited) {
                                    ColorScales.getPaletteColor(pt.filteredSignal, PaletteType.NICEA_SPECTRUM)
                                } else {
                                    Color(0xFF0B1120)
                                }

                                drawRect(
                                    color = cellColor,
                                    topLeft = Offset(x * cellW, y * cellH),
                                    size = Size(cellW - 1f, cellH - 1f)
                                )

                                if (isCurrent) {
                                    drawCircle(
                                        color = Color.White,
                                        radius = cellW * 0.35f,
                                        center = Offset(x * cellW + cellW / 2f, y * cellH + cellH / 2f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Walk Controls Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isScanning = !isScanning },
                        modifier = Modifier.weight(1f).testTag("toggle_scan_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    ) {
                        Icon(if (isScanning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isScanning) "Durdur" else "Taramayı Başlat")
                    }

                    OutlinedButton(
                        onClick = { takeSingleStep() },
                        enabled = !isScanning && currentStepY < gridHeight
                    ) {
                        Text("Tek Adım")
                    }

                    OutlinedButton(
                        onClick = {
                            isScanning = false
                            currentStepX = 0
                            currentStepY = 0
                            points = ScanGenerators.generateEmptyLiveGrid(gridWidth, gridHeight, stepDistance).points
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sıfırla")
                    }
                }

                // If scan completed, show Complete & Transfer button
                if (currentStepY >= gridHeight) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val completedGrid = ScanGrid(
                                id = "scan-live-${System.currentTimeMillis()}",
                                title = "Canlı Saha Taraması (Tamamlandı)",
                                date = "2026-11-01 13:00",
                                width = gridWidth,
                                height = gridHeight,
                                stepDistanceMeters = stepDistance,
                                points = points,
                                groundBalance = groundBalance,
                                sensorType = "Gradiometer 2-Sensör",
                                locationName = "Canlı Ölçüm Sahası",
                                notes = "10x10 adım canlı sensör probu ile kaydedildi."
                            )
                            onScanCompleted(completedGrid)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Taramayı Tamamla & 3D Analize Aktar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
