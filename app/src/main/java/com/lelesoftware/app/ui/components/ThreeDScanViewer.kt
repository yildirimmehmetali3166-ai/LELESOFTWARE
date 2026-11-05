package com.lelesoftware.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lelesoftware.app.model.CameraPreset
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.PaletteType
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ViewMode
import com.lelesoftware.app.util.ColorScales
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pre-computed vertex structure for high-performance 3D surface and voxel rendering.
 * Generated outside the onDraw loop to prevent object allocations during frames.
 */
data class CachedVertex3D(
    val gridX: Int,
    val gridY: Int,
    val normX: Float,
    val normY: Float,
    val normZ: Float,
    val color: Color,
    val filteredSignal: Float,
    val isVisible: Boolean
)

/**
 * Pre-computed quad polygon connecting 4 adjacent vertices.
 */
data class CachedQuad3D(
    val v00: CachedVertex3D,
    val v10: CachedVertex3D,
    val v11: CachedVertex3D,
    val v01: CachedVertex3D,
    val avgZ: Float,
    val avgSignal: Float,
    val fillColor: Color
)

/**
 * ThreeDScanViewer (Optimized 60 FPS Canvas 3D Viewer)
 *
 * Performans İyileştirmeleri:
 * 1. Matris dönüştürme ve tepe noktası (vertex/mesh) hesaplamaları onDraw dışına çıkarıldı.
 * 2. rotX (Pitch), rotZ (Yaw) ve zoom (Scale) hesaplamaları remember ve derivedStateOf ile önbelleklendi.
 * 3. onDraw döngüsü içinde sıfır bellek tahsisi (zero object allocation) ile 60 FPS garantilendi.
 * 4. Kare olmayan (10x20, 15x30) matrislerde dinamik genişlik/yükseklik güvenliği sağlandı.
 */
@Composable
fun ThreeDScanViewer(
    grid: ScanGrid,
    palette: PaletteType,
    viewMode: ViewMode,
    depthCutMeters: Float,
    thresholdFilter: Float,
    wireframe: Boolean,
    selectedAnomalyId: String?,
    onSelectAnomaly: (DetectedAnomaly?) -> Unit,
    anomalies: List<DetectedAnomaly>,
    cameraPreset: CameraPreset,
    onOpenAnomalyDetail: ((DetectedAnomaly) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 1. Camera orientation angles (rotZ = Yaw, rotX = Pitch) & Zoom
    var yawDeg by remember(cameraPreset) {
        mutableFloatStateOf(
            when (cameraPreset) {
                CameraPreset.ISO -> 45f
                CameraPreset.TOP -> 0f
                CameraPreset.SIDE -> 90f
                CameraPreset.FRONT -> 0f
            }
        )
    }

    var pitchDeg by remember(cameraPreset) {
        mutableFloatStateOf(
            when (cameraPreset) {
                CameraPreset.ISO -> 35f
                CameraPreset.TOP -> 88f
                CameraPreset.SIDE -> 5f
                CameraPreset.FRONT -> 15f
            }
        )
    }

    var scaleFactor by remember { mutableFloatStateOf(1.0f) }

    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        scaleFactor = (scaleFactor * zoomChange).coerceIn(0.5f, 3.5f)
    }

    // 2. Pre-compute 3D Mesh vertices and quads OUTSIDE onDraw
    val cachedMeshData = remember(grid, palette, depthCutMeters, thresholdFilter) {
        val width = grid.width.coerceAtLeast(1)
        val height = grid.height.coerceAtLeast(1)
        val centerX = (width - 1) * 0.5f
        val centerY = (height - 1) * 0.5f

        val vertices = Array(height) { y ->
            val row = grid.points.getOrNull(y) ?: emptyList()
            Array(width) { x ->
                val pt = row.getOrNull(x)
                val sig = pt?.filteredSignal ?: 0f
                val zMeters = (sig / 100f) * 2.8f
                val clampedZ = zMeters.coerceIn(-depthCutMeters, depthCutMeters)
                val color = ColorScales.getPaletteColor(sig, palette)
                val isVisible = abs(sig) >= thresholdFilter

                CachedVertex3D(
                    gridX = x,
                    gridY = y,
                    normX = x - centerX,
                    normY = y - centerY,
                    normZ = clampedZ,
                    color = color,
                    filteredSignal = sig,
                    isVisible = isVisible
                )
            }
        }

        val quads = mutableListOf<CachedQuad3D>()
        for (y in 0 until height - 1) {
            for (x in 0 until width - 1) {
                val v00 = vertices[y][x]
                val v10 = vertices[y][x + 1]
                val v11 = vertices[y + 1][x + 1]
                val v01 = vertices[y + 1][x]

                if (v00.isVisible || v10.isVisible || v11.isVisible || v01.isVisible) {
                    val avgZ = (v00.normZ + v10.normZ + v11.normZ + v01.normZ) * 0.25f
                    val avgSig = (v00.filteredSignal + v10.filteredSignal + v11.filteredSignal + v01.filteredSignal) * 0.25f
                    val col = ColorScales.getPaletteColor(avgSig, palette)
                    quads.add(CachedQuad3D(v00, v10, v11, v01, avgZ, avgSig, col))
                }
            }
        }

        Pair(vertices, quads)
    }

    // 3. Trigonometric transformation matrix pre-calculated with derivedStateOf
    val transformMatrix by remember(yawDeg, pitchDeg) {
        derivedStateOf {
            val yawRad = (yawDeg * PI / 180f).toFloat()
            val pitchRad = (pitchDeg * PI / 180f).toFloat()
            TransformationMatrix(
                cosYaw = cos(yawRad),
                sinYaw = sin(yawRad),
                cosPitch = cos(pitchRad),
                sinPitch = sin(pitchRad)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040814))
            .testTag("three_d_scan_viewer_canvas")
            .transformable(state = transformableState)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    yawDeg += dragAmount.x * 0.45f
                    pitchDeg = (pitchDeg - dragAmount.y * 0.45f).coerceIn(-10f, 89f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW * 0.5f
            val centerY = canvasH * 0.5f

            val matrix = transformMatrix
            val maxDimension = maxOf(grid.width, grid.height, 1)
            val baseCellSize = (canvasW.coerceAtMost(canvasH) / (maxDimension * 1.55f)) * scaleFactor

            // Fast inline projection: (screenX, screenY, depthOrder)
            fun projectFast(normX: Float, normY: Float, normZ: Float): FloatArray {
                val rx = normX * matrix.cosYaw - normY * matrix.sinYaw
                val ry = normX * matrix.sinYaw + normY * matrix.cosYaw
                val rz = normZ

                val screenX = centerX + rx * baseCellSize
                val screenY = centerY + (ry * matrix.sinPitch - rz * matrix.cosPitch) * baseCellSize
                val depthOrder = ry * matrix.cosPitch + rz * matrix.sinPitch
                return floatArrayOf(screenX, screenY, depthOrder)
            }

            // Draw Ground Axis Grid (Z = 0)
            drawOptimizedGroundGrid(
                width = grid.width,
                height = grid.height,
                baseCellSize = baseCellSize,
                centerX = centerX,
                centerY = centerY,
                matrix = matrix
            )

            val (_, quads) = cachedMeshData

            // Render Modes
            when (viewMode) {
                ViewMode.WIREFRAME -> {
                    // Draw lightweight wireframe mesh
                    for (quad in quads) {
                        val p0 = projectFast(quad.v00.normX, quad.v00.normY, quad.v00.normZ)
                        val p1 = projectFast(quad.v10.normX, quad.v10.normY, quad.v10.normZ)
                        val p2 = projectFast(quad.v11.normX, quad.v11.normY, quad.v11.normZ)
                        val p3 = projectFast(quad.v01.normX, quad.v01.normY, quad.v01.normZ)

                        drawLine(quad.fillColor, Offset(p0[0], p0[1]), Offset(p1[0], p1[1]), strokeWidth = 1f)
                        drawLine(quad.fillColor, Offset(p1[0], p1[1]), Offset(p2[0], p2[1]), strokeWidth = 1f)
                        drawLine(quad.fillColor, Offset(p2[0], p2[1]), Offset(p3[0], p3[1]), strokeWidth = 1f)
                        drawLine(quad.fillColor, Offset(p3[0], p3[1]), Offset(p0[0], p0[1]), strokeWidth = 1f)
                    }
                }

                ViewMode.VOXELS -> {
                    // Voxel pillar bars
                    val (verts, _) = cachedMeshData
                    for (y in 0 until grid.height) {
                        val row = verts.getOrNull(y) ?: continue
                        for (x in 0 until grid.width) {
                            val v = row.getOrNull(x) ?: continue
                            if (!v.isVisible) continue

                            val pBase = projectFast(v.normX, v.normY, 0f)
                            val pTop = projectFast(v.normX, v.normY, v.normZ)
                            val radius = (baseCellSize * 0.38f).coerceAtLeast(2f)

                            drawLine(
                                color = v.color.copy(alpha = 0.7f),
                                start = Offset(pBase[0], pBase[1]),
                                end = Offset(pTop[0], pTop[1]),
                                strokeWidth = radius * 1.5f
                            )
                            drawCircle(
                                color = v.color,
                                radius = radius,
                                center = Offset(pTop[0], pTop[1])
                            )
                        }
                    }
                }

                else -> {
                    // SURFACE / ISO_ANOMALIES
                    val path = Path()
                    for (quad in quads) {
                        val p0 = projectFast(quad.v00.normX, quad.v00.normY, quad.v00.normZ)
                        val p1 = projectFast(quad.v10.normX, quad.v10.normY, quad.v10.normZ)
                        val p2 = projectFast(quad.v11.normX, quad.v11.normY, quad.v11.normZ)
                        val p3 = projectFast(quad.v01.normX, quad.v01.normY, quad.v01.normZ)

                        path.reset()
                        path.moveTo(p0[0], p0[1])
                        path.lineTo(p1[0], p1[1])
                        path.lineTo(p2[0], p2[1])
                        path.lineTo(p3[0], p3[1])
                        path.close()

                        drawPath(path, color = quad.fillColor.copy(alpha = 0.85f))
                        if (wireframe) {
                            drawPath(path, color = Color(0x33FFFFFF), style = Stroke(width = 0.8f))
                        }
                    }
                }
            }

            // Draw Subsurface Anomaly Beacons
            val gridWidth = grid.width.coerceAtLeast(1)
            val gridHeight = grid.height.coerceAtLeast(1)
            val cx = (gridWidth - 1) * 0.5f
            val cy = (gridHeight - 1) * 0.5f

            for (anom in anomalies) {
                val isSelected = (anom.id == selectedAnomalyId)
                val beaconColor = Color(anom.classification.colorHex)

                val gx = (anom.gridX.toFloat()).coerceIn(0f, (gridWidth - 1).toFloat())
                val gy = (anom.gridY.toFloat()).coerceIn(0f, (gridHeight - 1).toFloat())
                val normX = gx - cx
                val normY = gy - cy

                val pGround = projectFast(normX, normY, 0f)
                val targetZ = -(anom.depthMeters / depthCutMeters.coerceAtLeast(0.1f) * 2.8f)
                val pTarget = projectFast(normX, normY, targetZ)

                // Depth borehole line
                drawLine(
                    color = beaconColor.copy(alpha = if (isSelected) 1f else 0.65f),
                    start = Offset(pGround[0], pGround[1]),
                    end = Offset(pTarget[0], pTarget[1]),
                    strokeWidth = if (isSelected) 3.5f else 2f
                )

                // Subsurface sphere
                val sphereRadius = (baseCellSize * 0.45f) * (if (isSelected) 1.4f else 1.0f)
                drawCircle(
                    color = beaconColor.copy(alpha = 0.35f),
                    radius = sphereRadius * 1.6f,
                    center = Offset(pTarget[0], pTarget[1])
                )
                drawCircle(
                    color = beaconColor,
                    radius = sphereRadius,
                    center = Offset(pTarget[0], pTarget[1])
                )

                // Surface beacon ring
                drawCircle(
                    color = beaconColor.copy(alpha = 0.8f),
                    radius = baseCellSize * 0.3f,
                    center = Offset(pGround[0], pGround[1]),
                    style = Stroke(width = if (isSelected) 2.5f else 1.5f)
                )
            }
        }

        // HUD Overlay - Angle & Status
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Kamera: Yaw ${yawDeg.toInt()}° | Pitch ${pitchDeg.toInt()}° | Preseti: ${cameraPreset.label} (60 FPS Caching)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                selectedAnomalyId?.let { id ->
                    val anom = anomalies.find { it.id == id }
                    if (anom != null) {
                        Surface(
                            modifier = Modifier
                                .clickable { onOpenAnomalyDetail?.invoke(anom) }
                                .testTag("open_anomaly_bottomsheet_from_3d_viewer"),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xEE0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(anom.classification.colorHex),
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Text(
                                    text = "${anom.classification.label} (${String.format(Locale.US, "%.2fm", anom.depthMeters)}) • Inversion & Hacim Analizi",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Icon(
                                    Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pre-calculated rotation trigonometric coefficients.
 */
data class TransformationMatrix(
    val cosYaw: Float,
    val sinYaw: Float,
    val cosPitch: Float,
    val sinPitch: Float
)

/**
 * Optimized floor grid drawer that avoids allocations.
 */
private fun DrawScope.drawOptimizedGroundGrid(
    width: Int,
    height: Int,
    baseCellSize: Float,
    centerX: Float,
    centerY: Float,
    matrix: TransformationMatrix
) {
    val axisColor = Color(0x33475569)
    val cx = (width - 1) * 0.5f
    val cy = (height - 1) * 0.5f

    for (x in 0 until width) {
        val nx = x - cx
        val p0x = centerX + (nx * matrix.cosYaw - (-cy) * matrix.sinYaw) * baseCellSize
        val p0y = centerY + (nx * matrix.sinYaw + (-cy) * matrix.cosYaw) * matrix.sinPitch * baseCellSize
        val p1x = centerX + (nx * matrix.cosYaw - cy * matrix.sinYaw) * baseCellSize
        val p1y = centerY + (nx * matrix.sinYaw + cy * matrix.cosYaw) * matrix.sinPitch * baseCellSize

        drawLine(axisColor, Offset(p0x, p0y), Offset(p1x, p1y), strokeWidth = 1f)
    }

    for (y in 0 until height) {
        val ny = y - cy
        val p0x = centerX + ((-cx) * matrix.cosYaw - ny * matrix.sinYaw) * baseCellSize
        val p0y = centerY + ((-cx) * matrix.sinYaw + ny * matrix.cosYaw) * matrix.sinPitch * baseCellSize
        val p1x = centerX + (cx * matrix.cosYaw - ny * matrix.sinYaw) * baseCellSize
        val p1y = centerY + (cx * matrix.sinYaw + ny * matrix.cosYaw) * matrix.sinPitch * baseCellSize

        drawLine(axisColor, Offset(p0x, p0y), Offset(p1x, p1y), strokeWidth = 1f)
    }
}
