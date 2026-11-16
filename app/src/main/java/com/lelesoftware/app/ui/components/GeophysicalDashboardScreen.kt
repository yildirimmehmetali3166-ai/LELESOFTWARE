package com.lelesoftware.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.PaletteType
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ScanPoint
import com.lelesoftware.app.model.TargetClassification
import com.lelesoftware.app.ui.theme.AmberPrimary
import com.lelesoftware.app.ui.theme.CyanAccent
import com.lelesoftware.app.ui.theme.DisplayFontFamily
import com.lelesoftware.app.ui.theme.EmeraldSuccess
import com.lelesoftware.app.ui.theme.MonoFontFamily
import com.lelesoftware.app.ui.theme.PlatinumRim
import com.lelesoftware.app.ui.theme.RoseDanger
import com.lelesoftware.app.ui.theme.SkyTelemetry
import com.lelesoftware.app.ui.theme.SlateCardBorder
import com.lelesoftware.app.ui.theme.SlateCardSurface
import com.lelesoftware.app.ui.theme.SlateDarkBackground
import com.lelesoftware.app.ui.theme.SlateElevatedSurface
import com.lelesoftware.app.ui.theme.TextMuted
import com.lelesoftware.app.ui.theme.VoidBlue
import com.lelesoftware.app.util.ColorScales
import com.lelesoftware.app.util.ExportFileFormat
import com.lelesoftware.app.util.GridExportConfig
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Primary Geophysical Dashboard Screen
 *
 * Displays simulated real-time Dual Fluxgate (50 Hz BLE) geophysical scan data
 * using an interactive grid-based Canvas visualization, live ΔnT KPI metrics,
 * real-time waveform strip, cell inspector, and live anomaly detection feed.
 */
@Composable
fun GeophysicalDashboardScreen(
    simulationViewModel: ScanSimulationViewModel = viewModel(),
    onOpenIn3D: (ScanGrid) -> Unit = {},
    onOpenIn2D: (ScanGrid) -> Unit = {},
    onInspectAnomaly: (ScanGrid, DetectedAnomaly) -> Unit = { _, _ -> },
    onSaveAnomaly: (ScanGrid, DetectedAnomaly) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by simulationViewModel.uiState.collectAsState()
    var exportSnapshot by remember {
        mutableStateOf<Triple<ScanGrid, List<DetectedAnomaly>, GridExportConfig>?>(null)
    }
    var exportInitialFormat by remember { mutableStateOf(ExportFileFormat.PDF) }

    fun openExportModal(format: ExportFileFormat) {
        val snapGrid = simulationViewModel.buildCompletedGrid()
        val snapConfig = GridExportConfig(
            palette = uiState.activePalette,
            smoothGrid = uiState.smoothGrid,
            showCellValues = uiState.showCellValues,
            trailCoordinates = uiState.trailCoordinates,
            activeProbeCell = if (uiState.currentStepY < uiState.gridHeight) {
                uiState.currentStepX to uiState.currentStepY
            } else null,
            selectedCell = uiState.selectedCell,
            sectorNumber = uiState.sectorNumber
        )
        exportInitialFormat = format
        exportSnapshot = Triple(snapGrid, uiState.discoveredAnomalies, snapConfig)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "dashboard_pulse")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )
    val reticleExpand by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reticle_expand"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
            .testTag("primary_geophysical_dashboard")
    ) {
        val isExpandedWidth = maxWidth >= 720.dp

        if (isExpandedWidth) {
            // Tablet / Foldable canonical two-pane layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.22f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardLiveStatusHeader(
                        uiState = uiState,
                        beaconAlpha = beaconAlpha,
                        onToggleAudio = { simulationViewModel.toggleAudio() }
                    )
                    LiveGeophysicalGridCard(
                        uiState = uiState,
                        beaconAlpha = beaconAlpha,
                        reticleExpand = reticleExpand,
                        onSelectCell = { gx, gy -> simulationViewModel.selectCell(gx, gy) },
                        onSelectPalette = { simulationViewModel.setPalette(it) },
                        onToggleValues = { simulationViewModel.toggleShowValues() },
                        onToggleSmooth = { simulationViewModel.toggleSmoothGrid() }
                    )
                    DashboardTransportControls(
                        uiState = uiState,
                        onToggleScan = { simulationViewModel.toggleScanning() },
                        onSingleStep = { simulationViewModel.takeSingleStep() },
                        onReset = { simulationViewModel.resetScan() },
                        onToggleSpeed = { simulationViewModel.toggleSpeed() },
                        onToggleAutoLoop = { simulationViewModel.toggleAutoLoop() },
                        onTogglePattern = {
                            val next = if (uiState.walkPattern == "zigzag") "parallel" else "zigzag"
                            simulationViewModel.setWalkPattern(next)
                        },
                        onTransferTo3D = { onOpenIn3D(simulationViewModel.buildCompletedGrid()) },
                        onTransferTo2D = { onOpenIn2D(simulationViewModel.buildCompletedGrid()) },
                        onExportPdf = { openExportModal(ExportFileFormat.PDF) },
                        onExportPng = { openExportModal(ExportFileFormat.PNG) }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardKpiGrid(uiState = uiState)
                    SelectedCellInspectorCard(
                        uiState = uiState,
                        onClearSelection = { simulationViewModel.clearSelectedCell() }
                    )
                    CompactWaveformCard(uiState = uiState, beaconAlpha = beaconAlpha)
                    LiveAnomaliesFeedCard(
                        anomalies = uiState.discoveredAnomalies,
                        onFocusCell = { anom -> simulationViewModel.selectCell(anom.gridX, anom.gridY) },
                        onInspect = { anom ->
                            onInspectAnomaly(simulationViewModel.buildCompletedGrid(), anom)
                        },
                        onSave = { anom ->
                            onSaveAnomaly(simulationViewModel.buildCompletedGrid(), anom)
                        }
                    )
                }
            }
        } else {
            // Compact Phone layout: Header KPIs + Hero Grid Visualization immediately visible
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardLiveStatusHeader(
                    uiState = uiState,
                    beaconAlpha = beaconAlpha,
                    onToggleAudio = { simulationViewModel.toggleAudio() }
                )

                DashboardKpiGrid(uiState = uiState)

                LiveGeophysicalGridCard(
                    uiState = uiState,
                    beaconAlpha = beaconAlpha,
                    reticleExpand = reticleExpand,
                    onSelectCell = { gx, gy -> simulationViewModel.selectCell(gx, gy) },
                    onSelectPalette = { simulationViewModel.setPalette(it) },
                    onToggleValues = { simulationViewModel.toggleShowValues() },
                    onToggleSmooth = { simulationViewModel.toggleSmoothGrid() }
                )

                DashboardTransportControls(
                    uiState = uiState,
                    onToggleScan = { simulationViewModel.toggleScanning() },
                    onSingleStep = { simulationViewModel.takeSingleStep() },
                    onReset = { simulationViewModel.resetScan() },
                    onToggleSpeed = { simulationViewModel.toggleSpeed() },
                    onToggleAutoLoop = { simulationViewModel.toggleAutoLoop() },
                    onTogglePattern = {
                        val next = if (uiState.walkPattern == "zigzag") "parallel" else "zigzag"
                        simulationViewModel.setWalkPattern(next)
                    },
                    onTransferTo3D = { onOpenIn3D(simulationViewModel.buildCompletedGrid()) },
                    onTransferTo2D = { onOpenIn2D(simulationViewModel.buildCompletedGrid()) },
                    onExportPdf = { openExportModal(ExportFileFormat.PDF) },
                    onExportPng = { openExportModal(ExportFileFormat.PNG) }
                )

                SelectedCellInspectorCard(
                    uiState = uiState,
                    onClearSelection = { simulationViewModel.clearSelectedCell() }
                )

                CompactWaveformCard(uiState = uiState, beaconAlpha = beaconAlpha)

                LiveAnomaliesFeedCard(
                    anomalies = uiState.discoveredAnomalies,
                    onFocusCell = { anom -> simulationViewModel.selectCell(anom.gridX, anom.gridY) },
                    onInspect = { anom ->
                        onInspectAnomaly(simulationViewModel.buildCompletedGrid(), anom)
                    },
                    onSave = { anom ->
                        onSaveAnomaly(simulationViewModel.buildCompletedGrid(), anom)
                    }
                )
            }
        }
    }

    exportSnapshot?.let { (snapGrid, snapAnoms, snapConfig) ->
        ScanExportDialog(
            isOpen = true,
            initialFormat = exportInitialFormat,
            grid = snapGrid,
            anomalies = snapAnoms,
            config = snapConfig,
            onDismiss = { exportSnapshot = null }
        )
    }
}

@Composable
private fun DashboardLiveStatusHeader(
    uiState: ScanSimulationUiState,
    beaconAlpha: Float,
    onToggleAudio: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_live_status_header"),
        color = SlateCardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (uiState.isScanning) EmeraldSuccess.copy(alpha = beaconAlpha)
                        else AmberPrimary
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (uiState.isScanning) "CANLI JEOFİZİK TARAMA SİMÜLASYONU" else "TARAMA DURAKLATILDI",
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.6.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "Sektör #${uiState.sectorNumber} • BLE 5.2 (50 Hz) • ${uiState.gridWidth}×${uiState.gridHeight} Izgara",
                    color = SkyTelemetry,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SlateElevatedSurface,
                border = BorderStroke(1.dp, PlatinumRim.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "%${uiState.deviceStatus.signalQuality}",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }

            IconButton(
                onClick = onToggleAudio,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("dashboard_audio_toggle_button")
            ) {
                Icon(
                    imageVector = if (uiState.isAudioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (uiState.isAudioEnabled) "Sesi Kapat" else "Sesi Aç",
                    tint = if (uiState.isAudioEnabled) AmberPrimary else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardKpiGrid(uiState: ScanSimulationUiState) {
    val totalCells = (uiState.gridWidth * uiState.gridHeight).coerceAtLeast(1)
    val progressRatio = (uiState.visitedCount.toFloat() / totalCells.toFloat()).coerceIn(0f, 1f)
    val deltaNt = uiState.deviceStatus.gradientDelta
    val deltaColor by animateColorAsState(
        targetValue = when {
            deltaNt > 400 -> AmberPrimary
            deltaNt < -350 -> VoidBlue
            else -> CyanAccent
        },
        label = "kpi_delta_color"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardKpiTile(
                title = "Gradyan (ΔnT)",
                value = String.format(Locale.US, "%+d nT", deltaNt),
                subtitle = if (uiState.dspOutput?.isAnomaly == true) "Anomali Sinyali" else "Zemin Dengeli",
                accentColor = deltaColor,
                modifier = Modifier.weight(1f)
            )
            DashboardKpiTile(
                title = "Hedef Derinlik",
                value = String.format(Locale.US, "%.2f m", uiState.estimatedTargetDepthMeters),
                subtitle = "FWHM / Peters",
                accentColor = AmberPrimary,
                modifier = Modifier.weight(1f)
            )
            DashboardKpiTile(
                title = "Tespit / İlerleme",
                value = "${uiState.discoveredAnomalies.size} Hedef",
                subtitle = "%${(progressRatio * 100).roundToInt()} (${uiState.visitedCount}/$totalCells)",
                accentColor = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DashboardKpiTile(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SlateCardSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
            Text(
                text = value,
                color = accentColor,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = Color(0xFFCBD5E1),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LiveGeophysicalGridCard(
    uiState: ScanSimulationUiState,
    beaconAlpha: Float,
    reticleExpand: Float,
    onSelectCell: (Int, Int) -> Unit,
    onSelectPalette: (PaletteType) -> Unit,
    onToggleValues: () -> Unit,
    onToggleSmooth: () -> Unit
) {
    val gridW = uiState.gridWidth.coerceAtLeast(1)
    val gridH = uiState.gridHeight.coerceAtLeast(1)
    val totalCells = (gridW * gridH).coerceAtLeast(1)
    val progress = (uiState.visitedCount.toFloat() / totalCells.toFloat()).coerceIn(0f, 1f)
    val zoomState = rememberGridZoomPanState(gridW, gridH)
    val visibleBounds = zoomState.visibleNormalizedBounds()
    val totalWidthMeters = gridW * uiState.stepDistanceMeters
    val totalHeightMeters = gridH * uiState.stepDistanceMeters

    val valuePaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 22f
            color = android.graphics.Color.WHITE
            setShadowLayer(2.5f, 0f, 0f, android.graphics.Color.BLACK)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_grid_visualization_card"),
        colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Header + Quick Display Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Gerçek Zamanlı Izgara Haritası",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", gridW * uiState.stepDistanceMeters)}m × ${String.format(Locale.US, "%.1f", gridH * uiState.stepDistanceMeters)}m • ${if (uiState.walkPattern == "zigzag") "Zikzak" else "Paralel"} Tarama",
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = uiState.showCellValues,
                        onClick = onToggleValues,
                        label = { Text("Değer", fontSize = 11.sp, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary.copy(alpha = 0.22f),
                            selectedLabelColor = AmberPrimary
                        ),
                        modifier = Modifier.testTag("grid_toggle_values_chip")
                    )
                    FilterChip(
                        selected = uiState.smoothGrid,
                        onClick = onToggleSmooth,
                        label = { Text("Yumuşat", fontSize = 11.sp, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
                            selectedLabelColor = CyanAccent
                        ),
                        modifier = Modifier.testTag("grid_toggle_smooth_chip")
                    )
                }
            }

            // Palette Selector Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PaletteType.entries.forEach { palette ->
                    val shortName = when (palette) {
                        PaletteType.LELE_SPECTRUM -> "LELE Spektrum"
                        PaletteType.THERMAL_JET -> "Termal Jet"
                        PaletteType.MAGMA -> "Magma"
                        PaletteType.EMERALD_VOID -> "Zümrüt Boşluk"
                        PaletteType.GOLD_RUSH -> "Altın Odaklı"
                    }
                    FilterChip(
                        selected = uiState.activePalette == palette,
                        onClick = { onSelectPalette(palette) },
                        label = {
                            Text(
                                text = shortName,
                                fontSize = 11.sp,
                                fontWeight = if (uiState.activePalette == palette) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = SlateDarkBackground
                        )
                    )
                }
            }

            // Pinch-to-Zoom & Pan Controls Strip
            GridZoomToolbar(
                state = zoomState,
                tagPrefix = "dashboard_grid"
            )

            // Axis-Aligned Grid Visualization Canvas
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val xMinM = visibleBounds.uLeft * totalWidthMeters
                val xMaxM = visibleBounds.uRight * totalWidthMeters
                val xMidM = (xMinM + xMaxM) * 0.5f

                // Top X-axis meter labels (dynamically reflect zoomed viewport bounds)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(String.format(Locale.US, "%.1fm", xMinM), color = TextMuted, fontSize = 9.sp, fontFamily = MonoFontFamily)
                    Text(
                        String.format(Locale.US, "%.1fm", xMidM),
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = MonoFontFamily
                    )
                    Text(
                        String.format(Locale.US, "%.1fm", xMaxM),
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = MonoFontFamily
                    )
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val yAxisWidth = 28.dp
                    val gap = 6.dp
                    val canvasWidth = (maxWidth - yAxisWidth - gap).coerceAtLeast(120.dp)
                    val canvasHeight = canvasWidth * (gridH.toFloat() / gridW.toFloat())
                    val yMinM = visibleBounds.vTop * totalHeightMeters
                    val yMaxM = visibleBounds.vBottom * totalHeightMeters
                    val yMidM = (yMinM + yMaxM) * 0.5f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        // Left Y-axis meter labels aligned to visible zoomed viewport
                        Column(
                            modifier = Modifier
                                .width(yAxisWidth)
                                .height(canvasHeight),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(String.format(Locale.US, "%.1fm", yMinM), color = TextMuted, fontSize = 9.sp, fontFamily = MonoFontFamily)
                            Text(
                                String.format(Locale.US, "%.1fm", yMidM),
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = MonoFontFamily
                            )
                            Text(
                                String.format(Locale.US, "%.1fm", yMaxM),
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = MonoFontFamily
                            )
                        }

                        // Interactive Geophysical Grid Matrix (Pinch-to-Zoom, Pan, Tap-to-Inspect)
                        Box(
                            modifier = Modifier
                                .width(canvasWidth)
                                .height(canvasHeight)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF040814))
                                .border(1.5.dp, if (zoomState.isZoomed) CyanAccent.copy(alpha = 0.7f) else SlateCardBorder, RoundedCornerShape(12.dp))
                                .geophysicalGridGestures(
                                    state = zoomState,
                                    key1 = gridW to gridH
                                ) { tapOffset, viewportSize ->
                                    val (gx, gy) = zoomState.viewportToGridCell(
                                        tapPos = tapOffset,
                                        size = viewportSize,
                                        gridWidth = gridW,
                                        gridHeight = gridH
                                    )
                                    onSelectCell(gx, gy)
                                }
                                .testTag("geophysical_grid_canvas_box")
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
                            val cellW = renderW / gridW.toFloat()
                            val cellH = renderH / gridH.toFloat()
                            val invScale = (1f / zoomState.scale.coerceAtLeast(1f))

                            // 1. Render cells (Smooth interpolated or crisp matrix blocks)
                            if (uiState.smoothGrid) {
                                val subX = gridW * 2
                                val subY = gridH * 2
                                val subW = renderW / subX.toFloat()
                                val subH = renderH / subY.toFloat()

                                for (sy in 0 until subY) {
                                    val gy = (sy.toFloat() / subY) * (gridH - 1)
                                    val y0 = gy.toInt().coerceIn(0, gridH - 1)
                                    val y1 = (y0 + 1).coerceIn(0, gridH - 1)
                                    val wy = gy - y0

                                    for (sx in 0 until subX) {
                                        val gx = (sx.toFloat() / subX) * (gridW - 1)
                                        val x0 = gx.toInt().coerceIn(0, gridW - 1)
                                        val x1 = (x0 + 1).coerceIn(0, gridW - 1)
                                        val wx = gx - x0

                                        val p00 = uiState.points.getOrNull(y0)?.getOrNull(x0)
                                        val p10 = uiState.points.getOrNull(y0)?.getOrNull(x1)
                                        val p01 = uiState.points.getOrNull(y1)?.getOrNull(x0)
                                        val p11 = uiState.points.getOrNull(y1)?.getOrNull(x1)

                                        val anyVisited = (p00?.timestamp ?: 0L) > 0L || (p10?.timestamp ?: 0L) > 0L
                                        val col = if (anyVisited) {
                                            val s00 = p00?.filteredSignal ?: 0f
                                            val s10 = p10?.filteredSignal ?: s00
                                            val s01 = p01?.filteredSignal ?: s00
                                            val s11 = p11?.filteredSignal ?: s01
                                            val interpolated = (s00 * (1f - wx) + s10 * wx) * (1f - wy) +
                                                (s01 * (1f - wx) + s11 * wx) * wy
                                            ColorScales.getPaletteColor(interpolated, uiState.activePalette)
                                        } else {
                                            Color(0xFF070E1D)
                                        }

                                        drawRect(
                                            color = col,
                                            topLeft = Offset(sx * subW, sy * subH),
                                            size = Size(subW + 0.6f, subH + 0.6f)
                                        )
                                    }
                                }
                            } else {
                                for (y in 0 until gridH) {
                                    val row = uiState.points.getOrNull(y) ?: continue
                                    for (x in 0 until gridW) {
                                        val pt = row.getOrNull(x) ?: continue
                                        val isVisited = pt.timestamp > 0L
                                        val topLeft = Offset(x * cellW, y * cellH)

                                        if (isVisited) {
                                            val cellColor = ColorScales.getPaletteColor(
                                                pt.filteredSignal,
                                                uiState.activePalette
                                            )
                                            drawRect(
                                                color = cellColor,
                                                topLeft = topLeft,
                                                size = Size(cellW, cellH)
                                            )
                                            drawRect(
                                                color = Color(0x28000000),
                                                topLeft = topLeft,
                                                size = Size(cellW, cellH),
                                                style = Stroke(width = 1f)
                                            )
                                        } else {
                                            drawRect(
                                                color = Color(0xFF070E1D),
                                                topLeft = topLeft,
                                                size = Size(cellW, cellH)
                                            )
                                            drawRect(
                                                color = Color(0xFF132038),
                                                topLeft = topLeft,
                                                size = Size(cellW, cellH),
                                                style = Stroke(width = 0.8f)
                                            )
                                            drawCircle(
                                                color = Color(0xFF1F304D),
                                                radius = 1.6f,
                                                center = Offset(
                                                    topLeft.x + cellW * 0.5f,
                                                    topLeft.y + cellH * 0.5f
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Subtle grid overlay in smooth mode + optional cell values
                            if (uiState.smoothGrid) {
                                for (x in 1 until gridW) {
                                    drawLine(
                                        color = Color(0x22FFFFFF),
                                        start = Offset(x * cellW, 0f),
                                        end = Offset(x * cellW, renderH),
                                        strokeWidth = 0.7f
                                    )
                                }
                                for (y in 1 until gridH) {
                                    drawLine(
                                        color = Color(0x22FFFFFF),
                                        start = Offset(0f, y * cellH),
                                        end = Offset(renderW, y * cellH),
                                        strokeWidth = 0.7f
                                    )
                                }
                            }

                            if (uiState.showCellValues || zoomState.scale >= 1.85f) {
                                valuePaint.textSize = (minOf(cellW, cellH) * 0.32f).coerceIn(9f, 22f)
                                val yOffset = valuePaint.textSize * 0.35f
                                drawIntoCanvas { canvas ->
                                    for (y in 0 until gridH) {
                                        val row = uiState.points.getOrNull(y) ?: continue
                                        for (x in 0 until gridW) {
                                            val pt = row.getOrNull(x) ?: continue
                                            if (pt.timestamp > 0L) {
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
                            }

                            // 3. Survey Traverse Trail (last N cells)
                            if (uiState.trailCoordinates.size >= 2) {
                                val trailPath = Path()
                                uiState.trailCoordinates.forEachIndexed { idx, (tx, ty) ->
                                    val px = tx * cellW + cellW * 0.5f
                                    val py = ty * cellH + cellH * 0.5f
                                    if (idx == 0) trailPath.moveTo(px, py) else trailPath.lineTo(px, py)
                                }
                                drawPath(
                                    path = trailPath,
                                    color = Color.White.copy(alpha = 0.42f),
                                    style = Stroke(width = (2.0f * invScale).coerceAtLeast(0.8f), cap = StrokeCap.Round)
                                )
                            }

                            // 4. Discovered Anomaly Halos
                            val anomalyRadius = (minOf(cellW, cellH) * 0.46f).coerceAtLeast(7f)
                            for (anom in uiState.discoveredAnomalies) {
                                val cx = anom.gridX * cellW + cellW * 0.5f
                                val cy = anom.gridY * cellH + cellH * 0.5f
                                val anomColor = Color(anom.classification.colorHex)
                                drawCircle(
                                    color = anomColor.copy(alpha = 0.25f * beaconAlpha),
                                    radius = anomalyRadius * 1.25f,
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = anomColor,
                                    radius = anomalyRadius * 0.8f,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = (2.2f * invScale).coerceAtLeast(1f))
                                )
                            }

                            // 5. Active Live Probe Beacon
                            if (uiState.currentStepY < gridH && uiState.currentStepX < gridW) {
                                val probeCenter = Offset(
                                    uiState.currentStepX * cellW + cellW * 0.5f,
                                    uiState.currentStepY * cellH + cellH * 0.5f
                                )
                                val baseR = minOf(cellW, cellH) * 0.42f
                                drawCircle(
                                    color = CyanAccent.copy(alpha = 0.42f * beaconAlpha),
                                    radius = baseR * reticleExpand,
                                    center = probeCenter
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = baseR * 0.42f,
                                    center = probeCenter
                                )
                                drawCircle(
                                    color = AmberPrimary,
                                    radius = baseR * 0.78f,
                                    center = probeCenter,
                                    style = Stroke(width = (2.2f * invScale).coerceAtLeast(1f))
                                )
                            }

                            // 6. Selected Inspection Cell Reticle
                            uiState.selectedCell?.let { (sx, sy) ->
                                val safeX = sx.coerceIn(0, gridW - 1)
                                val safeY = sy.coerceIn(0, gridH - 1)
                                val topLeft = Offset(safeX * cellW, safeY * cellH)
                                drawRect(
                                    color = Color.White,
                                    topLeft = topLeft,
                                    size = Size(cellW, cellH),
                                    style = Stroke(width = (3f * invScale).coerceAtLeast(1.2f))
                                )
                                drawCircle(
                                    color = CyanAccent,
                                    radius = (4.5f * invScale).coerceAtLeast(2f),
                                    center = Offset(topLeft.x + cellW * 0.5f, topLeft.y + cellH * 0.5f)
                                )
                            }
                        }
                        }
                    }
                }
            }

            // Progress Bar + Spectrum Color Scale Legend
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = AmberPrimary,
                    trackColor = SlateElevatedSurface
                )

                val legendColors = remember(uiState.activePalette) {
                    listOf(-100f, -50f, 0f, 50f, 100f).map {
                        ColorScales.getPaletteColor(it, uiState.activePalette)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(legendColors))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-100 Boşluk", color = VoidBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("0 Homojen Zemin", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("+100 Değerli Metal", color = AmberPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DashboardTransportControls(
    uiState: ScanSimulationUiState,
    onToggleScan: () -> Unit,
    onSingleStep: () -> Unit,
    onReset: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleAutoLoop: () -> Unit,
    onTogglePattern: () -> Unit,
    onTransferTo3D: () -> Unit,
    onTransferTo2D: () -> Unit,
    onExportPdf: () -> Unit,
    onExportPng: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SlateCardSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Primary Transport Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleScan,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("dashboard_play_pause_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isScanning) RoseDanger else EmeraldSuccess,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isScanning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isScanning) "Duraklat" else "Taramayı Başlat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onSingleStep,
                    enabled = !uiState.isScanning && uiState.currentStepY < uiState.gridHeight,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("dashboard_step_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+1 Adım", color = Color.White, fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("dashboard_reset_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Yeni Sektör", tint = AmberPrimary, modifier = Modifier.size(18.dp))
                }
            }

            // Secondary Config & View Transfer Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = true,
                    onClick = onToggleSpeed,
                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = {
                        val speedStr = if (uiState.scanSpeedMultiplier == 2.0f) "2x Hızlı" else if (uiState.scanSpeedMultiplier == 0.5f) "0.5x Hassas" else "1x Normal"
                        Text(speedStr, fontSize = 11.sp, maxLines = 1)
                    }
                )

                FilterChip(
                    selected = uiState.autoLoop,
                    onClick = onToggleAutoLoop,
                    leadingIcon = { Icon(Icons.Default.Loop, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text("Oto Döngü", fontSize = 11.sp, maxLines = 1) }
                )

                FilterChip(
                    selected = uiState.walkPattern == "zigzag",
                    onClick = onTogglePattern,
                    label = {
                        Text(
                            if (uiState.walkPattern == "zigzag") "Desen: Zikzak" else "Desen: Paralel",
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                )
            }

            // Export / Transfer to 3D & 2D
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTransferTo3D,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_open_3d_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPrimary,
                        contentColor = SlateDarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("3D Sahnede Aç", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onTransferTo2D,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_open_2d_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("2D Kesitte Aç", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }

            // Export Current Grid Visualization as PDF or PNG Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_export_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.75f))
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF Rapor", color = AmberPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onExportPng,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_export_png_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.75f))
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PNG Görsel", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SelectedCellInspectorCard(
    uiState: ScanSimulationUiState,
    onClearSelection: () -> Unit
) {
    val activeCoords = uiState.selectedCell ?: run {
        val last = uiState.trailCoordinates.lastOrNull()
        last ?: (uiState.currentStepX.coerceIn(0, uiState.gridWidth - 1) to uiState.currentStepY.coerceIn(0, uiState.gridHeight - 1))
    }
    val point: ScanPoint? = uiState.points.getOrNull(activeCoords.second)?.getOrNull(activeCoords.first)
    val isUserSelected = uiState.selectedCell != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_cell_inspector_card"),
        color = SlateCardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isUserSelected) CyanAccent.copy(alpha = 0.6f) else SlateCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUserSelected) "SEÇİLİ HÜCRE DENETİMİ (${activeCoords.first}, ${activeCoords.second})"
                        else "SON ÖLÇÜLEN PROB HÜCRESİ (${activeCoords.first}, ${activeCoords.second})",
                        color = if (isUserSelected) CyanAccent else PlatinumRim,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    val xM = activeCoords.first * uiState.stepDistanceMeters
                    val yM = activeCoords.second * uiState.stepDistanceMeters
                    Text(
                        text = String.format(Locale.US, "Saha Koordinatı: X=%.1fm • Y=%.1fm", xM, yM),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                if (isUserSelected) {
                    Surface(
                        modifier = Modifier
                            .clickable { onClearSelection() }
                            .testTag("clear_cell_selection_chip"),
                        shape = RoundedCornerShape(8.dp),
                        color = SlateElevatedSurface,
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Canlı Takibe Dön",
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (point == null || point.timestamp <= 0L) {
                Text(
                    text = "Bu hücre henüz taranmadı. Prob bu koordinata ulaştığında ham manyetik nT ve derinlik değerleri görüntülenecektir.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            } else {
                val classColor = Color(point.classification.colorHex)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InspectorMiniBox(
                        label = "Sinyal (%)",
                        value = String.format(Locale.US, "%+.1f%%", point.filteredSignal),
                        valueColor = classColor,
                        modifier = Modifier.weight(1f)
                    )
                    InspectorMiniBox(
                        label = "Ham Alan",
                        value = "${point.rawSignal} nT",
                        valueColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    InspectorMiniBox(
                        label = "Tahmini Derinlik",
                        value = if (point.depth > 0f) String.format(Locale.US, "%.2f m", point.depth) else "Yüzey",
                        valueColor = AmberPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(classColor)
                    )
                    Text(
                        text = point.classification.label,
                        color = classColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Güven: %${point.confidence}",
                        color = Color(0xFFCBD5E1),
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectorMiniBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SlateElevatedSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = TextMuted, fontSize = 9.sp, maxLines = 1)
        Text(
            text = value,
            color = valueColor,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun CompactWaveformCard(
    uiState: ScanSimulationUiState,
    beaconAlpha: Float
) {
    val samples = uiState.signalSamples
    val currentDelta = samples.lastOrNull()?.gradientDelta ?: uiState.deviceStatus.gradientDelta.toFloat()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_waveform_card"),
        color = SlateCardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Canlı Gradyan Osiloskopu (ΔnT)",
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format(Locale.US, "%+.1f nT", currentDelta),
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (currentDelta > 0f) AmberPrimary else CyanAccent
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF040814))
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                    val w = size.width
                    val h = size.height
                    val zeroY = h / 2f

                    drawLine(
                        color = SkyTelemetry.copy(alpha = 0.35f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.2f
                    )

                    if (samples.size >= 2) {
                        val display = samples.takeLast(70)
                        val maxAbs = (display.maxOfOrNull { abs(it.gradientDelta) } ?: 30f).coerceAtLeast(30f) * 1.15f
                        val stepX = w / (display.size - 1).coerceAtLeast(1)
                        val wavePath = Path()

                        display.forEachIndexed { idx, s ->
                            val px = idx * stepX
                            val norm = (s.gradientDelta / maxAbs).coerceIn(-1f, 1f)
                            val py = zeroY - norm * (zeroY * 0.88f)
                            if (idx == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
                        }

                        drawPath(
                            path = wavePath,
                            brush = Brush.horizontalGradient(listOf(CyanAccent, AmberPrimary, PlatinumRim)),
                            style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                        )

                        val lastNorm = (display.last().gradientDelta / maxAbs).coerceIn(-1f, 1f)
                        val lastY = zeroY - lastNorm * (zeroY * 0.88f)
                        drawCircle(
                            color = AmberPrimary.copy(alpha = 0.45f * beaconAlpha),
                            radius = 9f,
                            center = Offset(w, lastY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.5f,
                            center = Offset(w, lastY)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveAnomaliesFeedCard(
    anomalies: List<DetectedAnomaly>,
    onFocusCell: (DetectedAnomaly) -> Unit,
    onInspect: (DetectedAnomaly) -> Unit,
    onSave: (DetectedAnomaly) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_anomalies_feed_card"),
        color = SlateCardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anlık Tespit Edilen Anomaliler",
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SlateElevatedSurface
                ) {
                    Text(
                        text = "${anomalies.size} Hedef",
                        color = AmberPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (anomalies.isEmpty()) {
                Text(
                    text = "Prob sahada ilerledikçe tespit edilen metal ve boşluk anomalileri burada listelenecektir.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            } else {
                for (anom in anomalies) {
                    val accent = Color(anom.classification.colorHex)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFocusCell(anom) }
                            .testTag("dashboard_anomaly_item_${anom.gridX}_${anom.gridY}"),
                        color = SlateElevatedSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anom.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "(${String.format(Locale.US, "%.1f", anom.xMeters)}m, ${String.format(Locale.US, "%.1f", anom.yMeters)}m) • Derinlik ~${String.format(Locale.US, "%.2f", anom.depthMeters)}m • %${anom.signalStrength}",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { onSave(anom) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "Anomaliyi Kaydet",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Surface(
                                modifier = Modifier.clickable { onInspect(anom) },
                                shape = RoundedCornerShape(6.dp),
                                color = AmberPrimary.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "Analiz",
                                    color = AmberPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
