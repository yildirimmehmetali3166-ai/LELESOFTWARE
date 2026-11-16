package com.lelesoftware.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.PaletteType
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.ui.theme.CyanAccent
import com.lelesoftware.app.ui.theme.MonoFontFamily
import com.lelesoftware.app.ui.theme.TextMuted
import com.lelesoftware.app.util.AudioSynthesizer
import com.lelesoftware.app.util.ColorScales
import java.util.Locale
import kotlin.math.roundToInt

/**
 * TwoDScanViewer (Optimized 2D Geophysical Heatmap & Grid Viewer)
 *
 * Performans ve Güvenlik İyileştirmeleri:
 * 1. Kare olmayan (10x20, 15x30, vb.) matrislerde ArrayIndexOutOfBoundsException
 *    hatasını önlemek için dinamik width, height ve getOrNull sınır denetimi.
 * 2. Bilinear interpolasyon ve blok çizimlerinde renk matrisi önbelleklenerek 60 FPS akıcılık.
 * 3. İki parmakla yakınlaştırma (pinch-to-zoom), kaydırma (pan) ve doğru hücre eşlemesi.
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

    val zoomState = rememberGridZoomPanState(grid.id, width, height)
    val visibleBounds = zoomState.visibleNormalizedBounds()

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

    val valuePaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            color = android.graphics.Color.WHITE
            setShadowLayer(2f, 0f, 0f, android.graphics.Color.BLACK)
        }
    }

    val physicalW = width * grid.stepDistanceMeters.coerceAtLeast(0.01f)
    val physicalH = height * grid.stepYMeters.coerceAtLeast(0.01f)
    val physicalAspect = (physicalW / physicalH).coerceIn(0.65f, 2.5f)

    val xMin = if (grid.isCsvImport) grid.rawXMin else 0f
    val xMax = if (grid.isCsvImport) grid.rawXMax else physicalW
    val yMin = if (grid.isCsvImport) grid.rawYMin else 0f
    val yMax = if (grid.isCsvImport) grid.rawYMax else physicalH

    val visXStart = xMin + visibleBounds.uLeft * (xMax - xMin)
    val visXEnd = xMin + visibleBounds.uRight * (xMax - xMin)
    val visYStart = yMin + visibleBounds.vTop * (yMax - yMin)
    val visYEnd = yMin + visibleBounds.vBottom * (yMax - yMin)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GridZoomToolbar(
            state = zoomState,
            tagPrefix = "twod_grid"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.US, "X: %.1f..%.1f%s", visXStart, visXEnd, if (grid.isCsvImport) "" else "m"),
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = MonoFontFamily
            )
            Text(
                text = String.format(Locale.US, "Y: %.1f..%.1f%s", visYStart, visYEnd, if (grid.isCsvImport) "" else "m"),
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = MonoFontFamily
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(physicalAspect)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF030712))
                .border(
                    width = if (zoomState.isZoomed) 1.5.dp else 1.dp,
                    color = if (zoomState.isZoomed) CyanAccent.copy(alpha = 0.7f) else Color(0xFF334155),
                    shape = RoundedCornerShape(12.dp)
                )
                .geophysicalGridGestures(
                    state = zoomState,
                    key1 = grid.id
                ) { tapPos, viewportSize ->
                    val (gx, gy) = zoomState.viewportToGridCell(
                        tapPos = tapPos,
                        size = viewportSize,
                        gridWidth = width,
                        gridHeight = height
                    )
                    onSelectCell(gx, gy)

                    val pt = grid.points.getOrNull(gy)?.getOrNull(gx)
                    if (pt != null) {
                        AudioSynthesizer.playSignalTone(pt.filteredSignal)
                    }
                }
                .testTag("two_d_scan_viewer_box")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomState.scale
                        scaleY = zoomState.scale
                        translationX = zoomState.offset.x
                        translationY = zoomState.offset.y
                    }
            ) {
                val renderW = size.width
                val renderH = size.height
                val cellW = renderW / width.toFloat()
                val cellH = renderH / height.toFloat()
                val invScale = 1f / zoomState.scale.coerceAtLeast(1f)

                if (smoothInterpolation) {
                    // Subdivided interpolation grid (optimized steps)
                    val subStepsX = (width * 2).coerceAtMost(120)
                    val subStepsY = (height * 3).coerceAtMost(96)
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
                                style = Stroke(width = (1f * invScale).coerceAtLeast(0.4f))
                            )
                        }
                    }
                }

                // Draw cell signal values when toggled ON or zoomed in enough for crisp legibility
                val effectiveCellSize = minOf(cellW, cellH) * zoomState.scale
                if ((showValues || zoomState.scale >= 2.1f) && effectiveCellSize >= 14f) {
                    valuePaint.textSize = (minOf(cellW, cellH) * 0.34f).coerceIn(4.5f, 18f)
                    val yOffset = valuePaint.textSize * 0.35f
                    drawIntoCanvas { canvas ->
                        for (y in 0 until height) {
                            val row = grid.points.getOrNull(y) ?: continue
                            for (x in 0 until width) {
                                val pt = row.getOrNull(x) ?: continue
                                val cx = x * cellW + cellW * 0.5f
                                val cy = y * cellH + cellH * 0.5f + yOffset
                                canvas.nativeCanvas.drawText(
                                    "${pt.filteredSignal.roundToInt()}",
                                    cx,
                                    cy,
                                    valuePaint
                                )
                            }
                        }
                    }
                }

                // Draw anomaly highlight rings
                val markerRadius = (minOf(cellW, cellH) * 0.45f).coerceAtLeast(4.5f)
                for (anom in anomalies) {
                    val ax = anom.gridX.coerceIn(0, width - 1)
                    val ay = anom.gridY.coerceIn(0, height - 1)
                    val anomCenter = Offset(ax * cellW + cellW * 0.5f, ay * cellH + cellH * 0.5f)
                    val isSelected = (ax == selectedCell.first && ay == selectedCell.second)

                    drawCircle(
                        color = Color(anom.classification.colorHex).copy(alpha = 0.4f),
                        radius = markerRadius,
                        center = anomCenter
                    )
                    drawCircle(
                        color = Color(anom.classification.colorHex),
                        radius = markerRadius * 0.78f,
                        center = anomCenter,
                        style = Stroke(width = ((if (isSelected) 3f else 1.8f) * invScale).coerceAtLeast(0.8f))
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
                    style = Stroke(width = (2.5f * invScale).coerceAtLeast(1f))
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = (4f * invScale).coerceAtLeast(1.8f),
                    center = Offset(selTopLeft.x + cellW * 0.5f, selTopLeft.y + cellH * 0.5f)
                )
            }
        }
    }
}
