package com.example.niceadedektor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.niceadedektor.model.CameraPreset
import com.example.niceadedektor.model.DetectedAnomaly
import com.example.niceadedektor.model.PaletteType
import com.example.niceadedektor.model.ScanGrid
import com.example.niceadedektor.model.ViewMode
import com.example.niceadedektor.ui.components.AnalysisDialog
import com.example.niceadedektor.ui.components.BottomControlsBar
import com.example.niceadedektor.ui.components.DevelopmentPlanScreen
import com.example.niceadedektor.ui.components.LiveScannerScreen
import com.example.niceadedektor.ui.components.ThreeDimensionalVisualizer
import com.example.niceadedektor.ui.components.TwoDimensionalView
import com.example.niceadedektor.ui.theme.NiceaDedektorTheme
import com.example.niceadedektor.util.ScanGenerators

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NiceaDedektorTheme {
                NiceaDedektorApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NiceaDedektorApp() {
    // Current Active Tab
    var activeTab by remember { mutableStateOf("3d") } // 3d, 2d, live, plan

    // Active Grid & Anomalies
    var currentGrid by remember { mutableStateOf(ScanGenerators.generateAntikMezarOdasi()) }
    val anomalies = remember(currentGrid) { ScanGenerators.detectAnomalies(currentGrid) }

    // 3D Parameters
    var palette by remember { mutableStateOf(PaletteType.NICEA_SPECTRUM) }
    var viewMode by remember { mutableStateOf(ViewMode.SURFACE) }
    var depthCutMeters by remember { mutableFloatStateOf(7.5f) }
    var thresholdFilter by remember { mutableFloatStateOf(15f) }
    var wireframe by remember { mutableStateOf(false) }
    var cameraPreset by remember { mutableStateOf(CameraPreset.ISO) }
    var selectedAnomalyId by remember { mutableStateOf<String?>(null) }
    var isControlsCollapsed by remember { mutableStateOf(false) }

    // Modals & Menus
    var isAnalysisOpen by remember { mutableStateOf(false) }
    var isPresetsMenuOpen by remember { mutableStateOf(false) }

    fun loadPreset(key: String) {
        selectedAnomalyId = null
        currentGrid = when (key) {
            "gold" -> ScanGenerators.generateAltinKupu()
            "tunel" -> ScanGenerators.generateTunelVeMagara()
            else -> ScanGenerators.generateAntikMezarOdasi()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Radar,
                            contentDescription = "Nicea Logo",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "NICEA DEDEKTÖR",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "3D & 2D Yeraltı ve Alan Tarama",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                ),
                actions = {
                    // Presets Dropdown
                    Box {
                        IconButton(
                            onClick = { isPresetsMenuOpen = true },
                            modifier = Modifier.testTag("preset_menu_button")
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = "Saha Preseti Seç", tint = Color(0xFFF59E0B))
                        }

                        DropdownMenu(
                            expanded = isPresetsMenuOpen,
                            onDismissRequest = { isPresetsMenuOpen = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Lahit & Mezar Odası", color = Color.White) },
                                onClick = {
                                    loadPreset("lahit")
                                    isPresetsMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Altın & Sikke Küpü", color = Color.White) },
                                onClick = {
                                    loadPreset("gold")
                                    isPresetsMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tünel & Mahzen Hattı", color = Color.White) },
                                onClick = {
                                    loadPreset("tunel")
                                    isPresetsMenuOpen = false
                                }
                            )
                        }
                    }

                    // Analysis Report Modal
                    IconButton(
                        onClick = { isAnalysisOpen = true },
                        modifier = Modifier.testTag("analysis_button")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = "Rapor", tint = Color(0xFF38BDF8))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = activeTab == "3d",
                    onClick = { activeTab = "3d" },
                    icon = { Icon(Icons.Default.ViewInAr, contentDescription = null) },
                    label = { Text("3D Görünüm", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFF59E0B),
                        indicatorColor = Color(0x33F59E0B),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "2d",
                    onClick = { activeTab = "2d" },
                    icon = { Icon(Icons.Default.Layers, contentDescription = null) },
                    label = { Text("2D & Kesit", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFF59E0B),
                        indicatorColor = Color(0x33F59E0B),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "live",
                    onClick = { activeTab = "live" },
                    icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = null) },
                    label = { Text("Canlı Tarama", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFF59E0B),
                        indicatorColor = Color(0x33F59E0B),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "plan",
                    onClick = { activeTab = "plan" },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                    label = { Text("Mimari Plan", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFF59E0B),
                        indicatorColor = Color(0x33F59E0B),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF020617))
        ) {
            when (activeTab) {
                "3d" -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ThreeDimensionalVisualizer(
                            grid = currentGrid,
                            palette = palette,
                            viewMode = viewMode,
                            depthCutMeters = depthCutMeters,
                            thresholdFilter = thresholdFilter,
                            wireframe = wireframe,
                            selectedAnomalyId = selectedAnomalyId,
                            onSelectAnomaly = { selectedAnomalyId = it?.id },
                            anomalies = anomalies,
                            cameraPreset = cameraPreset
                        )

                        // Bottom Docked Controls
                        BottomControlsBar(
                            palette = palette,
                            onSelectPalette = { palette = it },
                            viewMode = viewMode,
                            onSelectViewMode = { viewMode = it },
                            depthCutMeters = depthCutMeters,
                            onChangeDepthCut = { depthCutMeters = it },
                            thresholdFilter = thresholdFilter,
                            onChangeThreshold = { thresholdFilter = it },
                            wireframe = wireframe,
                            onToggleWireframe = { wireframe = !wireframe },
                            cameraPreset = cameraPreset,
                            onSelectCamera = { cameraPreset = it },
                            isCollapsed = isControlsCollapsed,
                            onToggleCollapse = { isControlsCollapsed = !isControlsCollapsed },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }

                "2d" -> {
                    TwoDimensionalView(
                        grid = currentGrid,
                        palette = palette,
                        anomalies = anomalies,
                        selectedAnomalyId = selectedAnomalyId,
                        onSelectAnomaly = { selectedAnomalyId = it?.id }
                    )
                }

                "live" -> {
                    LiveScannerScreen(
                        onScanCompleted = { newGrid ->
                            currentGrid = newGrid
                            selectedAnomalyId = null
                            activeTab = "3d"
                        }
                    )
                }

                "plan" -> {
                    DevelopmentPlanScreen()
                }
            }
        }
    }

    // Detailed Report & Export Modal
    AnalysisDialog(
        isOpen = isAnalysisOpen,
        onClose = { isAnalysisOpen = false },
        grid = currentGrid,
        anomalies = anomalies
    )
}
