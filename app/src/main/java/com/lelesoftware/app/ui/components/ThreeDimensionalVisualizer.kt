package com.lelesoftware.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lelesoftware.app.model.CameraPreset
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.PaletteType
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ViewMode

/**
 * ThreeDimensionalVisualizer
 *
 * Yüksek performanslı ThreeDScanViewer çekirdeğini kullanan 3D görüntüleme bileşeni.
 * Canvas içi matris dönüştürme ve izometrik hesaplamaları önbellekleyerek 60 FPS akıcılık sağlar.
 */
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
    onOpenAnomalyDetail: ((DetectedAnomaly) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ThreeDScanViewer(
        grid = grid,
        palette = palette,
        viewMode = viewMode,
        depthCutMeters = depthCutMeters,
        thresholdFilter = thresholdFilter,
        wireframe = wireframe,
        selectedAnomalyId = selectedAnomalyId,
        onSelectAnomaly = onSelectAnomaly,
        anomalies = anomalies,
        cameraPreset = cameraPreset,
        onOpenAnomalyDetail = onOpenAnomalyDetail,
        modifier = modifier
    )
}
