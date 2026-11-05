package com.lelesoftware.app.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.SoilType
import com.lelesoftware.app.model.TargetClassification
import com.lelesoftware.app.util.SignalDepthCalculator
import java.util.Locale
import kotlin.math.abs

/**
 * AnomalyDetailBottomSheet (Faz 2)
 *
 * 2D matris veya 3D Voxel üzerinde bir anomali veya noktaya dokunulduğunda açılan;
 * - FWHM & Manyetik Inversion ile Otomatik Derinlik Hesabı (m)
 * - Voxel Küme Entegrasyonu ile Tahmini Hacim (m³) ve Form Analizi
 * - Manyetik Gradyan Şiddeti (ΔnT) ve Tepe Genliği
 * - Anomali Türü (Boşluk / Ferromanyetik / Kıymetli Metal / Mineral)
 * - Ayarlanabilir Sensör Yerden Yüksekliği Slider'ı (h_sensor: 0.05m - 0.50m)
 * - Zemin Tipi Kalibrasyonu
 * - SQLite Room Veritabanına Anlık Kayıt Butonu
 * sunan Material 3 BottomSheet bileşeni.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnomalyDetailBottomSheet(
    grid: ScanGrid,
    anomaly: DetectedAnomaly?,
    gridX: Int,
    gridY: Int,
    onDismiss: () -> Unit,
    onSaveToDatabase: ((DetectedAnomaly) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sensorHeight by remember { mutableFloatStateOf(0.20f) }
    var selectedSoilType by remember { mutableStateOf(SoilType.STANDARD) }
    var isSavedLocally by remember { mutableStateOf(false) }

    // Derinlik & Hacim Inversion Motorunu çalıştır
    val depthResult = remember(grid, gridX, gridY, sensorHeight, selectedSoilType) {
        SignalDepthCalculator.calculateAnomalyDepth(
            grid = grid,
            centerX = gridX,
            centerY = gridY,
            soilType = selectedSoilType,
            sensorHeightMeters = sensorHeight
        )
    }

    val point = grid.points[gridY.coerceIn(0, grid.height - 1)][gridX.coerceIn(0, grid.width - 1)]
    val classification = anomaly?.classification ?: point.classification
    val badgeColor = Color(classification.colorHex)

    val currentAnomalyInstance = remember(anomaly, depthResult, gridX, gridY) {
        anomaly?.copy(
            depthMeters = depthResult.estimatedDepthMeters,
            volumeM3 = depthResult.estimatedVolumeM3,
            depthConfidence = depthResult.confidencePercent,
            fwhmMeters = depthResult.fwhmMeters
        ) ?: DetectedAnomaly(
            id = "anom-${grid.id}-$gridX-$gridY",
            title = "${classification.label} Hedefi",
            classification = classification,
            xMeters = gridX * grid.stepDistanceMeters,
            yMeters = gridY * grid.stepDistanceMeters,
            gridX = gridX,
            gridY = gridY,
            depthMeters = depthResult.estimatedDepthMeters,
            volumeM3 = depthResult.estimatedVolumeM3,
            signalStrength = point.filteredSignal.toInt(),
            description = depthResult.formFactor,
            peakAmplitude = point.filteredSignal,
            fwhmMeters = depthResult.fwhmMeters,
            minDepthMeters = depthResult.minDepthMeters,
            maxDepthMeters = depthResult.maxDepthMeters,
            depthConfidence = depthResult.confidencePercent,
            calculationFormula = depthResult.method
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        modifier = modifier.testTag("anomaly_detail_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = badgeColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, badgeColor),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (classification) {
                                    TargetClassification.VOID -> Icons.Default.Layers
                                    TargetClassification.PRECIOUS -> Icons.Default.ViewInAr
                                    TargetClassification.FERROUS -> Icons.Default.Sensors
                                    else -> Icons.Default.Terrain
                                },
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = classification.label,
                                color = badgeColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "(${gridX}, ${gridY})",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = depthResult.formFactor,
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sheet_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color(0xFF94A3B8))
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B))

            // Primary 4 Metrics Grid (Depth, Volume, Delta nT, Confidence)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Depth (FWHM & Inversion)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Hesaplanan Derinlik", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = String.format(Locale.US, "%.2f m", depthResult.estimatedDepthMeters),
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = String.format(Locale.US, "±%.2f m (FWHM)", abs(depthResult.maxDepthMeters - depthResult.estimatedDepthMeters)),
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }

                // Metric 2: Estimated Volume (m³)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Tahmini Hacim", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = String.format(Locale.US, "%.2f m³", depthResult.estimatedVolumeM3),
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Voxel Entegrasyonu",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 3: Magnetic Intensity
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Manyetik Şiddet (ΔnT)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = String.format(Locale.US, "%.1f nT", depthResult.peakAmplitudeNanoTesla),
                            color = if (point.filteredSignal >= 0) Color(0xFF10B981) else Color(0xFF38BDF8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = String.format(Locale.US, "Genlik: %+.1f %%", depthResult.peakAmplitudePercent),
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }

                // Metric 4: Model Confidence
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Inversion Güveni", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = "%${depthResult.confidencePercent}",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = depthResult.method.split("(")[0].trim(),
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Sensor Height Slider (h_sensor parameter: 0.05m to 0.50m)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Height, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                            Text("Sensör Yerden Yüksekliği (h_sensor)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(
                            text = String.format(Locale.US, "%.2f m", sensorHeight),
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Peters Half-Slope formülü: z = k·FWHM - h_sensor. Sensör probunun zemin mesafesini dengeleyerek net anomali derinliğini hassaslaştırır.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Slider(
                        value = sensorHeight,
                        onValueChange = { sensorHeight = it },
                        valueRange = 0.05f..0.50f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF59E0B),
                            activeTrackColor = Color(0xFFF59E0B),
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("sensor_height_slider")
                    )
                }
            }

            // Soil Attenuation Type Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Zemin Tipi & Sönümleme Katsayısı:",
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF00E5FF)
                            )
                        )
                    }
                }
            }

            // Depth Attenuation Decay Curve
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Manyetik Dipol Alanı Sönümleme Eğrisi (B(z) ∝ M/z³)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val pts = depthResult.profileCurve
                        if (pts.isEmpty()) return@Canvas

                        val maxAmp = pts.maxOfOrNull { it.second }?.coerceAtLeast(1f) ?: 1f
                        val path = Path()

                        pts.forEachIndexed { i, pt ->
                            val x = (pt.first / 5.0f) * w
                            val y = h - ((pt.second / maxAmp) * (h - 10f) + 5f)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFF00E5FF),
                            style = Stroke(width = 2.5f)
                        )

                        // Draw marker at estimated depth
                        val markerX = (depthResult.estimatedDepthMeters / 5.0f).coerceIn(0f, 1f) * w
                        drawLine(
                            color = Color(0xFFF59E0B),
                            start = Offset(markerX, 0f),
                            end = Offset(markerX, h),
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // Save to SQLite Room Database Button
            Button(
                onClick = {
                    onSaveToDatabase?.invoke(currentAnomalyInstance)
                    isSavedLocally = true
                },
                enabled = !isSavedLocally,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_to_sqlite_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSavedLocally) Color(0xFF10B981) else Color(0xFFF59E0B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isSavedLocally) Icons.Default.Check else Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSavedLocally) "SQLite Veritabanına Kaydedildi" else "SQLite Veritabanına Kaydet (Çevrimdışı)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
