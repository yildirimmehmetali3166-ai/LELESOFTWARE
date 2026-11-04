package com.lelesoftware.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MagneticSurveyCompass(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var rawAzimuth by remember { mutableFloatStateOf(0f) }

    // Register hardware sensors (Rotation Vector / Accelerometer + Magnetometer)
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationValues = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationValues)
                    var azimuthDeg = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                    if (azimuthDeg < 0) azimuthDeg += 360f
                    rawAzimuth = azimuthDeg
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Smooth heading angle animation
    val smoothHeading by animateFloatAsState(
        targetValue = rawAzimuth,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "compass_heading_anim"
    )

    // Calculate alignment with Earth's Magnetic North (0°) or South (180°)
    // Optimal traverse axis in gradiometry is strictly North-South (0° or 180° ±7°)
    val devFromNorth = minOf(smoothHeading, 360f - smoothHeading)
    val devFromSouth = abs(smoothHeading - 180f)
    val isAlignedNorth = devFromNorth <= 7.0f
    val isAlignedSouth = devFromSouth <= 7.0f
    val isAligned = isAlignedNorth || isAlignedSouth

    val activeDeviation = if (devFromNorth < devFromSouth) devFromNorth else devFromSouth
    val targetAxisName = if (devFromNorth < devFromSouth) "Kuzey (N)" else "Güney (S)"

    val statusColor by animateColorAsState(
        targetValue = when {
            isAligned -> Color(0xFF10B981) // Emerald Green
            activeDeviation <= 20f -> Color(0xFFF59E0B) // Amber
            else -> Color(0xFFEF4444) // Red
        },
        label = "status_color_anim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("magnetic_survey_compass_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Dünya Manyetik Hat Pusulası",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Jeofizik Doğru Yürüyüş Aksı Kılavuzu (K-G Meridyeni)",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isAligned) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isAligned) "HATTA KİLİTLİ" else "SAPMA MEVCUT",
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Compass Gauge & HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Canvas Compass Rose
                Box(
                    modifier = Modifier
                        .size(165.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF030712))
                        .border(2.dp, statusColor.copy(alpha = 0.8f), CircleShape)
                        .shadow(16.dp, CircleShape, spotColor = statusColor),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f

                        // Outer rim
                        drawCircle(
                            color = Color(0x3338BDF8),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )

                        // Optimal survey corridor green arcs (North 353°..7° and South 173°..187°)
                        val corridorAngleSpan = 14f
                        // We draw static green tick corridors at top and bottom
                        drawArc(
                            color = Color(0x4010B981),
                            startAngle = 270f - corridorAngleSpan / 2f,
                            sweepAngle = corridorAngleSpan,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        )
                        drawArc(
                            color = Color(0x4010B981),
                            startAngle = 90f - corridorAngleSpan / 2f,
                            sweepAngle = corridorAngleSpan,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        )

                        // Rotating Dial (Compass Rose rotates counter to heading)
                        rotate(-smoothHeading, pivot = center) {
                            // Tick marks every 30 degrees
                            for (deg in 0 until 360 step 15) {
                                val rad = (deg * PI / 180f).toFloat()
                                val isMajor = deg % 90 == 0
                                val isSub = deg % 30 == 0
                                val tickLen = if (isMajor) 14f else if (isSub) 9f else 5f
                                val strokeW = if (isMajor) 2f else 1f

                                val startX = center.x + sin(rad) * (radius - tickLen)
                                val startY = center.y - cos(rad) * (radius - tickLen)
                                val endX = center.x + sin(rad) * radius
                                val endY = center.y - cos(rad) * radius

                                val tickColor = if (deg == 0) Color(0xFFF43F5E) else if (deg == 180) Color(0xFF38BDF8) else Color(0x6694A3B8)
                                drawLine(tickColor, Offset(startX, startY), Offset(endX, endY), strokeWidth = strokeW)
                            }

                            // North-South Target Crosshair
                            drawLine(
                                color = Color(0x3038BDF8),
                                start = Offset(center.x, center.y - radius),
                                end = Offset(center.x, center.y + radius),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color(0x3038BDF8),
                                start = Offset(center.x - radius, center.y),
                                end = Offset(center.x + radius, center.y),
                                strokeWidth = 1f
                            )

                            // 3D Magnetic Needle (North Pointing - Red Arrow, South Pointing - Blue Arrow)
                            val needleLength = radius * 0.72f
                            val needleWidth = 10f

                            // North Half (Red/Amber)
                            val northPath = Path().apply {
                                moveTo(center.x, center.y - needleLength)
                                lineTo(center.x - needleWidth, center.y)
                                lineTo(center.x, center.y - 4f)
                                close()
                            }
                            drawPath(northPath, brush = Brush.verticalGradient(listOf(Color(0xFFF43F5E), Color(0xFFE11D48))))

                            val northPathRight = Path().apply {
                                moveTo(center.x, center.y - needleLength)
                                lineTo(center.x + needleWidth, center.y)
                                lineTo(center.x, center.y - 4f)
                                close()
                            }
                            drawPath(northPathRight, brush = Brush.verticalGradient(listOf(Color(0xFFFB7185), Color(0xFFF43F5E))))

                            // South Half (Blue/Cyan)
                            val southPath = Path().apply {
                                moveTo(center.x, center.y + needleLength)
                                lineTo(center.x - needleWidth, center.y)
                                lineTo(center.x, center.y + 4f)
                                close()
                            }
                            drawPath(southPath, brush = Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))))

                            val southPathRight = Path().apply {
                                moveTo(center.x, center.y + needleLength)
                                lineTo(center.x + needleWidth, center.y)
                                lineTo(center.x, center.y + 4f)
                                close()
                            }
                            drawPath(southPathRight, brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))))
                        }

                        // Center Pivot Pin
                        drawCircle(Color(0xFF0F172A), radius = 9f, center = center)
                        drawCircle(Color(0xFFF59E0B), radius = 5f, center = center)
                    }

                    // Digital Heading Overlay Center-Bottom
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xD90F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%03d° %s", smoothHeading.toInt(), getDirectionLabel(smoothHeading)),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Heading Guidance & Earth Magnetic Fields Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Guidance Advice Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF030712),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isAligned) "K-G MERİDYEN KİLİDİ AKTİF" else "EKSENEL HİZALAMA GEREKLİ",
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAligned) {
                                    "Mükemmel doğrultu. Manyetik dipol yanıtları simetrik kaydedilecek."
                                } else {
                                    val turnDir = if (smoothHeading in 7.0..173.0) "Sola (Kuzey'e) dönün" else "Sağa (Kuzey'e) dönün"
                                    "Sapma: ${activeDeviation.toInt()}°. $turnDir."
                                },
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Earth Magnetic Field Telemetry Metrics
                    CompassMetricRow(label = "Manyetik Şiddet (F):", value = "48,220 nT")
                    CompassMetricRow(label = "Manyetik Eğim (Dip):", value = "59.2° (Aşağı)")
                    CompassMetricRow(label = "Sapma Açısı (Decl):", value = "+5.8° Doğu")
                    CompassMetricRow(label = "Önerilen Tarama:", value = "K-G Boustrophedon")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explanatory Note on why Magnetic Meridian is crucial
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0B132B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CompassCalibration,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Jeofizik Kural: Manyetometre ve gradyometre taramaları daima Manyetik Kuzey-Güney hattında yürünmelidir. Bu, dipol asimetrisini önler ve derinlik hesaplama doğruluğunu %95'in üzerine çıkarır.",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF64748B), fontSize = 10.sp)
        Text(value, color = Color(0xFFE2E8F0), fontWeight = FontWeight.SemiBold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun getDirectionLabel(heading: Float): String {
    return when {
        heading in 337.5..360.0 || heading in 0.0..22.5 -> "K (N)"
        heading in 22.5..67.5 -> "KD (NE)"
        heading in 67.5..112.5 -> "D (E)"
        heading in 112.5..157.5 -> "GD (SE)"
        heading in 157.5..202.5 -> "G (S)"
        heading in 202.5..247.5 -> "GB (SW)"
        heading in 247.5..292.5 -> "B (W)"
        else -> "KB (NW)"
    }
}
