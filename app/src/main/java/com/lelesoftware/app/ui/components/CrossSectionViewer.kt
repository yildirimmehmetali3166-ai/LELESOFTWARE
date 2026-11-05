package com.lelesoftware.app.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ScanPoint
import com.lelesoftware.app.model.TargetClassification

/**
 * CrossSectionViewer (2D / 3D Jeofizik Kesit ve Profil Çizici)
 *
 * Sınır Aşımı (Bounds Safety) Korumaları:
 * 1. Kare olmayan (10x20, 15x30, 20x50 vb.) matrislerde dinamik grid.height ve grid.width denetimi.
 * 2. getOrNull ve safe clamping ile sıfır ArrayIndexOutOfBoundsException garantisi.
 * 3. 0 veya 1 elemanlı kesitlerde bölme hatasını önleyen coerceAtLeast(1) koruması.
 */
@Composable
fun CrossSectionViewer(
    grid: ScanGrid,
    selectedCell: Pair<Int, Int>,
    sliceAxis: String,
    onToggleAxis: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalRows = grid.points.size.coerceAtLeast(1)
    val totalCols = (grid.points.firstOrNull()?.size ?: grid.width).coerceAtLeast(1)

    // Clamped coordinates to strictly prevent out of bounds
    val safeX = selectedCell.first.coerceIn(0, totalCols - 1)
    val safeY = selectedCell.second.coerceIn(0, totalRows - 1)

    // Safely extract slice points based on selected axis
    val slicePoints = remember(grid, safeX, safeY, sliceAxis) {
        if (sliceAxis == "X") {
            // Horizontal row slice (Y fixed, all X columns)
            val row = grid.points.getOrNull(safeY) ?: emptyList()
            if (row.isNotEmpty()) row else List(totalCols) { x ->
                ScanPoint(x, safeY, 0, 0f, 0f, TargetClassification.NEUTRAL, 0, 0L)
            }
        } else {
            // Vertical column slice (X fixed, all Y rows)
            grid.points.mapIndexed { y, row ->
                row.getOrNull(safeX) ?: ScanPoint(safeX, y, 0, 0f, 0f, TargetClassification.NEUTRAL, 0, 0L)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cross_section_viewer_card"),
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
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF38BDF8))
                    Column {
                        Text(
                            text = if (sliceAxis == "X") "Yatay Profil Kesiti (Y = $safeY, Genişlik: $totalCols adım)" else "Düşey Profil Kesiti (X = $safeX, Derinlik: $totalRows adım)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Matris Boyutu: ${totalCols}x${totalRows} (${(totalCols * grid.stepDistanceMeters).toInt()}m x ${(totalRows * grid.stepDistanceMeters).toInt()}m)",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = sliceAxis == "X",
                        onClick = { onToggleAxis("X") },
                        label = { Text("X Kesiti", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = sliceAxis == "Y",
                        onClick = { onToggleAxis("Y") },
                        label = { Text("Y Kesiti", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Section Plot
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
                    val groundY = h * 0.35f

                    // Ground zero reference line
                    drawLine(Color(0xFF475569), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 1.5f)

                    val count = slicePoints.size
                    if (count < 2) return@Canvas

                    val stepPx = w / (count - 1).toFloat()
                    val signalPath = Path()
                    val depthPath = Path()

                    for (i in 0 until count) {
                        val pt = slicePoints[i]
                        val px = i * stepPx
                        val sigY = groundY - (pt.filteredSignal / 100f) * (groundY * 0.85f)
                        if (i == 0) signalPath.moveTo(px, sigY) else signalPath.lineTo(px, sigY)

                        val dY = groundY + (pt.depth / 6f) * (h - groundY)
                        if (i == 0) depthPath.moveTo(px, dY) else depthPath.lineTo(px, dY)

                        // Vertical probe reference indicator at active point
                        val isActive = (sliceAxis == "X" && i == safeX) || (sliceAxis == "Y" && i == safeY)
                        if (isActive) {
                            drawLine(Color(0xFFF59E0B), Offset(px, 0f), Offset(px, h), strokeWidth = 2f)
                            drawCircle(Color(0xFFF59E0B), radius = 5f, center = Offset(px, sigY))
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
                        Text("Sinyal Genliği (ΔnT)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFEAB308), CircleShape))
                        Text("Derinlik Penetrasyonu", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
