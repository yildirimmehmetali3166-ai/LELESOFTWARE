package com.example.niceadedektor.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DevelopmentPlanScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("roadmap") }

    fun copyCode(code: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, code))
        Toast.makeText(context, "$label panoya kopyalandı", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth().testTag("dev_plan_header_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                    Column {
                        Text("Nicea Dedektör - Sistem Mimarisi & Planı", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Jeofizik Donanım, BLE Protokolü ve Android/Python Geliştirme Haritası", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeTab == "roadmap",
                        onClick = { activeTab = "roadmap" },
                        label = { Text("Yol Haritası", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = activeTab == "protocol",
                        onClick = { activeTab = "protocol" },
                        label = { Text("BLE Protokol", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = activeTab == "android",
                        onClick = { activeTab = "android" },
                        label = { Text("Android Mimarisi", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = activeTab == "python",
                        onClick = { activeTab = "python" },
                        label = { Text("Python/AI", fontSize = 11.sp) }
                    )
                }
            }
        }

        when (activeTab) {
            "roadmap" -> {
                RoadmapSection()
            }
            "protocol" -> {
                ProtocolSection(onCopy = ::copyCode)
            }
            "android" -> {
                AndroidSection(onCopy = ::copyCode)
            }
            "python" -> {
                PythonSection(onCopy = ::copyCode)
            }
        }
    }
}

@Composable
private fun RoadmapSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlanMilestoneCard(
            step = "Faz 1",
            title = "Çift Sensörlü Fluxgate Gradiometre Donanımı",
            description = "0.1 nT hassasiyetli FGM sensörleri, STM32F4 mikroişlemci, 24-bit ADC ve Nordic nRF52840 BLE modülü ile 50 Hz gerçek zamanlı manyetik gradyan veri toplama çubuğu.",
            status = "Tamamlandı / Entegre",
            statusColor = Color(0xFF10B981)
        )
        PlanMilestoneCard(
            step = "Faz 2",
            title = "Android Jetpack Compose & 3D Projeksiyon Motoru",
            description = "İzometrik ve perspektif 3D yeraltı topoğrafyası, derinlik katman kesiti, 5 farklı jeofizik spektrum renk paleti ve anomali çekirdek tespiti.",
            status = "Aktif Sürüm",
            statusColor = Color(0xFFF59E0B)
        )
        PlanMilestoneCard(
            step = "Faz 3",
            title = "Kriging Enterpolasyonu & Dipol Ayrıştırma Modeli",
            description = "Manyetik dipol yanıtlarından nesne boyut, derinlik (FWHM katsayısı) ve kütlesel hacim (m³) tahmini için yapay zeka destekli dipol sınıflandırıcı.",
            status = "Geliştirme Aşamasında",
            statusColor = Color(0xFF38BDF8)
        )
        PlanMilestoneCard(
            step = "Faz 4",
            title = "RTK GNSS Entegrasyonu & 3D Nokta Bulutu Haritalandırma",
            description = "Santimetre hassasiyetinde santimetrik RTK GPS koordinatları ile arazi yüzey haritası üzerine 3D anomali katmanlarının bindirilmesi.",
            status = "Planlandı",
            statusColor = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun ProtocolSection(onCopy: (String, String) -> Unit) {
    val packetStructure = """// BLE GATT Karakteristik Yapısı (50 Hz Canlı Akış)
// Servis UUID: 0000FFF0-0000-1000-8000-00805F9B34FB
// Notify Karakteristik: 0000FFF1-0000-1000-8000-00805F9B34FB

struct SensorPacket {
    uint8_t  header[2];      // 0xAA, 0x55 (Paket Başlangıcı)
    uint32_t timestampMs;    // Zaman Damgası
    int32_t  sensorA_nT;     // Üst Fluxgate Sensör (nanoTesla)
    int32_t  sensorB_nT;     // Alt Fluxgate Sensör (nanoTesla)
    int16_t  pitch_deg;      // Eğim Açısı x10 (-900 .. +900)
    int16_t  roll_deg;       // Yalpa Açısı x10 (-1800 .. +1800)
    uint8_t  batteryPct;     // Batarya Yüzdesi (0 .. 100)
    uint8_t  checksum;       // XOR Kontrol Toplamı
};"""

    CodeSnippetCard(
        title = "BLE İletişim Protokolü & Paket Formatı",
        code = packetStructure,
        onCopy = { onCopy(packetStructure, "BLE Protokol Kodu") }
    )
}

@Composable
private fun AndroidSection(onCopy: (String, String) -> Unit) {
    val gradleDependencies = """// build.gradle.kts (Module :app)
dependencies {
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("no.nordicsemi.android:ble:2.7.2")
    implementation("no.nordicsemi.android:ble-ktx:2.7.2")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}"""

    CodeSnippetCard(
        title = "Android Gradle Bağımlılıkları & Kütüphaneler",
        code = gradleDependencies,
        onCopy = { onCopy(gradleDependencies, "Gradle Kodları") }
    )
}

@Composable
private fun PythonSection(onCopy: (String, String) -> Unit) {
    val pythonScript = """# Nicea Dedektör - Jeofizik Veri Analiz & Kriging Enterpolasyon
import numpy as np
import scipy.interpolate as interp

def interpolate_grid(raw_points, resolution=100):
    # Bilinear veya Kriging enterpolasyon matrisi
    grid_z = interp.griddata(
        points=raw_points[:, :2],
        values=raw_points[:, 2],
        xi=(grid_x, grid_y),
        method='cubic'
    )
    return grid_z"""

    CodeSnippetCard(
        title = "Python Jeofizik Algoritma Kütüphanesi",
        code = pythonScript,
        onCopy = { onCopy(pythonScript, "Python Kodu") }
    )
}

@Composable
private fun PlanMilestoneCard(
    step: String,
    title: String,
    description: String,
    status: String,
    statusColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                    ) {
                        Text(step, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun CodeSnippetCard(
    title: String,
    code: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF030712),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Text(
                    text = code,
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
