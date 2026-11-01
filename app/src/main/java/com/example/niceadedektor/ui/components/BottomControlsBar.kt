package com.example.niceadedektor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.niceadedektor.model.CameraPreset
import com.example.niceadedektor.model.PaletteType
import com.example.niceadedektor.model.ViewMode
import java.util.Locale

@Composable
fun BottomControlsBar(
    palette: PaletteType,
    onSelectPalette: (PaletteType) -> Unit,
    viewMode: ViewMode,
    onSelectViewMode: (ViewMode) -> Unit,
    depthCutMeters: Float,
    onChangeDepthCut: (Float) -> Unit,
    thresholdFilter: Float,
    onChangeThreshold: (Float) -> Unit,
    wireframe: Boolean,
    onToggleWireframe: () -> Unit,
    cameraPreset: CameraPreset,
    onSelectCamera: (CameraPreset) -> Unit,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("bottom_controls_bar"),
        colors = CardDefaults.cardColors(containerColor = Color(0xEB0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Bar & Collapse Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Text(
                        text = "3D Görselleştirme & Filtre Kontrolleri",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                IconButton(onClick = onToggleCollapse, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (isCollapsed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isCollapsed) "Genişlet" else "Daralt",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            AnimatedVisibility(
                visible = !isCollapsed,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Palettes Horizontal Scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaletteType.entries.forEach { p ->
                            FilterChip(
                                selected = palette == p,
                                onClick = { onSelectPalette(p) },
                                label = { Text(p.label, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }

                    // View Modes & Wireframe
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ViewMode.entries.forEach { v ->
                            FilterChip(
                                selected = viewMode == v,
                                onClick = { onSelectViewMode(v) },
                                label = { Text(v.label, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        FilterChip(
                            selected = wireframe,
                            onClick = onToggleWireframe,
                            label = { Text("Tel Izgara", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }

                    // Sliders: Depth Cut & Threshold Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Derinlik Kesiti", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(String.format(Locale.US, "%.1fm", depthCutMeters), color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = depthCutMeters,
                                onValueChange = onChangeDepthCut,
                                valueRange = 1f..12f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFF59E0B),
                                    activeTrackColor = Color(0xFFF59E0B)
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sinyal Eşiği", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("${thresholdFilter.toInt()}%", color = Color(0xFF06B6D4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = thresholdFilter,
                                onValueChange = onChangeThreshold,
                                valueRange = 0f..80f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF06B6D4),
                                    activeTrackColor = Color(0xFF06B6D4)
                                )
                            )
                        }
                    }

                    // Camera Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CameraPreset.entries.forEach { cam ->
                            FilterChip(
                                selected = cameraPreset == cam,
                                onClick = { onSelectCamera(cam) },
                                label = { Text(cam.label, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
