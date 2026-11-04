package com.lelesoftware.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.PaletteType
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ScanPoint
import com.lelesoftware.app.model.SoilType
import com.lelesoftware.app.util.AudioSynthesizer
import com.lelesoftware.app.util.ColorScales
import com.lelesoftware.app.util.SignalDepthCalculator
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TwoDimensionalView(
    grid: ScanGrid,
    palette: PaletteType,
    anomalies: List<DetectedAnomaly>,
    selectedAnomalyId: String?,
    onSelectAnomaly: (DetectedAnomaly?) -> Unit,
    modifier: Modifier = Modifier
) {
    var smoothInterpolation by remember { mutableStateOf(true) }
    var showValues by remember { mutableStateOf(false) }
    var sliceAxis by remember { mutableStateOf("X") }
    var selectedCell by remember(grid) {
        mutableStateOf(
            Pair(
                grid.width / 2,
                grid.height / 2
            )
        )
    }

    val selectedPoint: ScanPoint = grid.points[selectedCell.second][selectedCell.first]
    var selectedSoilType by remember { mutableStateOf(SoilType.STANDARD) }
    val calculatedDepth = remember(selectedCell, grid, selectedSoilType) {
        SignalDepthCalculator.estimateCellDepth(
            grid = grid,
            x = selectedCell.first,
            y = selectedCell.second,
            soilType = selectedSoilType
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: 2D Geophysical Heatmap Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("two_dimensional_heatmap_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header & Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFFF59E0B))
                        Text(
                            text = "2D Plan Görünümü & Isı Matrisi",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = smoothInterpolation,
                            onClick = { smoothInterpolation = !smoothInterpolation },
                            label = { Text(if (smoothInterpolation) "Bilinear" else "Blok", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = showValues,
                            onClick = { showValues = !showValues },
                            label = { Text("Sayılar", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Heatmap Canvas with North Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(grid) {
                                detectTapGestures { offset ->
                                    val cellW = size.width / grid.width
                                    val cellH = size.height / grid.height
                                    val gx = (offset.x / cellW).toInt().coerceIn(0, grid.width - 1)
                                    val gy = (offset.y / cellH).toInt().coerceIn(0, grid.height - 1)
                                    selectedCell = Pair(gx, gy)
                                    val pt = grid.points[gy][gx]
                                    AudioSynthesizer.playSignalTone(pt.filteredSignal)

                                    val match = anomalies.find { it.gridX == gx && it.gridY == gy }
                                    onSelectAnomaly(match)
                                }
                            }
                    ) {
                        val renderW = size.width
                        val renderH = size.height
                        val cellW = renderW / grid.width
                        val cellH = renderH / grid.height

                        if (smoothInterpolation) {
                            // Subdivided interpolation grid
                            val subStepsX = grid.width * 4
                            val subStepsY = grid.height * 4
                            val subW = renderW / subStepsX
                            val subH = renderH / subStepsY

                            for (sy in 0 until subStepsY) {
                                val gy = (sy.toFloat() / subStepsY) * (grid.height - 1)
                                val y0 = gy.toInt()
                                val y1 = (y0 + 1).coerceAtMost(grid.height - 1)
                                val wy = gy - y0

                                for (sx in 0 until subStepsX) {
                                    val gx = (sx.toFloat() / subStepsX) * (grid.width - 1)
                                    val x0 = gx.toInt()
                                    val x1 = (x0 + 1).coerceAtMost(grid.width - 1)
                                    val wx = gx - x0

                                    val s00 = grid.points[y0][x0].filteredSignal
                                    val s10 = grid.points[y0][x1].filteredSignal
                                    val s01 = grid.points[y1][x0].filteredSignal
                                    val s11 = grid.points[y1][x1].filteredSignal

                                    val sTop = s00 * (1f - wx) + s10 * wx
                                    val sBottom = s01 * (1f - wx) + s11 * wx
                                    val sig = sTop * (1f - wy) + sBottom * wy

                                    val col = ColorScales.getPaletteColor(sig, palette)
                                    drawRect(
                                        color = col,
                                        topLeft = Offset(sx * subW, sy * subH),
                                        size = Size(subW + 0.5f, subH + 0.5f)
                                    )
                                }
                            }
                        } else {
                            // Discrete Blocks
                            for (y in 0 until grid.height) {
                                for (x in 0 until grid.width) {
                                    val sig = grid.points[y][x].filteredSignal
                                    val col = ColorScales.getPaletteColor(sig, palette)
                                    drawRect(
                                        color = col,
                                        topLeft = Offset(x * cellW, y * cellH),
                                        size = Size(cellW, cellH)
                                    )
                                    drawRect(
                                        color = Color(0x33000000),
                                        topLeft = Offset(x * cellW, y * cellH),
                                        size = Size(cellW, cellH),
                                        style = Stroke(width = 1f)
                                    )
                                }
                            }
                        }

                        // Draw Anomaly Target Rings
                        for (anom in anomalies) {
                            val cx = anom.gridX * cellW + cellW / 2f
                            val cy = anom.gridY * cellH + cellH / 2f
                            val isSel = anom.id == selectedAnomalyId
                            val anomColor = Color(anom.classification.colorHex)

                            drawCircle(
                                color = anomColor,
                                radius = cellW * 0.42f,
                                center = Offset(cx, cy),
                                style = Stroke(width = if (isSel) 3.5f else 2f)
                            )
                            drawCircle(
                                color = anomColor.copy(alpha = 0.45f),
                                radius = cellW * 0.22f,
                                center = Offset(cx, cy)
                            )
                        }

                        // Selected Cell Crosshair
                        val selX = selectedCell.first * cellW + cellW / 2f
                        val selY = selectedCell.second * cellH + cellH / 2f
                        drawCircle(
                            color = Color.White,
                            radius = cellW * 0.48f,
                            center = Offset(selX, selY),
                            style = Stroke(width = 2.5f)
                        )
                        drawLine(Color.White, Offset(selX - cellW * 0.7f, selY), Offset(selX + cellW * 0.7f, selY), strokeWidth = 1.5f)
                        drawLine(Color.White, Offset(selX, selY - cellH * 0.7f), Offset(selX, selY + cellH * 0.7f), strokeWidth = 1.5f)
                    }

                    // North Direction Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = "North",
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(14.dp)
                            )
                            Text("KUZEY (N)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 2: Selected Point Detail HUD
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = "Seçili Nokta: (${selectedCell.first}, ${selectedCell.second}) - ${(selectedCell.first * grid.stepDistanceMeters).format(1)}m x ${(selectedCell.second * grid.stepDistanceMeters).format(1)}m",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(selectedPoint.classification.colorHex).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedPoint.classification.colorHex))
                    ) {
                        Text(
                            text = selectedPoint.classification.label,
                            color = Color(selectedPoint.classification.colorHex),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Manyetik Sinyal", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = "${selectedPoint.filteredSignal.format(1)} % (${selectedPoint.rawSignal} nT)",
                            color = if (selectedPoint.filteredSignal > 0) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column {
                        Text("Hesaplanan Derinlik", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = if (abs(selectedPoint.filteredSignal) > 12f) {
                                "${calculatedDepth.estimatedDepthMeters.format(2)} m"
                            } else "Yüzey (<0.3m)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column {
                        Text("Genlik Güveni", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = "%${calculatedDepth.confidencePercent}",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Section 2.5: Sinyal Genliği ile Derinlik Hesaplayıcı & Sönümleme Analizi
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signal_amplitude_depth_calculator_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Depth Inversion",
                            tint = Color(0xFF10B981)
                        )
                        Column {
                            Text(
                                text = "Sinyal Genliği ile Derinlik Hesaplayıcı",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "2D Genlik & FWHM İnversiyon Modeli (Peters Half-Slope)",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text(
                            text = "%${calculatedDepth.confidencePercent} GÜVEN",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Soil Type Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Zemin Tipi & Sönümleme Kalibrasyonu (Soil Factor):",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SoilType.entries.forEach { soil ->
                            FilterChip(
                                selected = selectedSoilType == soil,
                                onClick = { selectedSoilType = soil },
                                label = {
                                    Text(
                                        text = "${soil.label.split("/")[0].trim()} (${soil.attenuationFactor}x)",
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Primary Metrics Banner (Depth, Amplitude, FWHM, Tolerance)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF020617))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Estimated Depth Block
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("HESAPLANAN DERİNLİK", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${calculatedDepth.estimatedDepthMeters.format(2)}",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "metre",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "Aralık: [${calculatedDepth.minDepthMeters.format(2)}m - ${calculatedDepth.maxDepthMeters.format(2)}m] (±${((calculatedDepth.maxDepthMeters - calculatedDepth.minDepthMeters) / 2f).format(2)}m)",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    // Net Amplitude Block
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .padding(start = 8.dp)
                    ) {
                        Text("NET GENLİK (ΔA)", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${calculatedDepth.netDeltaAmplitude.format(1)}%",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${calculatedDepth.peakAmplitudeNanoTesla.roundToInt()} nT dipol",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    // FWHM Block
                    Column(modifier = Modifier.weight(0.9f)) {
                        Text("FWHM ÇAPI", color = Color(0xFFA855F7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${calculatedDepth.fwhmMeters.format(2)} m",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Oran: ${(calculatedDepth.estimatedDepthMeters / calculatedDepth.fwhmMeters).format(2)}",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                // Interactive Dual Canvas:
                // 1) Sinyal Genlik Sönümleme Eğrisi Canvas
                Text(
                    text = "Sinyal Genliği Sönümleme Eğrisi (Amplitude vs Depth Decay):",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val w = size.width
                        val h = size.height
                        val maxD = 5.0f // max depth 5.0m
                        val maxA = 100f // max amplitude 100%

                        // Horizontal grid lines (0%, 25%, 50%, 75%, 100%)
                        for (step in 1..4) {
                            val gy = h - (step / 4f) * h
                            drawLine(Color(0xFF1E293B), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                        }

                        // Vertical depth ticks (1m, 2m, 3m, 4m, 5m)
                        for (d in 1..5) {
                            val gx = (d / maxD) * w
                            drawLine(Color(0xFF1E293B), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                        }

                        // Curve Path
                        val curvePoints = calculatedDepth.profileCurve
                        if (curvePoints.isNotEmpty()) {
                            val curvePath = Path()
                            curvePoints.forEachIndexed { idx, pt ->
                                val px = (pt.first / maxD).coerceIn(0f, 1f) * w
                                val py = h - (pt.second / maxA).coerceIn(0f, 1f) * (h * 0.9f)
                                if (idx == 0) curvePath.moveTo(px, py) else curvePath.lineTo(px, py)
                            }
                            drawPath(
                                path = curvePath,
                                color = Color(0xFF06B6D4),
                                style = Stroke(width = 3f)
                            )
                        }

                        // Target marker on curve
                        val targetX = (calculatedDepth.estimatedDepthMeters / maxD).coerceIn(0f, 1f) * w
                        val targetY = h - (calculatedDepth.netDeltaAmplitude / maxA).coerceIn(0f, 1f) * (h * 0.9f)

                        // Beacon pulse & circle
                        drawCircle(Color(0xFFF59E0B).copy(alpha = 0.35f), radius = 12f, center = Offset(targetX, targetY))
                        drawCircle(Color(0xFFF59E0B), radius = 5f, center = Offset(targetX, targetY))

                        // Vertical dashed reference line down to axis
                        drawLine(
                            color = Color(0xFFF59E0B),
                            start = Offset(targetX, 0f),
                            end = Offset(targetX, h),
                            strokeWidth = 1.5f
                        )
                    }

                    // Overlay labels on Canvas
                    Text(
                        text = "Yüzey (0.0m)",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                    )
                    Text(
                        text = "5.0m",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xCC0F172A)
                    ) {
                        Text(
                            text = "Hedef: ${calculatedDepth.estimatedDepthMeters.format(2)}m (${calculatedDepth.netDeltaAmplitude.format(1)}%)",
                            color = Color(0xFFF59E0B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // 2) Subsurface Stratigraphy Layer Visualizer
                Text(
                    text = "Yeraltı Katman Kesiti & Hedef Derinlik Bandı:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
                        val w = size.width
                        val h = size.height
                        val maxD = 5.0f

                        // Surface line at top
                        val groundY = h * 0.15f
                        drawLine(Color(0xFF22C55E), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 3f)

                        // Topsoil fill
                        drawRect(
                            color = Color(0xFF334155).copy(alpha = 0.25f),
                            topLeft = Offset(0f, groundY),
                            size = Size(w, h - groundY)
                        )

                        // Depth tick marks on right side (1m, 2m, 3m, 4m)
                        for (d in 1..4) {
                            val dy = groundY + (d / maxD) * (h - groundY)
                            drawLine(Color(0xFF475569), Offset(w - 20f, dy), Offset(w, dy), strokeWidth = 1f)
                        }

                        // Target depth band (minDepth .. maxDepth)
                        val topY = groundY + (calculatedDepth.minDepthMeters / maxD).coerceIn(0f, 1f) * (h - groundY)
                        val botY = groundY + (calculatedDepth.maxDepthMeters / maxD).coerceIn(0f, 1f) * (h - groundY)
                        val centerY = groundY + (calculatedDepth.estimatedDepthMeters / maxD).coerceIn(0f, 1f) * (h - groundY)

                        val bandColor = Color(selectedPoint.classification.colorHex)

                        // Anomaly band rectangle
                        drawRect(
                            color = bandColor.copy(alpha = 0.35f),
                            topLeft = Offset(w * 0.25f, topY),
                            size = Size(w * 0.50f, (botY - topY).coerceAtLeast(8f))
                        )
                        drawRect(
                            color = bandColor,
                            topLeft = Offset(w * 0.25f, topY),
                            size = Size(w * 0.50f, (botY - topY).coerceAtLeast(8f)),
                            style = Stroke(width = 1.5f)
                        )

                        // Center line
                        drawLine(
                            color = Color.White,
                            start = Offset(w * 0.20f, centerY),
                            end = Offset(w * 0.80f, centerY),
                            strokeWidth = 2f
                        )
                    }

                    // Stratigraphy text annotations
                    Text(
                        text = "Zemin Yüzeyi (0.0m)",
                        color = Color(0xFF22C55E),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopStart).padding(top = 4.dp, start = 14.dp)
                    )
                    Text(
                        text = "Tavan: ${calculatedDepth.minDepthMeters.format(2)}m | Merkez: ${calculatedDepth.estimatedDepthMeters.format(2)}m | Taban: ${calculatedDepth.maxDepthMeters.format(2)}m",
                        color = Color(0xFFF1F5F9),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                    )
                }

                // Methodology note
                Text(
                    text = "• Algoritma: ${calculatedDepth.method}\n• ${selectedSoilType.description} (${selectedSoilType.attenuationFactor}x sönümleme düzeltmesi uygulandı)",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // Section 3: Depth Cross-Section Analyzer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("depth_cross_section_card"),
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
                        Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = Color(0xFF06B6D4))
                        Text(
                            text = "Jeofizik Derinlik Kesiti (Cross-Section)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = sliceAxis == "X",
                            onClick = { sliceAxis = "X" },
                            label = { Text("Y Kesiti (X Aksı)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = sliceAxis == "Y",
                            onClick = { sliceAxis = "Y" },
                            label = { Text("X Kesiti (Y Aksı)", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Profile Canvas: Plots Depth Penetration & Signal Amplitude
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF040817))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val w = size.width
                        val h = size.height

                        // Axis lines
                        val groundY = h * 0.35f
                        drawLine(Color(0xFF475569), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 1.5f)

                        // Data along selected slice
                        val slicePoints = if (sliceAxis == "X") {
                            grid.points[selectedCell.second]
                        } else {
                            grid.points.map { it[selectedCell.first] }
                        }

                        val count = slicePoints.size
                        val stepPx = w / (count - 1).coerceAtLeast(1)

                        // Signal Curve (above ground line)
                        val signalPath = Path()
                        val depthPath = Path()

                        for (i in 0 until count) {
                            val p = slicePoints[i]
                            val px = i * stepPx
                            // Signal amplitude scaled [-100..100] -> +/- (groundY * 0.8)
                            val sigY = groundY - (p.filteredSignal / 100f) * (groundY * 0.85f)
                            if (i == 0) signalPath.moveTo(px, sigY) else signalPath.lineTo(px, sigY)

                            // Subsurface depth penetration (below ground line)
                            val dY = groundY + (p.depth / 6f) * (h - groundY)
                            if (i == 0) depthPath.moveTo(px, dY) else depthPath.lineTo(px, dY)

                            // Draw vertical probe line at selected column
                            if ((sliceAxis == "X" && i == selectedCell.first) || (sliceAxis == "Y" && i == selectedCell.second)) {
                                drawLine(Color(0xFFF59E0B), Offset(px, 0f), Offset(px, h), strokeWidth = 2f)
                            }
                        }

                        drawPath(signalPath, color = Color(0xFF06B6D4), style = Stroke(width = 2.5f))
                        drawPath(depthPath, color = Color(0xFFEAB308), style = Stroke(width = 2f))
                    }

                    // Legend
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF06B6D4), CircleShape))
                            Text("Sinyal Eğrisi", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFEAB308), CircleShape))
                            Text("Derinlik Penetrasyonu", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Section 4: Detected Anomalies Cards
        Text(
            text = "Tespit Edilen Anomaliler (${anomalies.size} Hedef)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        for (anom in anomalies) {
            val isSelected = anom.id == selectedAnomalyId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectAnomaly(if (isSelected) null else anom)
                        selectedCell = Pair(anom.gridX, anom.gridY)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) Color(anom.classification.colorHex) else Color(0xFF1E293B)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(anom.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(anom.description, color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Derinlik: ${anom.depthMeters.format(2)}m [±${((anom.maxDepthMeters - anom.minDepthMeters) / 2f).format(2)}m] | Genlik: %${anom.signalStrength} | FWHM: ${anom.fwhmMeters.format(2)}m | Güven: %${anom.depthConfidence}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Konum: (${anom.xMeters.format(1)}m, ${anom.yMeters.format(1)}m) • ${anom.calculationFormula}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(anom.classification.colorHex).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(anom.classification.colorHex))
                    ) {
                        Text(
                            text = "${anom.signalStrength}%",
                            color = Color(anom.classification.colorHex),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun Float.format(digits: Int): String {
    return String.format(Locale.US, "%.${digits}f", this)
}
