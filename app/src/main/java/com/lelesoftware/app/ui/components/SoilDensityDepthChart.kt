package com.lelesoftware.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.TargetClassification
import java.util.Locale
import kotlin.math.sin

/**
 * High-performance D3-style Data-Driven Chart Component for LELE SOFTWARE Dashboard.
 * Visualizes detected soil matrix density (g/cm³) and anomaly depth (m) in real-time.
 *
 * Coordinates:
 * - Y-Axis: Depth from surface 0.0m (top) down to 10.0m (bottom).
 * - X-Axis: Soil Density from 1.0 g/cm³ (soft/organic) to 3.2 g/cm³ (dense mineral/bedrock).
 * - Area Fill & Contour Lines: Geophysical stratigraphy layers.
 * - Anomaly Nodes: Real-time scatter points plotted with confidence halos and classification colors.
 */
@Composable
fun SoilDensityDepthChart(
    anomalies: List<DetectedAnomaly>,
    currentDepth: Float = 3.2f,
    currentDensity: Float = 1.95f,
    onSaveToDatabase: ((DetectedAnomaly) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedAnomaly by remember { mutableStateOf<DetectedAnomaly?>(null) }
    var chartMode by remember { mutableStateOf("realtime") } // "realtime" or "strata"

    val infiniteTransition = rememberInfiniteTransition(label = "d3_chart_anim")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("d3_soil_density_depth_chart_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070D1E)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "TOPRAK YOĞUNLUĞU & DERİNLİK DAĞILIMI",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "D3 Gerçek Zamanlı Jeofizik Stratigrafi Grafiği",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = chartMode == "realtime",
                        onClick = { chartMode = "realtime" },
                        label = { Text("Canlı", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF00E5FF)
                        )
                    )
                    FilterChip(
                        selected = chartMode == "strata",
                        onClick = { chartMode = "strata" },
                        label = { Text("Tabakalar", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFF59E0B)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Telemetry Readout Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yoğunluk (ρ): ${String.format(Locale.US, "%.2f", currentDensity)} g/cm³",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Prob Derinliği: ${String.format(Locale.US, "%.1f", currentDepth)}m",
                    color = Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${anomalies.size} Hedef",
                    color = Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // D3 Canvas Graph Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF02040A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(anomalies) {
                            detectTapGestures { tapOffset ->
                                val padLeft = 45f
                                val padRight = 20f
                                val padTop = 20f
                                val padBottom = 30f
                                val chartW = size.width - padLeft - padRight
                                val chartH = size.height - padTop - padBottom

                                // Find closest anomaly to tap
                                val hit = anomalies.minByOrNull { anom ->
                                    val anomDensity = 1.3f + (anom.signalStrength / 100f) * 1.5f
                                    val px = padLeft + ((anomDensity - 1.0f) / 2.2f) * chartW
                                    val py = padTop + (anom.depthMeters / 10.0f) * chartH
                                    val dx = tapOffset.x - px
                                    val dy = tapOffset.y - py
                                    dx * dx + dy * dy
                                }
                                if (hit != null) {
                                    val anomDensity = 1.3f + (hit.signalStrength / 100f) * 1.5f
                                    val px = padLeft + ((anomDensity - 1.0f) / 2.2f) * chartW
                                    val py = padTop + (hit.depthMeters / 10.0f) * chartH
                                    val dist = kotlin.math.sqrt((tapOffset.x - px) * (tapOffset.x - px) + (tapOffset.y - py) * (tapOffset.y - py))
                                    if (dist < 40f) {
                                        selectedAnomaly = hit
                                    } else {
                                        selectedAnomaly = null
                                    }
                                }
                            }
                        }
                ) {
                    val padLeft = 45f
                    val padRight = 20f
                    val padTop = 20f
                    val padBottom = 30f
                    val chartW = size.width - padLeft - padRight
                    val chartH = size.height - padTop - padBottom

                    // 1. Gridlines & Axis Lines
                    val depthTicks = listOf(0f, 2.5f, 5.0f, 7.5f, 10.0f)
                    val densityTicks = listOf(1.0f, 1.5f, 2.0f, 2.5f, 3.0f)

                    // Horizontal depth gridlines
                    depthTicks.forEach { d ->
                        val y = padTop + (d / 10f) * chartH
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(padLeft, y),
                            end = Offset(padLeft + chartW, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }

                    // Vertical density gridlines
                    densityTicks.forEach { rho ->
                        val x = padLeft + ((rho - 1.0f) / 2.2f) * chartW
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(x, padTop),
                            end = Offset(x, padTop + chartH),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }

                    // 2. Background Geological Strata Shading (if enabled)
                    if (chartMode == "strata") {
                        // Soil Strata 1: 0 - 2.5m (Humus & Alluvium)
                        drawRect(
                            color = Color(0x1A10B981),
                            topLeft = Offset(padLeft, padTop),
                            size = Size(chartW, (2.5f / 10f) * chartH)
                        )
                        // Soil Strata 2: 2.5 - 5.5m (Clay & Cavity Layer)
                        drawRect(
                            color = Color(0x1A00E5FF),
                            topLeft = Offset(padLeft, padTop + (2.5f / 10f) * chartH),
                            size = Size(chartW, (3.0f / 10f) * chartH)
                        )
                        // Soil Strata 3: 5.5 - 10m (Bedrock & Mineral Host)
                        drawRect(
                            color = Color(0x1AF59E0B),
                            topLeft = Offset(padLeft, padTop + (5.5f / 10f) * chartH),
                            size = Size(chartW, (4.5f / 10f) * chartH)
                        )
                    }

                    // 3. Continuous D3 S-Curve Density Gradient Area (Real-time Geophysical Gradient)
                    val curvePath = Path()
                    val areaPath = Path()

                    val sampleCount = 40
                    areaPath.moveTo(padLeft, padTop + chartH)

                    for (i in 0..sampleCount) {
                        val frac = i.toFloat() / sampleCount.toFloat()
                        val d = frac * 10f // depth 0 to 10m
                        val perturbation = sin((frac * 3.5f + pulseProgress) * kotlin.math.PI.toFloat()) * 0.12f
                        val baseRho = 1.35f + frac * 1.3f + perturbation
                        val x = padLeft + (((baseRho - 1.0f) / 2.2f).coerceIn(0f, 1f)) * chartW
                        val y = padTop + frac * chartH

                        if (i == 0) {
                            curvePath.moveTo(x, y)
                            areaPath.lineTo(x, y)
                        } else {
                            curvePath.lineTo(x, y)
                            areaPath.lineTo(x, y)
                        }
                    }
                    areaPath.lineTo(padLeft, padTop + chartH)
                    areaPath.close()

                    // Draw translucent area fill under the curve
                    drawPath(
                        path = areaPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0x2200E5FF), Color(0x0500E5FF)),
                            startX = padLeft,
                            endX = padLeft + chartW
                        )
                    )

                    // Draw D3 line stroke
                    drawPath(
                        path = curvePath,
                        color = Color(0xFF00E5FF),
                        style = Stroke(width = 2.5f)
                    )

                    // 4. Live Sensor Probe Depth Marker (Horizontal Scanning Ray)
                    val liveY = padTop + (currentDepth.coerceIn(0f, 10f) / 10f) * chartH
                    val liveX = padLeft + ((currentDensity.coerceIn(1.0f, 3.2f) - 1.0f) / 2.2f) * chartW

                    drawLine(
                        color = Color(0xFFF59E0B).copy(alpha = 0.7f),
                        start = Offset(padLeft, liveY),
                        end = Offset(padLeft + chartW, liveY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    // Probe position glowing dot
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 6f,
                        center = Offset(liveX, liveY)
                    )
                    drawCircle(
                        color = Color(0xFFF59E0B).copy(alpha = 0.4f),
                        radius = 12f * (1f + pulseProgress * 0.5f),
                        center = Offset(liveX, liveY),
                        style = Stroke(width = 1.5f)
                    )

                    // 5. Detected Anomaly Scatter Nodes
                    for (anom in anomalies) {
                        // Estimate soil density based on classification & signal
                        val anomDensity = when (anom.classification) {
                            TargetClassification.PRECIOUS -> 2.65f + (anom.signalStrength / 200f)
                            TargetClassification.VOID -> 1.15f + (anom.depthMeters * 0.05f)
                            TargetClassification.FERROUS -> 2.45f
                            TargetClassification.MINERAL -> 2.25f
                            else -> 1.85f
                        }.coerceIn(1.0f, 3.2f)

                        val px = padLeft + ((anomDensity - 1.0f) / 2.2f) * chartW
                        val py = padTop + (anom.depthMeters.coerceIn(0f, 10f) / 10f) * chartH

                        val nodeColor = Color(anom.classification.colorHex)
                        val isSelected = (selectedAnomaly?.id == anom.id)

                        // Outer halo
                        drawCircle(
                            color = nodeColor.copy(alpha = if (isSelected) 0.6f else 0.25f),
                            radius = if (isSelected) 14f else 9f,
                            center = Offset(px, py)
                        )
                        // Inner solid node
                        drawCircle(
                            color = nodeColor,
                            radius = if (isSelected) 7f else 4.5f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2f,
                            center = Offset(px, py)
                        )
                    }

                    // 6. Draw Axes Lines
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(padLeft, padTop),
                        end = Offset(padLeft, padTop + chartH),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(padLeft, padTop + chartH),
                        end = Offset(padLeft + chartW, padTop + chartH),
                        strokeWidth = 2f
                    )
                }

                // Y-Axis Depth Labels (0m, 5m, 10m)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 6.dp, top = 14.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.0m", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("2.5m", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("5.0m", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("7.5m", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("10.0m", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                // X-Axis Density Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 45.dp, end = 20.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1.0 (Boşluk/Kum)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Text("1.8 (Alüvyon)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Text("2.5 (Kaya)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Text("3.2 (Metal/Dipol)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }

            // Anomaly Selection Tooltip & Save Action
            if (selectedAnomaly != null) {
                val anom = selectedAnomaly!!
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(anom.classification.colorHex))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(Color(anom.classification.colorHex), CircleShape))
                                Text(anom.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("• ${anom.classification.label}", color = Color(anom.classification.colorHex), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Derinlik: ${String.format(Locale.US, "%.2f", anom.depthMeters)}m | Genlik: %${anom.signalStrength} | Güven: %${anom.depthConfidence}",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }

                        if (onSaveToDatabase != null) {
                            IconButton(
                                onClick = { onSaveToDatabase(anom) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "SQLite Veritabanına Kaydet",
                                    tint = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chart Footer Notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Y-Aksı: Derinlik (0-10m) • X-Aksı: Toprak Yoğunluğu (ρ g/cm³)",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
                Text(
                    text = "D3 Voksel Matrisi",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
