package com.lelesoftware.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.PaletteType
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ScanPoint
import com.lelesoftware.app.util.AudioSynthesizer
import com.lelesoftware.app.util.ColorScales

/**
 * TwoDScanViewer (Optimized 2D Geophysical Heatmap & Grid Viewer)
 *
 * Performans ve Güvenlik İyileştirmeleri:
 * 1. Kare olmayan (10x20, 15x30, vb.) matrislerde ArrayIndexOutOfBoundsException
 *    hatasını önlemek için dinamik width, height ve getOrNull sınır denetimi.
 * 2. Bilinear interpolasyon ve blok çizimlerinde renk matrisi önbelleklenerek 60 FPS akıcılık.
 * 3. Dokunma ve hücre seçimlerinde matris dışına çıkılmasını önleyen matematiksel coerce/clamp mantığı.
 */
@Composable
fun TwoDScanViewer(
    grid: ScanGrid,
    palette: PaletteType,
    smoothInterpolation: Boolean,
    showValues: Boolean,
    selectedCell: Pair<Int, Int>,
    onSelectCell: (Int, Int) -> Unit,
    anomalies: List<DetectedAnomaly> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Dynamic dimensions for non-square matrices
    val width = grid.width.coerceAtLeast(1)
    val height = grid.height.coerceAtLeast(1)

    // Pre-calculate cell colors outside onDraw pass
    val precomputedColors = remember(grid, palette) {
        Array(height) { y ->
            val row = grid.points.getOrNull(y) ?: emptyList()
            Array(width) { x ->
                val signal = row.getOrNull(x)?.filteredSignal ?: 0f
                ColorScales.getPaletteColor(signal, palette)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(width.toFloat() / height.toFloat().coerceAtLeast(1f))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF030712))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .testTag("two_d_scan_viewer_box")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(grid) {
                    detectTapGestures { offset ->
                        val cellW = size.width / width.toFloat()
                        val cellH = size.height / height.toFloat()
                        val gx = (offset.x / cellW).toInt().coerceIn(0, width - 1)
                        val gy = (offset.y / cellH).toInt().coerceIn(0, height - 1)

                        onSelectCell(gx, gy)

                        val pt = grid.points.getOrNull(gy)?.getOrNull(gx)
                        if (pt != null) {
                            AudioSynthesizer.playSignalTone(pt.filteredSignal)
                        }
                    }
                }
        ) {
            val renderW = size.width
            val renderH = size.height
            val cellW = renderW / width.toFloat()
            val cellH = renderH / height.toFloat()

            if (smoothInterpolation) {
                // Subdivided interpolation grid (optimized steps)
                val subStepsX = width * 3
                val subStepsY = height * 3
                val subW = renderW / subStepsX.toFloat()
                val subH = renderH / subStepsY.toFloat()

                for (sy in 0 until subStepsY) {
                    val gy = (sy.toFloat() / subStepsY) * (height - 1)
                    val y0 = gy.toInt().coerceIn(0, height - 1)
                    val y1 = (y0 + 1).coerceIn(0, height - 1)
                    val wy = gy - y0

                    val r0 = grid.points.getOrNull(y0)
                    val r1 = grid.points.getOrNull(y1)

                    for (sx in 0 until subStepsX) {
                        val gx = (sx.toFloat() / subStepsX) * (width - 1)
                        val x0 = gx.toInt().coerceIn(0, width - 1)
                        val x1 = (x0 + 1).coerceIn(0, width - 1)
                        val wx = gx - x0

                        val s00 = r0?.getOrNull(x0)?.filteredSignal ?: 0f
                        val s10 = r0?.getOrNull(x1)?.filteredSignal ?: s00
                        val s01 = r1?.getOrNull(x0)?.filteredSignal ?: s00
                        val s11 = r1?.getOrNull(x1)?.filteredSignal ?: s01

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
                // Discrete Blocks with safe dynamic boundaries
                for (y in 0 until height) {
                    val colorRow = precomputedColors.getOrNull(y)
                    for (x in 0 until width) {
                        val col = colorRow?.getOrNull(x) ?: Color.DarkGray
                        drawRect(
                            color = col,
                            topLeft = Offset(x * cellW, y * cellH),
                            size = Size(cellW, cellH)
                        )
                        drawRect(
                            color = Color(0x22000000),
                            topLeft = Offset(x * cellW, y * cellH),
                            size = Size(cellW, cellH),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            // Draw anomaly highlight rings
            for (anom in anomalies) {
                val ax = anom.gridX.coerceIn(0, width - 1)
                val ay = anom.gridY.coerceIn(0, height - 1)
                val anomCenter = Offset(ax * cellW + cellW * 0.5f, ay * cellH + cellH * 0.5f)
                val isSelected = (ax == selectedCell.first && ay == selectedCell.second)

                drawCircle(
                    color = Color(anom.classification.colorHex).copy(alpha = 0.4f),
                    radius = cellW * 0.45f,
                    center = anomCenter
                )
                drawCircle(
                    color = Color(anom.classification.colorHex),
                    radius = cellW * 0.35f,
                    center = anomCenter,
                    style = Stroke(width = if (isSelected) 2.5f else 1.5f)
                )
            }

            // Draw selected cell reticle cursor
            val safeSelX = selectedCell.first.coerceIn(0, width - 1)
            val safeSelY = selectedCell.second.coerceIn(0, height - 1)
            val selTopLeft = Offset(safeSelX * cellW, safeSelY * cellH)

            drawRect(
                color = Color.White,
                topLeft = selTopLeft,
                size = Size(cellW, cellH),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 4f,
                center = Offset(selTopLeft.x + cellW * 0.5f, selTopLeft.y + cellH * 0.5f)
            )
        }
    }
}
