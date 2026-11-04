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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.TargetClassification
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Animated High-Tech Radar Pulse Visualizer for LELE SOFTWARE.
 * Features:
 * - Concentric expanding sonar pulse ripples out from the center core
 * - 360-degree rotating radar sweep arm with trailing phosphorescent glow
 * - Subterranean anomaly ping indicators that flare when swept
 * - Real-time range rings (1m, 2m, 3m, 4m, 5m) and cardinal azimuth ticks
 * - Magnetic gradient telemetry and detection status HUD
 */
@Composable
fun AnimatedRadarPulse(
    isScanning: Boolean,
    gradientDelta: Float,
    anomalies: List<DetectedAnomaly> = emptyList(),
    modifier: Modifier = Modifier,
    rangeMeters: Float = 5.0f,
    onRangeChange: ((Float) -> Unit)? = null
) {
    var selectedRange by remember { mutableFloatStateOf(rangeMeters) }

    // Infinite transitions for rotating sweep and expanding ripples
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse_transitions")

    // Continuous 360-degree rotating beam sweep
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isScanning) 2800 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    // Three staggered expanding ripple waves
    val waveProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress_1"
    )

    val waveProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress_2"
    )

    val waveProgress3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress_3"
    )

    // Center emitter beacon pulse
    val centerBeaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "center_beacon_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("animated_radar_pulse_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070D1E)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isScanning) Color(0xFF10B981) else Color(0xFFF59E0B),
                                shape = CircleShape
                            )
                    )
                    Column {
                        Text(
                            text = "LELE SOFTWARE MANYETİK RADAR",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (isScanning) "AKTİF JEOFİZİK ALAN TARAMASI" else "RADAR BEKLEMEDE (HAZIR)",
                            color = if (isScanning) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Telemetry Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = if (gradientDelta > 15f) Color(0xFFF59E0B) else if (gradientDelta < -15f) Color(0xFF38BDF8) else Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Δ: ${String.format(Locale.US, "%+.1f", gradientDelta)} nT",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Radar Circular Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF02040A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .testTag("radar_canvas_container"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = (size.minDimension / 2f) * 0.92f

                    // 1. Concentric Range Calibration Rings
                    val ringCount = 5
                    for (i in 1..ringCount) {
                        val ringRadius = (maxRadius / ringCount) * i
                        val isOuter = (i == ringCount)
                        drawCircle(
                            color = if (isOuter) Color(0xFF00E5FF).copy(alpha = 0.4f) else Color(0xFF1E293B),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(
                                width = if (isOuter) 1.8f else 1.0f,
                                pathEffect = if (!isOuter && i % 2 == 1) PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) else null
                            )
                        )
                    }

                    // 2. Cardinal Crosshairs & Azimuth Spokes
                    // 0° (North-South) and 90° (East-West)
                    drawLine(
                        color = Color(0x3300E5FF),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1.2f
                    )
                    drawLine(
                        color = Color(0x3300E5FF),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1.2f
                    )

                    // 45° Diagonal spokes
                    val diagLen = maxRadius * 0.85f
                    for (angleDeg in listOf(45f, 135f, 225f, 315f)) {
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val endX = center.x + diagLen * cos(rad).toFloat()
                        val endY = center.y + diagLen * sin(rad).toFloat()
                        drawLine(
                            color = Color(0x1A00E5FF),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 0.8f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
                        )
                    }

                    // 3. Expanding Sonar Wave Ripples (when scanning is active or in standby pulse)
                    if (isScanning) {
                        drawExpandingWave(center, maxRadius, waveProgress1, Color(0xFF00E5FF))
                        drawExpandingWave(center, maxRadius, waveProgress2, Color(0xFF10B981))
                        drawExpandingWave(center, maxRadius, waveProgress3, Color(0xFFF59E0B))
                    } else {
                        drawExpandingWave(center, maxRadius, waveProgress1, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    }

                    // 4. Rotating Radar Sweep Beam & Trailing Phosphorus Glow
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEnd = Offset(
                        center.x + maxRadius * cos(sweepRad).toFloat(),
                        center.y + maxRadius * sin(sweepRad).toFloat()
                    )

                    // Draw trailing phosphorescent sweep sector (approx 45 degrees)
                    val trailSteps = 16
                    for (step in 1..trailSteps) {
                        val stepRatio = step.toFloat() / trailSteps.toFloat()
                        val trailingAngle = sweepAngle - (stepRatio * 45f)
                        val tRad = Math.toRadians(trailingAngle.toDouble())
                        val tEnd = Offset(
                            center.x + maxRadius * cos(tRad).toFloat(),
                            center.y + maxRadius * sin(tRad).toFloat()
                        )
                        val trailAlpha = (1f - stepRatio).pow(2.0f) * (if (isScanning) 0.35f else 0.18f)
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = trailAlpha),
                            start = center,
                            end = tEnd,
                            strokeWidth = 4f
                        )
                    }

                    // Main Sweep Arm Line
                    drawLine(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFFFF), Color(0xFF00E5FF), Color(0xFF0284C7)),
                            center = center,
                            radius = maxRadius
                        ),
                        start = center,
                        end = sweepEnd,
                        strokeWidth = if (isScanning) 2.5f else 1.8f
                    )

                    // Glowing sweep beam head dot
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = sweepEnd
                    )
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                        radius = 8f,
                        center = sweepEnd
                    )

                    // 5. Detected Anomaly Blips & Magnetic Pings
                    // Map simulated or real anomaly points to radar polar coordinates
                    val displayAnomalies = if (anomalies.isNotEmpty()) {
                        anomalies
                    } else {
                        // Default subterranean magnetic targets for live feedback
                        listOf(
                            DetectedAnomaly(
                                id = "demo-gold",
                                title = "Altın Küpü",
                                classification = TargetClassification.PRECIOUS,
                                xMeters = 1.8f,
                                yMeters = 1.5f,
                                gridX = 3,
                                gridY = 3,
                                depthMeters = 2.4f,
                                volumeM3 = 0.45f,
                                signalStrength = 88,
                                description = "Yüksek ferromanyetik dipol"
                            ),
                            DetectedAnomaly(
                                id = "demo-void",
                                title = "Mezar Odası / Boşluk",
                                classification = TargetClassification.VOID,
                                xMeters = -2.1f,
                                yMeters = -1.2f,
                                gridX = 2,
                                gridY = 4,
                                depthMeters = 3.6f,
                                volumeM3 = 2.8f,
                                signalStrength = 75,
                                description = "Negatif manyetik kavitasyon"
                            ),
                            DetectedAnomaly(
                                id = "demo-ferrous",
                                title = "Demir / Çapa",
                                classification = TargetClassification.FERROUS,
                                xMeters = 2.4f,
                                yMeters = -2.0f,
                                gridX = 5,
                                gridY = 2,
                                depthMeters = 1.2f,
                                volumeM3 = 0.2f,
                                signalStrength = 64,
                                description = "Manyetik metal hedef"
                            )
                        )
                    }

                    for (anom in displayAnomalies) {
                        // Calculate radius from center based on selectedRange (normalized 0..1)
                        val dist = sqrt(anom.xMeters.pow(2) + anom.yMeters.pow(2))
                        val normDist = (dist / selectedRange).coerceIn(0.15f, 0.92f)
                        val anomAngleRad = atan2(anom.yMeters.toDouble(), anom.xMeters.toDouble())
                        var anomAngleDeg = Math.toDegrees(anomAngleRad).toFloat()
                        if (anomAngleDeg < 0) anomAngleDeg += 360f

                        val blipCenter = Offset(
                            center.x + (maxRadius * normDist) * cos(anomAngleRad).toFloat(),
                            center.y + (maxRadius * normDist) * sin(anomAngleRad).toFloat()
                        )

                        // Angular distance between sweep arm and anomaly
                        val angleDelta = kotlin.math.abs(sweepAngle - anomAngleDeg)
                        val isSwept = angleDelta < 18f || (360f - angleDelta) < 18f
                        val flareIntensity = if (isSwept && isScanning) 1.0f else 0.45f

                        val baseColor = when (anom.classification) {
                            TargetClassification.PRECIOUS -> Color(0xFFF59E0B) // Gold
                            TargetClassification.VOID -> Color(0xFF00E5FF) // Cyan / Void
                            TargetClassification.FERROUS -> Color(0xFFEF4444) // Red
                            TargetClassification.MINERAL -> Color(0xFF8B5CF6) // Purple
                            else -> Color(0xFF94A3B8)
                        }

                        // Pulsating Halo on swept detection
                        if (isSwept && isScanning) {
                            drawCircle(
                                color = baseColor.copy(alpha = 0.8f),
                                radius = 18f,
                                center = blipCenter,
                                style = Stroke(width = 2.5f)
                            )
                            drawCircle(
                                color = baseColor.copy(alpha = 0.35f),
                                radius = 28f,
                                center = blipCenter
                            )
                        }

                        // Target Core Dot
                        drawCircle(
                            color = baseColor.copy(alpha = flareIntensity),
                            radius = if (isSwept) 7f else 5f,
                            center = blipCenter
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = flareIntensity),
                            radius = 2.5f,
                            center = blipCenter
                        )
                    }

                    // 6. Central Sonar Fluxgate Sensor Core
                    drawCircle(
                        color = Color(0xFF02040A),
                        radius = 24f,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                        radius = 24f,
                        center = center,
                        style = Stroke(width = 1.8f)
                    )
                    drawCircle(
                        color = if (isScanning) Color(0xFF00E5FF).copy(alpha = centerBeaconAlpha * 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.3f),
                        radius = 16f,
                        center = center
                    )
                    drawCircle(
                        color = if (isScanning) Color(0xFF00E5FF) else Color(0xFFF59E0B),
                        radius = 6f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = center
                    )
                }

                // Cardinal Labels (N, E, S, W)
                Text(
                    text = "KUZEY (N)",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
                Text(
                    text = "GÜNEY (S)",
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
                Text(
                    text = "BATI (W)",
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                )
                Text(
                    text = "DOĞU (E)",
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )

                // Range Tag in Corner
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xCC0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = "Menzil: ${selectedRange.toInt()}m",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Range Selector & Anomaly Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Range Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3.0f, 5.0f, 10.0f).forEach { r ->
                        FilterChip(
                            selected = selectedRange == r,
                            onClick = {
                                selectedRange = r
                                onRangeChange?.invoke(r)
                            },
                            label = { Text("${r.toInt()}m", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF00E5FF)
                            )
                        )
                    }
                }

                // Legend Pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LegendIndicator(color = Color(0xFFF59E0B), label = "Metal")
                    LegendIndicator(color = Color(0xFF00E5FF), label = "Boşluk")
                    LegendIndicator(color = Color(0xFFEF4444), label = "Demir")
                }
            }
        }
    }
}

/**
 * Draws an expanding circular ripple wave with smooth alpha falloff.
 */
private fun DrawScope.drawExpandingWave(
    center: Offset,
    maxRadius: Float,
    progress: Float,
    color: Color
) {
    if (progress <= 0f || progress >= 1f) return
    val currentRadius = maxRadius * progress
    val alpha = (1f - progress).pow(1.5f) * 0.75f
    val strokeWidth = (3.0f * (1f - progress * 0.5f)).coerceAtLeast(1.0f)

    drawCircle(
        color = color.copy(alpha = alpha),
        radius = currentRadius,
        center = center,
        style = Stroke(width = strokeWidth)
    )
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
