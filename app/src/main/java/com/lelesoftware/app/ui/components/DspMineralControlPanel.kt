package com.lelesoftware.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelesoftware.app.dsp.Bno085KalmanFusion
import com.lelesoftware.app.dsp.DspMineralFilter
import java.util.Locale
import kotlin.math.abs

/**
 * DspMineralControlPanel (Faz 1)
 *
 * Çift FGM Fluxgate gradyometre sensörlerinden gelen ham ΔnT verisindeki yüksek
 * mineralli zemin (ıslak kil, kiremit, bazalt) gürültülerini filtreleyen ve
 * BNO085 IMU yön/eğim sapmalarını kompanse eden Material 3 kontrol paneli.
 *
 * - Z-Score Eşik (Threshold) Slider'ı (1.0 - 5.0)
 * - Hareketli Ortalama & Medyan Dinamik Baseline Takibi
 * - BNO085 9-Eksen IMU Kalman Filtresi & Eğim (Tilt) Kompansasyonu Aç/Kapat
 * - Canlı Sinyal / Baseline / Z-Score Telemetrisi
 */
@Composable
fun DspMineralControlPanel(
    rawDeltaNt: Float = 14.8f,
    currentPitch: Float = 1.4f,
    currentRoll: Float = -0.8f,
    currentYaw: Float = 42.0f,
    onFilterParametersChanged: ((zThreshold: Float, imuEnabled: Boolean, tiltCompensationEnabled: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var zThreshold by remember { mutableFloatStateOf(2.5f) }
    var baselineSpeed by remember { mutableFloatStateOf(0.18f) }
    var isImuKalmanEnabled by remember { mutableStateOf(true) }
    var isTiltCompensationEnabled by remember { mutableStateOf(true) }

    // DSP Engine Instance
    val dspFilter = remember { DspMineralFilter(windowSize = 11) }
    val imuFusion = remember { Bno085KalmanFusion() }

    // Filter computation
    val dspOutput = remember(rawDeltaNt, zThreshold, baselineSpeed) {
        dspFilter.processReading(
            rawDeltaNt = rawDeltaNt,
            zThreshold = zThreshold,
            baselineTrackingSpeed = baselineSpeed
        )
    }

    val imuOutput = remember(currentPitch, currentRoll, currentYaw, rawDeltaNt, isImuKalmanEnabled) {
        if (isImuKalmanEnabled) {
            imuFusion.update(
                pitchDeg = currentPitch,
                rollDeg = currentRoll,
                yawDeg = currentYaw,
                rawDeltaNt = rawDeltaNt,
                stepDistanceMeters = 0.5f
            )
        } else {
            Bno085KalmanFusion.FusionResult(
                rawPitch = currentPitch,
                rawRoll = currentRoll,
                rawYaw = currentYaw,
                filteredPitch = currentPitch,
                filteredRoll = currentRoll,
                filteredYaw = currentYaw,
                tiltCompensationFactor = 1.0f,
                compensatedDeltaNt = rawDeltaNt,
                projectedStepX = 0f,
                projectedStepY = 0.5f,
                tiltWarning = false
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dsp_mineral_control_panel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.FilterAlt,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "DSP Z-Score Mineral & IMU Filtresi",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Çift Fluxgate FGM + BNO085 9-Eksen IMU Fusion",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (dspOutput.isAnomaly) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (dspOutput.isAnomaly) Color(0xFF10B981) else Color(0xFFF59E0B))
                ) {
                    Text(
                        text = if (dspOutput.isAnomaly) "ANOMALİ TESPİT" else "MİNERAL BASTIRILDI",
                        color = if (dspOutput.isAnomaly) Color(0xFF10B981) else Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B))

            // DSP Threshold Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Z-Score Mineral Basılama Eşiği (Z_thresh):",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f σ", zThreshold),
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "Düşük eşik (<2.0) hassas anomalileri gösterir; yüksek eşik (>3.0) ıslak kil ve kiremit gürültüsünü filtreler.",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )

                Slider(
                    value = zThreshold,
                    onValueChange = {
                        zThreshold = it
                        onFilterParametersChanged?.invoke(zThreshold, isImuKalmanEnabled, isTiltCompensationEnabled)
                    },
                    valueRange = 1.0f..5.0f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFF59E0B),
                        activeTrackColor = Color(0xFFF59E0B),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("z_score_threshold_slider")
                )
            }

            // Real-Time Signal Metrics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Ham ΔnT", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(
                            text = String.format(Locale.US, "%.1f nT", rawDeltaNt),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Column {
                        Text("Zemin Baseline (μ)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(
                            text = String.format(Locale.US, "%.1f nT", dspOutput.baselineMean),
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Column {
                        Text("Z-Score Değeri", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(
                            text = String.format(Locale.US, "%.2f", dspOutput.zScore),
                            color = if (dspOutput.isAnomaly) Color(0xFF10B981) else Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Column {
                        Text("Filtrelenmiş Çıktı", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(
                            text = String.format(Locale.US, "%.1f nT", dspOutput.filteredDelta),
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // BNO085 IMU Sensor Fusion & Tilt Compensation Switch
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.CompassCalibration,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("BNO085 IMU Kalman Drift Kompansasyonu", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Adım sarsıntısı ve yön kaymasını düzeltir", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }

                    Switch(
                        checked = isImuKalmanEnabled,
                        onCheckedChange = {
                            isImuKalmanEnabled = it
                            onFilterParametersChanged?.invoke(zThreshold, isImuKalmanEnabled, isTiltCompensationEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("imu_kalman_switch")
                    )
                }

                // IMU Telemetry row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("Pitch (Eğim)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text(
                                text = String.format(Locale.US, "%.1f°", imuOutput.filteredPitch),
                                color = if (abs(imuOutput.filteredPitch) > 12f) Color(0xFFEF4444) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("Roll (Yatış)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text(
                                text = String.format(Locale.US, "%.1f°", imuOutput.filteredRoll),
                                color = if (abs(imuOutput.filteredRoll) > 12f) Color(0xFFEF4444) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("Yaw (Heading)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text(
                                text = String.format(Locale.US, "%.1f°", imuOutput.filteredYaw),
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("Eğim Faktörü", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text(
                                text = String.format(Locale.US, "%.2fx", imuOutput.tiltCompensationFactor),
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (imuOutput.tiltWarning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        Text(
                            text = "Prob dik duruş açısını aştı (>12°)! Eğim kompansasyonu devrede.",
                            color = Color(0xFFFCA5A5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Reset baseline button
            OutlinedButton(
                onClick = {
                    dspFilter.reset()
                    imuFusion.reset()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("reset_dsp_filter_button"),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("DSP Baseline & IMU Kalibrasyonunu Sıfırla", color = Color(0xFFE2E8F0), fontSize = 12.sp)
            }
        }
    }
}
