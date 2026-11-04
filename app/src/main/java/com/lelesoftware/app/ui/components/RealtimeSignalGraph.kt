package com.lelesoftware.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

/**
 * Data sample representing a single measurement point in time
 */
data class SignalSample(
    val timestampMs: Long,
    val sensorA: Float,     // Upper fluxgate reading in nT
    val sensorB: Float,     // Lower fluxgate reading in nT
    val gradientDelta: Float // Delta = B - A or A - B in nT
)

@Composable
fun RealtimeSignalGraph(
    samples: List<SignalSample>,
    currentDelta: Float,
    currentSensorA: Float,
    currentSensorB: Float,
    isStreaming: Boolean,
    onToggleStreaming: () -> Unit,
    onClearSamples: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-scale toggle (default: true for automatic fitting)
    var isAutoScale by remember { mutableStateOf(true) }
    var manualScale by remember { mutableFloatStateOf(500f) }
    // Channel mode: "delta" (gradient only), "all" (A, B and Delta)
    var channelMode by remember { mutableStateOf("delta") }

    // Dynamic auto-scale calculation based on min/max of current data buffer
    val targetScale = remember(samples, currentDelta, isAutoScale, manualScale, channelMode) {
        if (!isAutoScale) {
            manualScale
        } else {
            if (samples.isEmpty()) {
                maxOf(abs(currentDelta) * 1.25f, 40f)
            } else {
                val maxDelta = samples.maxOfOrNull { abs(it.gradientDelta) } ?: 0f
                val maxBufferVal = if (channelMode == "all") {
                    val maxA = samples.maxOfOrNull { abs(it.sensorA - 48100f) } ?: 0f
                    val maxB = samples.maxOfOrNull { abs(it.sensorB - 48100f) } ?: 0f
                    maxOf(maxDelta, maxA, maxB, abs(currentDelta))
                } else {
                    maxOf(maxDelta, abs(currentDelta))
                }
                // Add 20% margin headroom so data never touches the edges, minimum floor 30 nT
                (maxBufferVal * 1.20f).coerceAtLeast(30f)
            }
        }
    }

    // Smooth animated transition for Y-axis scale to prevent abrupt visual jumps
    val rangeScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "auto_scale_anim"
    )

    // Pulsing beacon on the leading edge (live marker)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_lead")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_lead_alpha"
    )

    // Calculate peaks in the current buffer
    val peakPositive = samples.maxOfOrNull { it.gradientDelta } ?: 0f
    val peakNegative = samples.minOfOrNull { it.gradientDelta } ?: 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_signal_graph_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "2D Gerçek Zamanlı Sinyal Osiloskopu",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (isAutoScale) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                                ) {
                                    Text(
                                        text = "OTO ÖLÇEK",
                                        color = Color(0xFF10B981),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "50 Hz Sürekli Akış • Dinamik Y-Ekseni Otomatik Uyum",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleStreaming,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isStreaming) "Durdur" else "Başlat",
                            tint = if (isStreaming) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onClearSamples,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Temizle",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Range Scale & Channel Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Range Scale Chips (With Auto-Fit Mode)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = isAutoScale,
                        onClick = { isAutoScale = true },
                        label = {
                            Text(
                                text = if (isAutoScale) "Oto: ±${rangeScale.toInt()} nT" else "Oto Ölçek",
                                fontSize = 10.sp,
                                fontWeight = if (isAutoScale) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )

                    listOf(100f, 500f, 2000f).forEach { r ->
                        FilterChip(
                            selected = !isAutoScale && manualScale == r,
                            onClick = {
                                isAutoScale = false
                                manualScale = r
                            },
                            label = { Text("±${r.toInt()} nT", fontSize = 10.sp) }
                        )
                    }
                }

                // Channel Filter
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = channelMode == "delta",
                        onClick = { channelMode = "delta" },
                        label = { Text("Δ Gradyan", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = channelMode == "all",
                        onClick = { channelMode = "all" },
                        label = { Text("A + B + Δ", fontSize = 10.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main 2D Canvas Graph Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF040817))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 42.dp, end = 12.dp, top = 14.dp, bottom = 22.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val zeroY = h / 2f

                    // 1. Threshold Zones (Shaded areas)
                    // Gold / Precious Metal Zone (Above +65% of scale)
                    val goldThresholdY = zeroY - (0.65f * zeroY)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x22EAB308), Color(0x00EAB308)),
                            startY = 0f,
                            endY = goldThresholdY
                        ),
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(w, goldThresholdY)
                    )

                    // Void / Cavity Zone (Below -40% of scale)
                    val voidThresholdY = zeroY + (0.40f * zeroY)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x003B82F6), Color(0x253B82F6)),
                            startY = voidThresholdY,
                            endY = h
                        ),
                        topLeft = Offset(0f, voidThresholdY),
                        size = androidx.compose.ui.geometry.Size(w, h - voidThresholdY)
                    )

                    // 2. Horizontal Reference Grid Lines
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                    // +Range line
                    val yTop = zeroY - (zeroY * 0.9f)
                    drawLine(Color(0x25FFFFFF), Offset(0f, yTop), Offset(w, yTop), pathEffect = dashedEffect)

                    // +Half line
                    val yUpper = zeroY - (zeroY * 0.45f)
                    drawLine(Color(0x18FFFFFF), Offset(0f, yUpper), Offset(w, yUpper), pathEffect = dashedEffect)

                    // Zero Baseline (Equator)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0x4038BDF8), Color(0x9938BDF8), Color(0x4038BDF8))
                        ),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.5f
                    )

                    // -Half line
                    val yLower = zeroY + (zeroY * 0.45f)
                    drawLine(Color(0x18FFFFFF), Offset(0f, yLower), Offset(w, yLower), pathEffect = dashedEffect)

                    // -Range line
                    val yBottom = zeroY + (zeroY * 0.9f)
                    drawLine(Color(0x25FFFFFF), Offset(0f, yBottom), Offset(w, yBottom), pathEffect = dashedEffect)

                    // 3. Vertical Time Division Grid Lines
                    val timeDivisions = 5
                    for (i in 0..timeDivisions) {
                        val gx = (w / timeDivisions) * i
                        drawLine(
                            color = Color(0x15FFFFFF),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            pathEffect = dashedEffect
                        )
                    }

                    // 4. Draw Waveform Curves
                    if (samples.isNotEmpty()) {
                        val maxPoints = 80
                        val displaySamples = if (samples.size > maxPoints) samples.takeLast(maxPoints) else samples
                        val stepX = w / (maxPoints - 1).coerceAtLeast(1)
                        val startOffset = w - (displaySamples.size - 1) * stepX

                        fun yForSignal(nT: Float): Float {
                            val normalized = (nT / rangeScale).coerceIn(-1.1f, 1.1f)
                            return zeroY - (normalized * (zeroY * 0.9f))
                        }

                        // Optional Sensor A (Upper) & Sensor B (Lower) background curves
                        if (channelMode == "all") {
                            val pathA = Path()
                            val pathB = Path()

                            displaySamples.forEachIndexed { idx, s ->
                                val px = startOffset + idx * stepX
                                val pyA = yForSignal(s.sensorA - 48100f)
                                val pyB = yForSignal(s.sensorB - 48100f)

                                if (idx == 0) {
                                    pathA.moveTo(px, pyA)
                                    pathB.moveTo(px, pyB)
                                } else {
                                    pathA.lineTo(px, pyA)
                                    pathB.lineTo(px, pyB)
                                }
                            }
                            drawPath(pathA, color = Color(0x6606B6D4), style = Stroke(width = 1.2f))
                            drawPath(pathB, color = Color(0x6610B981), style = Stroke(width = 1.2f))
                        }

                        // Gradient Delta Waveform (Main Hero Curve)
                        val deltaPath = Path()
                        val fillPath = Path()

                        displaySamples.forEachIndexed { idx, s ->
                            val px = startOffset + idx * stepX
                            val py = yForSignal(s.gradientDelta)

                            if (idx == 0) {
                                deltaPath.moveTo(px, py)
                                fillPath.moveTo(px, zeroY)
                                fillPath.lineTo(px, py)
                            } else {
                                val prevX = startOffset + (idx - 1) * stepX
                                val prevY = yForSignal(displaySamples[idx - 1].gradientDelta)
                                val midX = (prevX + px) / 2f
                                deltaPath.cubicTo(midX, prevY, midX, py, px, py)
                                fillPath.cubicTo(midX, prevY, midX, py, px, py)
                            }

                            if (idx == displaySamples.size - 1) {
                                fillPath.lineTo(px, zeroY)
                                fillPath.close()
                            }
                        }

                        // Shaded gradient fill beneath the curve towards zero
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x3DF59E0B), Color(0x05F59E0B)),
                                startY = 0f,
                                endY = h
                            )
                        )

                        // Glowing Main Line
                        drawPath(
                            path = deltaPath,
                            color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                            style = Stroke(width = 5.5f)
                        )
                        drawPath(
                            path = deltaPath,
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF06B6D4), Color(0xFFF59E0B), Color(0xFFEF4444))
                            ),
                            style = Stroke(width = 2.4f)
                        )

                        // Leading Pulse Marker at the newest point (Rightmost)
                        val lastX = w
                        val lastY = yForSignal(currentDelta)

                        drawCircle(
                            color = Color(0xFFF59E0B).copy(alpha = pulseAlpha * 0.4f),
                            radius = 12f,
                            center = Offset(lastX, lastY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.5f,
                            center = Offset(lastX, lastY)
                        )
                        drawLine(
                            color = Color(0xFFF59E0B).copy(alpha = pulseAlpha * 0.8f),
                            start = Offset(lastX, 0f),
                            end = Offset(lastX, h),
                            strokeWidth = 1.2f
                        )
                    }
                }

                // Y-Axis Scale Labels (Left Edge Overlay)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 6.dp, top = 8.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("+${rangeScale.toInt()}", color = Color(0xFFEAB308), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("+${(rangeScale / 2).toInt()}", color = Color(0xFF94A3B8), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("0 nT", color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("-${(rangeScale / 2).toInt()}", color = Color(0xFF94A3B8), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("-${rangeScale.toInt()}", color = Color(0xFF3B82F6), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                // X-Axis Time Labels (Bottom Edge Overlay)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 44.dp, end = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-10s", color = Color(0xFF64748B), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("-7s", color = Color(0xFF64748B), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("-5s", color = Color(0xFF64748B), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("-2s", color = Color(0xFF64748B), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("CANLI", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Telemetry HUD Footer (Metrics & Peak Values)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Delta Reading
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (currentDelta > 0) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Text("Δ Manyetik Gradyan:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        text = String.format(Locale.US, "%+.1f nT", currentDelta),
                        color = if (currentDelta > 20f) Color(0xFFF59E0B) else if (currentDelta < -20f) Color(0xFF38BDF8) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Peak positive & negative
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = String.format(Locale.US, "Maks: +%.0f nT", peakPositive),
                        color = Color(0xFFEAB308),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "Min: %.0f nT", peakNegative),
                        color = Color(0xFF3B82F6),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
