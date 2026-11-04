package com.lelesoftware.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import com.lelesoftware.app.ui.components.MagneticSurveyCompass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.lelesoftware.app.data.local.AnomalyRepository
import com.lelesoftware.app.model.CameraPreset
import com.lelesoftware.app.model.PaletteType
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.model.ViewMode
import com.lelesoftware.app.ui.components.AnalysisDialog
import com.lelesoftware.app.ui.components.BottomControlsBar
import com.lelesoftware.app.ui.components.DevelopmentPlanScreen
import com.lelesoftware.app.ui.components.LiveScannerScreen
import com.lelesoftware.app.ui.components.LoginScreen
import com.lelesoftware.app.ui.components.SavedAnomaliesDialog
import com.lelesoftware.app.ui.components.SoilDensityDepthChart
import com.lelesoftware.app.ui.components.SplashScreen
import com.lelesoftware.app.ui.components.ThreeDimensionalVisualizer
import com.lelesoftware.app.ui.components.TwoDimensionalView
import com.lelesoftware.app.ui.theme.LeleSoftwareTheme
import com.lelesoftware.app.util.ScanGenerators
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeleSoftwareTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }
    var operatorName by rememberSaveable { mutableStateOf("Baş Mühendis") }

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
    } else if (!isAuthenticated) {
        LoginScreen(
            onLoginSuccess = { name ->
                operatorName = name
                isAuthenticated = true
            },
            onGuestBypass = {
                operatorName = "Saha Operatörü (Çevrimdışı)"
                isAuthenticated = true
            }
        )
    } else {
        LeleSoftwareApp(
            onShowSplash = { showSplash = true },
            onLogout = { isAuthenticated = false },
            operatorName = operatorName
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeleSoftwareApp(
    onShowSplash: () -> Unit,
    onLogout: () -> Unit = {},
    operatorName: String = "Baş Mühendis"
) {
    // Current Active Tab
    var activeTab by remember { mutableStateOf("3d") } // 3d, 2d, live, plan

    // BackHandler: if not on 3d home tab, return to 3d tab
    BackHandler(enabled = activeTab != "3d") {
        activeTab = "3d"
    }

    // Active Grid & Anomalies
    var currentGrid by remember { mutableStateOf(ScanGenerators.generateAntikMezarOdasi()) }
    val anomalies = remember(currentGrid) { ScanGenerators.detectAnomalies(currentGrid) }

    // 3D Parameters
    var palette by remember { mutableStateOf(PaletteType.LELE_SPECTRUM) }
    var viewMode by remember { mutableStateOf(ViewMode.SURFACE) }
    var depthCutMeters by remember { mutableFloatStateOf(7.5f) }
    var thresholdFilter by remember { mutableFloatStateOf(15f) }
    var wireframe by remember { mutableStateOf(false) }
    var cameraPreset by remember { mutableStateOf(CameraPreset.ISO) }
    var selectedAnomalyId by remember { mutableStateOf<String?>(null) }
    var isControlsCollapsed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { AnomalyRepository.getInstance(context) }
    val savedAnomalies by repository.allAnomalies.collectAsState(initial = emptyList())

    // Modals & Menus
    var isAnalysisOpen by remember { mutableStateOf(false) }
    var isPresetsMenuOpen by remember { mutableStateOf(false) }
    var isCompassOpen by remember { mutableStateOf(false) }
    var isD3ChartOpen by remember { mutableStateOf(false) }
    var isSavedAnomaliesOpen by remember { mutableStateOf(false) }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onShowSplash() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_lele_software),
                            contentDescription = "LELE SOFTWARE Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "LELE SOFTWARE",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                                ) {
                                    Text(
                                        "PRO",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                "Yazılım Mühendisliği • 3D/2D Yeraltı Görüntüleme",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                ),
                actions = {
                    // D3 Real-Time Soil Density & Anomaly Depth Chart Toggle Button
                    IconButton(
                        onClick = { isD3ChartOpen = !isD3ChartOpen },
                        modifier = Modifier.testTag("d3_chart_button")
                    ) {
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = "D3 Toprak Yoğunluğu & Derinlik Grafiği",
                            tint = if (isD3ChartOpen) Color(0xFF00E5FF) else Color(0xFF94A3B8)
                        )
                    }

                    // Local SQLite Room Database Offline Storage Button
                    IconButton(
                        onClick = { isSavedAnomaliesOpen = true },
                        modifier = Modifier.testTag("sqlite_storage_button")
                    ) {
                        Box {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = "Kayıtlı Anomaliler (SQLite Room)",
                                tint = Color(0xFF10B981)
                            )
                            if (savedAnomalies.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${savedAnomalies.size.coerceAtMost(99)}",
                                            color = Color.Black,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Earth Magnetic Survey Compass Button
                    IconButton(
                        onClick = { isCompassOpen = true },
                        modifier = Modifier.testTag("compass_button")
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = "Manyetik Pusula", tint = Color(0xFF10B981))
                    }

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
                            DropdownMenuItem(
                                text = { Text("Manyetik Hat Pusulası", color = Color(0xFF10B981)) },
                                onClick = {
                                    isPresetsMenuOpen = false
                                    isCompassOpen = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Çoklu Platform (Android • iOS • Windows)", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
                                onClick = {
                                    isPresetsMenuOpen = false
                                    activeTab = "plan"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("LELE SOFTWARE Tanıtım & Açılış", color = Color(0xFF38BDF8)) },
                                onClick = {
                                    isPresetsMenuOpen = false
                                    onShowSplash()
                                }
                            )
                            HorizontalDivider(color = Color(0xFF1E293B))
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Aktif Operatör: $operatorName", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Oturumu Kapat", color = Color(0xFFFCA5A5), fontSize = 13.sp)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Logout, contentDescription = "Çıkış", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    isPresetsMenuOpen = false
                                    onLogout()
                                },
                                modifier = Modifier.testTag("logout_menu_item")
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

                    // Operator Account / Logout Button
                    IconButton(
                        onClick = { onLogout() },
                        modifier = Modifier.testTag("account_logout_button")
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Kullanıcı: $operatorName (Çıkış yap)",
                            tint = Color(0xFF00E5FF)
                        )
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
                    label = { Text("3D Görünüm", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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
                    label = { Text("2D & Kesit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null) },
                    label = { Text("Canlı Tarama", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                    label = { Text("Mimari & Çoklu Platform", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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

                        // D3 Chart Component Floating on Main 3D Dashboard
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Surface(
                                modifier = Modifier
                                    .clickable { isD3ChartOpen = !isD3ChartOpen }
                                    .testTag("toggle_d3_chart_button"),
                                shape = RoundedCornerShape(20.dp),
                                color = if (isD3ChartOpen) Color(0xFF00E5FF) else Color(0xCC0F172A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isD3ChartOpen) Color(0xFF00E5FF) else Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Timeline,
                                        contentDescription = null,
                                        tint = if (isD3ChartOpen) Color.Black else Color(0xFF00E5FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isD3ChartOpen) "D3 Grafiği Gizle" else "D3 Toprak Yoğunluğu & Derinlik",
                                        color = if (isD3ChartOpen) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (isD3ChartOpen) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.widthIn(max = 380.dp)) {
                                    SoilDensityDepthChart(
                                        anomalies = anomalies,
                                        currentDepth = (selectedAnomalyId?.let { id -> anomalies.find { it.id == id }?.depthMeters } ?: 3.2f),
                                        currentDensity = 1.95f,
                                        onSaveToDatabase = { anomaly ->
                                            coroutineScope.launch {
                                                repository.saveAnomaly(
                                                    anomaly = anomaly,
                                                    scanId = currentGrid.id,
                                                    locationName = currentGrid.locationName
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

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

    // Detailed Report & Export Modal with Room Database Save
    AnalysisDialog(
        isOpen = isAnalysisOpen,
        onClose = { isAnalysisOpen = false },
        grid = currentGrid,
        anomalies = anomalies,
        onSaveAllToDatabase = {
            coroutineScope.launch {
                repository.saveAllAnomalies(
                    anomalies = anomalies,
                    scanId = currentGrid.id,
                    locationName = currentGrid.locationName
                )
            }
        }
    )

    // SQLite Room Database Offline Anomalies Modal
    SavedAnomaliesDialog(
        isOpen = isSavedAnomaliesOpen,
        onClose = { isSavedAnomaliesOpen = false },
        savedAnomalies = savedAnomalies,
        onSelectAnomaly = { entity ->
            selectedAnomalyId = entity.id
            activeTab = "3d"
        },
        onDeleteAnomaly = { id ->
            coroutineScope.launch { repository.deleteAnomaly(id) }
        },
        onClearAll = {
            coroutineScope.launch { repository.clearAll() }
        }
    )

    // Earth Magnetic Survey Compass Modal
    if (isCompassOpen) {
        Dialog(onDismissRequest = { isCompassOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MagneticSurveyCompass()
                Button(
                    onClick = { isCompassOpen = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_compass_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "TAMAM / PUSULAYI KAPAT",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
