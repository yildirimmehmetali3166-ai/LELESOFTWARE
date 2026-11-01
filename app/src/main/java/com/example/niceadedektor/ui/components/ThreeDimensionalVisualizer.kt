package com.example.niceadedektor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.niceadedektor.model.CameraPreset
import com.example.niceadedektor.model.DetectedAnomaly
import com.example.niceadedektor.model.PaletteType
import com.example.niceadedektor.model.ScanGrid
import com.example.niceadedektor.model.ViewMode
import com.example.niceadedektor.util.ColorScales
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class Point3D(val x: Float, val y: Float, val z: Float)
private data class ProjectedPoint(val screenX: Float, val screenY: Float, val depthOrder: Float)

@Composable
fun ThreeDimensionalVisualizer(
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
    modifier: Modifier = Modifier
) {
    // 3D Camera Angles & Distance
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040814))
            .testTag("three_dimensional_canvas")
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
            val centerX = canvasW / 2f
            val centerY = canvasH / 2f

            val yawRad = (yawDeg * PI / 180f).toFloat()
            val pitchRad = (pitchDeg * PI / 180f).toFloat()

            val cosYaw = cos(yawRad)
            val sinYaw = sin(yawRad)
            val cosPitch = cos(pitchRad)
            val sinPitch = sin(pitchRad)

            val baseCellSize = (canvasW.coerceAtMost(canvasH) / (grid.width.coerceAtLeast(grid.height) * 1.55f)) * scaleFactor

            fun project(p: Point3D): ProjectedPoint {
                // Centered coordinates
                val cx = p.x - (grid.width - 1) * 0.5f
                val cy = p.y - (grid.height - 1) * 0.5f
                val cz = p.z

                // Yaw rotation around Z axis
                val x1 = cx * cosYaw - cy * sinYaw
                val y1 = cx * sinYaw + cy * cosYaw
                val z1 = cz

                // Pitch rotation around X axis
                val y2 = y1 * cosPitch - z1 * sinPitch
                val z2 = y1 * sinPitch + z1 * cosPitch

                // Perspective projection
                val distance = 25f
                val fov = distance / (distance + y2)

                val screenX = centerX + x1 * baseCellSize * fov
                val screenY = centerY - z2 * baseCellSize * fov

                return ProjectedPoint(screenX, screenY, y2)
            }

            // Draw Subsurface Grid / Terrain
            val width = grid.width
            val height = grid.height

            // Calculate 3D points for each cell
            val pointMatrix = Array(height) { y ->
                Array(width) { x ->
                    val pt = grid.points[y][x]
                    val filtered = pt.filteredSignal

                    // Elevation Z: Positive pushes up, negative dips into cavity
                    val zElevation = when {
                        abs(filtered) < thresholdFilter -> 0f
                        filtered < 0f -> {
                            // Negative anomaly: cavity/void penetrates down
                            val depthRatio = (pt.depth / depthCutMeters).coerceIn(0f, 1f)
                            -(depthRatio * 2.8f)
                        }
                        else -> {
                            // Positive metal dipole pushes upward
                            (filtered / 100f) * 2.2f
                        }
                    }

                    Point3D(x.toFloat(), y.toFloat(), zElevation)
                }
            }

            val projectedMatrix = Array(height) { y ->
                Array(width) { x ->
                    project(pointMatrix[y][x])
                }
            }

            // Draw bounding subterranean cage
            drawGroundGrid(
                width = width,
                height = height,
                project = ::project
            )

            // Render Modes
            when (viewMode) {
                ViewMode.SURFACE -> {
                    // Draw filled surface quads with depth sorting
                    data class Quad(
                        val x: Int,
                        val y: Int,
                        val p0: ProjectedPoint,
                        val p1: ProjectedPoint,
                        val p2: ProjectedPoint,
                        val p3: ProjectedPoint,
                        val depth: Float,
                        val color: Color
                    )

                    val quads = mutableListOf<Quad>()
                    for (y in 0 until height - 1) {
                        for (x in 0 until width - 1) {
                            val p0 = projectedMatrix[y][x]
                            val p1 = projectedMatrix[y][x + 1]
                            val p2 = projectedMatrix[y + 1][x + 1]
                            val p3 = projectedMatrix[y + 1][x]

                            val avgDepth = (p0.depthOrder + p1.depthOrder + p2.depthOrder + p3.depthOrder) / 4f
                            val avgSignal = (
                                grid.points[y][x].filteredSignal +
                                grid.points[y][x + 1].filteredSignal +
                                grid.points[y + 1][x + 1].filteredSignal +
                                grid.points[y + 1][x].filteredSignal
                            ) / 4f

                            val color = ColorScales.getPaletteColor(avgSignal, palette)
                            quads.add(Quad(x, y, p0, p1, p2, p3, avgDepth, color))
                        }
                    }

                    // Sort back to front
                    quads.sortByDescending { it.depth }

                    for (q in quads) {
                        val path = Path().apply {
                            moveTo(q.p0.screenX, q.p0.screenY)
                            lineTo(q.p1.screenX, q.p1.screenY)
                            lineTo(q.p2.screenX, q.p2.screenY)
                            lineTo(q.p3.screenX, q.p3.screenY)
                            close()
                        }
                        drawPath(path, color = q.color.copy(alpha = 0.88f))
                        if (wireframe) {
                            drawPath(path, color = Color(0x33FFFFFF), style = Stroke(width = 1f))
                        }
                    }
                }

                ViewMode.VOXELS -> {
                    // Render columnar 3D voxel prisms
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val pt = grid.points[y][x]
                            if (abs(pt.filteredSignal) < thresholdFilter) continue

                            val basePt = project(Point3D(x.toFloat(), y.toFloat(), 0f))
                            val topPt = projectedMatrix[y][x]
                            val col = ColorScales.getPaletteColor(pt.filteredSignal, palette)

                            drawLine(
                                color = col.copy(alpha = 0.9f),
                                start = Offset(basePt.screenX, basePt.screenY),
                                end = Offset(topPt.screenX, topPt.screenY),
                                strokeWidth = baseCellSize * 0.45f
                            )
                            drawCircle(
                                color = col,
                                radius = baseCellSize * 0.28f,
                                center = Offset(topPt.screenX, topPt.screenY)
                            )
                        }
                    }
                }

                ViewMode.WIREFRAME -> {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val p = projectedMatrix[y][x]
                            val col = ColorScales.getPaletteColor(grid.points[y][x].filteredSignal, palette)

                            if (x < width - 1) {
                                val px = projectedMatrix[y][x + 1]
                                drawLine(col.copy(alpha = 0.75f), Offset(p.screenX, p.screenY), Offset(px.screenX, px.screenY), strokeWidth = 1.6f)
                            }
                            if (y < height - 1) {
                                val py = projectedMatrix[y + 1][x]
                                drawLine(col.copy(alpha = 0.75f), Offset(p.screenX, p.screenY), Offset(py.screenX, py.screenY), strokeWidth = 1.6f)
                            }
                        }
                    }
                }

                ViewMode.ISO_ANOMALIES -> {
                    // Transparent ghost ground grid + glowing anomaly spheres
                    for (y in 0 until height) {
                        for (x in 0 until width - 1) {
                            val p = projectedMatrix[y][x]
                            val px = projectedMatrix[y][x + 1]
                            drawLine(Color(0x2264748B), Offset(p.screenX, p.screenY), Offset(px.screenX, px.screenY), strokeWidth = 1f)
                        }
                    }
                }
            }

            // Draw Anomaly Beacons & Markers
            for (anom in anomalies) {
                val gx = anom.gridX.toFloat()
                val gy = anom.gridY.toFloat()
                val isSelected = anom.id == selectedAnomalyId
                val beaconColor = Color(anom.classification.colorHex)

                val groundPos = project(Point3D(gx, gy, 0f))
                val targetZ = -(anom.depthMeters / depthCutMeters * 2.8f)
                val targetPos = project(Point3D(gx, gy, targetZ))

                // Depth borehole line
                drawLine(
                    color = beaconColor.copy(alpha = if (isSelected) 1f else 0.65f),
                    start = Offset(groundPos.screenX, groundPos.screenY),
                    end = Offset(targetPos.screenX, targetPos.screenY),
                    strokeWidth = if (isSelected) 3.5f else 2f
                )

                // Subsurface Anomaly Sphere
                val sphereRadius = (baseCellSize * 0.45f) * (if (isSelected) 1.4f else 1.0f)
                drawCircle(
                    color = beaconColor.copy(alpha = 0.35f),
                    radius = sphereRadius * 1.6f,
                    center = Offset(targetPos.screenX, targetPos.screenY)
                )
                drawCircle(
                    color = beaconColor,
                    radius = sphereRadius,
                    center = Offset(targetPos.screenX, targetPos.screenY)
                )

                // Surface ring
                drawCircle(
                    color = beaconColor.copy(alpha = 0.8f),
                    radius = baseCellSize * 0.3f,
                    center = Offset(groundPos.screenX, groundPos.screenY),
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
            Text(
                text = "Kamera: Yaw ${yawDeg.toInt()}° | Pitch ${pitchDeg.toInt()}° | Preseti: ${cameraPreset.label}",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}

private fun DrawScope.drawGroundGrid(
    width: Int,
    height: Int,
    project: (Point3D) -> ProjectedPoint
) {
    val axisColor = Color(0x33475569)

    // Baseline floor lines (Z = 0)
    for (x in 0 until width) {
        val pStart = project(Point3D(x.toFloat(), 0f, 0f))
        val pEnd = project(Point3D(x.toFloat(), (height - 1).toFloat(), 0f))
        drawLine(axisColor, Offset(pStart.screenX, pStart.screenY), Offset(pEnd.screenX, pEnd.screenY), strokeWidth = 1f)
    }

    for (y in 0 until height) {
        val pStart = project(Point3D(0f, y.toFloat(), 0f))
        val pEnd = project(Point3D((width - 1).toFloat(), y.toFloat(), 0f))
        drawLine(axisColor, Offset(pStart.screenX, pStart.screenY), Offset(pEnd.screenX, pEnd.screenY), strokeWidth = 1f)
    }
}
