package com.lelesoftware.app.ui.components

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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lelesoftware.app.model.PaletteType
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.util.ColorScales

/**
 * LiveScannerScreen
 *
 * Thread Yönetimi & Sensör Akışı:
 * ScanSimulationViewModel aracılığıyla BLE 50 Hz sensör akışı, ΔnT manyetik gradyan
 * ve matris işleme adımlarını Main Thread yerine Dispatchers.Default üzerinde çalıştırır.
 */
@Composable
fun LiveScannerScreen(
    onScanCompleted: (ScanGrid) -> Unit,
    viewModel: ScanSimulationViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val gridWidth = uiState.gridWidth
    val gridHeight = uiState.gridHeight
    val deviceStatus = uiState.deviceStatus

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
                            Text(deviceStatus.deviceName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Bağlı (BLE 50 Hz - Dispatchers.Default)", color = Color(0xFF10B981), fontSize = 12.sp)
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                "Sinyal: %${deviceStatus.signalQuality}",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                        Text("Sensör A (Üst)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("${deviceStatus.currentSensorA} nT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column {
                        Text("Sensör B (Alt)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("${deviceStatus.currentSensorB} nT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column {
                        Text("Gradyan Farkı (Δ)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            "${deviceStatus.gradientDelta} nT",
                            color = if (deviceStatus.gradientDelta > 0) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column {
                        Text("Eğim (P/R)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            "${deviceStatus.currentPitch}° / ${deviceStatus.currentRoll}°",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Section 1.5: DSP Z-Score Mineral & IMU Sensor Fusion Control Panel
        DspMineralControlPanel(
            rawDeltaNt = deviceStatus.gradientDelta.toFloat(),
            currentPitch = deviceStatus.currentPitch,
            currentRoll = deviceStatus.currentRoll,
            currentYaw = (45f + (uiState.currentStepX * 4f)),
            onFilterParametersChanged = { zThresh, imuEnabled, _ ->
                viewModel.setZThreshold(zThresh)
                viewModel.setImuEnabled(imuEnabled)
            }
        )

        // Section 2: Earth Magnetic Axis Survey Compass
        MagneticSurveyCompass()

        // Section 3: Animated Radar Pulse Effect - In the Center of Active Scanning
        AnimatedRadarPulse(
            isScanning = uiState.isScanning,
            gradientDelta = deviceStatus.gradientDelta.toFloat()
        )

        // Section 4: Real-time 2D Signal Graph (Canvas API Oscilloscope)
        RealtimeSignalGraph(
            samples = uiState.signalSamples,
            currentDelta = uiState.signalSamples.lastOrNull()?.gradientDelta ?: deviceStatus.gradientDelta.toFloat(),
            currentSensorA = deviceStatus.currentSensorA.toFloat(),
            currentSensorB = deviceStatus.currentSensorB.toFloat(),
            isStreaming = uiState.isGraphStreaming,
            onToggleStreaming = { viewModel.toggleGraphStreaming() },
            onClearSamples = { viewModel.clearSamples() }
        )

        // Section 5: Live Scanning Walking Matrix
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
                        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = Color(0xFFF59E0B))
                        Text(
                            text = "Adım Adım Saha Taraması",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = uiState.walkPattern == "zigzag",
                            onClick = { viewModel.setWalkPattern("zigzag") },
                            label = { Text("Zikzak", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = uiState.walkPattern == "parallel",
                            onClick = { viewModel.setWalkPattern("parallel") },
                            label = { Text("Paralel", fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                val totalSteps = gridWidth * gridHeight
                val currentStepIndex = (uiState.currentStepY * gridWidth + uiState.currentStepX).coerceAtMost(totalSteps)
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
                    Text("Adım: $currentStepIndex / $totalSteps", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("Konum: (${uiState.currentStepX}, ${uiState.currentStepY.coerceAtMost(gridHeight - 1)})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grid Visualization Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(gridWidth.toFloat() / gridHeight.toFloat().coerceAtLeast(1f))
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
                            val row = uiState.points.getOrNull(y) ?: continue
                            for (x in 0 until gridWidth) {
                                val pt = row.getOrNull(x) ?: continue
                                val isVisited = pt.timestamp > 0
                                val isCurrent = (x == uiState.currentStepX && y == uiState.currentStepY && uiState.isScanning)

                                val cellColor = if (isVisited) {
                                    ColorScales.getPaletteColor(pt.filteredSignal, PaletteType.LELE_SPECTRUM)
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
                        onClick = { viewModel.toggleScanning() },
                        modifier = Modifier.weight(1f).testTag("toggle_scan_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isScanning) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    ) {
                        Icon(if (uiState.isScanning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (uiState.isScanning) "Durdur" else "Taramayı Başlat")
                    }

                    OutlinedButton(
                        onClick = { viewModel.takeSingleStep() },
                        enabled = !uiState.isScanning && uiState.currentStepY < gridHeight
                    ) {
                        Text("Tek Adım")
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetScan() }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sıfırla")
                    }
                }

                // If scan completed, show Complete & Transfer button
                if (uiState.currentStepY >= gridHeight) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val completedGrid = viewModel.buildCompletedGrid()
                            onScanCompleted(completedGrid)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("complete_live_scan_button"),
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
