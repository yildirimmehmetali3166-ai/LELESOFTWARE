package com.example.niceadedektor.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Sensors
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.niceadedektor.R

@Composable
fun DevelopmentPlanScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("hardware") }

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
        // Header Card
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
                        Text("Nicea Dedektör - Sistem Mimarisi & Donanım Rehberi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("En Yüksek Hassasiyetli Donanım Parçaları, Şema ve Entegrasyon", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeTab == "hardware",
                        onClick = { activeTab = "hardware" },
                        label = { Text("Donanım & Parçalar", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = activeTab == "protocol",
                        onClick = { activeTab = "protocol" },
                        label = { Text("BLE Protokol", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = activeTab == "roadmap",
                        onClick = { activeTab = "roadmap" },
                        label = { Text("Yol Haritası", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = activeTab == "android",
                        onClick = { activeTab = "android" },
                        label = { Text("Yazılım", fontSize = 11.sp) }
                    )
                }
            }
        }

        when (activeTab) {
            "hardware" -> {
                HardwareSection()
            }
            "protocol" -> {
                ProtocolSection(onCopy = ::copyCode)
            }
            "roadmap" -> {
                RoadmapSection()
            }
            "android" -> {
                AndroidSection(onCopy = ::copyCode)
            }
        }
    }
}

@Composable
private fun HardwareSection() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Technical Schematic Diagram Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RESMİ SİSTEM DONANIM ŞEMASI & PROB ANATOMİSİ",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text(
                            text = "Hassasiyet: <0.1 nT",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Schematic Image Display
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                    color = Color.Black
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hardware_schematic),
                        contentDescription = "Donanım Şeması",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Şema: Çift Fluxgate Sensörlü (1.0m aralıklı) Dikey Gradyan Çubuğu, TI ADS1262 24/32-Bit ADC, STM32F427 DSP İşlemci, BNO085 9-Eksen IMU ve nRF52840 BLE 5.2 Mimari Bağlantısı.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // 2. Component Details Cards
        Text(
            text = "En İyi Sonuç İçin Gerekli Profesyonel Donanım Parçaları",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        HardwareComponentCard(
            category = "1. Manyetik Algılama (Sensör Grubu)",
            title = "Stefan Mayer FLC-100 veya Bartington Mag-03 Fluxgate Sensör",
            specs = "• Hassasiyet: 0.1 nT (nanoTesla) / <5 pT/√Hz gürültü\n• Ölçüm Aralığı: ±100 µT (Dünya manyetik alanı ~50 µT)\n• Montaj: Dikey prob borusunda aralarında tam 100 cm (1 metre) mesafe olacak şekilde 2 adet yerleştirilir (Üst Sensör A, Alt Sensör B). Alt sensör yerdeki anomalileri yakalarken, üst sensör atmosferik parazitleri süzerek gradiometrik farkı (Δ = A - B) kusursuz çıkartır.",
            tag = "Kritik Parça",
            tagColor = Color(0xFFEF4444)
        )

        HardwareComponentCard(
            category = "2. Sinyal Dönüştürücü (ADC)",
            title = "Texas Instruments ADS1262 (32-Bit) veya ADS1256 (24-Bit)",
            specs = "• Çözünürlük: Gerçek 24/32-Bit ultra düşük gürültülü Delta-Sigma ADC\n• Örnekleme: 38.4 kSPS'e kadar programlanabilir hız\n• Dahili PGA (Programlanabilir Kazanç Amfisi) ve 2.5V Ultra-Düşük Kaymalı Voltaj Referansı\n• Neden Şart: Standart mikrodenetleyicilerin 10-12 bit ADC'leri topraktaki nanoTesla seviyesindeki mikro anomalileri göremez. ADS1262 ile 1 LSB < 0.05 nT hassasiyet elde edilir.",
            tag = "Maksimum Hassasiyet",
            tagColor = Color(0xFFF59E0B)
        )

        HardwareComponentCard(
            category = "3. Ana İşlemci (DSP & Hesaplama)",
            title = "STM32F427 / STM32F401 (ARM Cortex-M4F 168 MHz) veya ESP32-S3",
            specs = "• Donanımsal FPU (Kayan Nokta Birimi) ve DSP talimat seti\n• SPI Arayüzü: ADS1262 ile 8 MHz hızında senkron veri transferi\n• Görevi: Sensör A ve Sensör B arasındaki farkı hesaplar, 50 Hz gerçek zamanlı Kalman / Butterworth gürültü filtrelerini koşturur ve açı verisiyle eşler.",
            tag = "İşlem Gücü",
            tagColor = Color(0xFF38BDF8)
        )

        HardwareComponentCard(
            category = "4. Eğim & Yön Algılama (IMU)",
            title = "Bosch Sensortec BNO085 (9-Eksenli AR/VR Hareket İşlemcisi)",
            specs = "• Entegre Sensör Füzyonu (3-eksenli ivmeölçer + 3-eksenli jiroskop + manyetometre)\n• Pitch (Eğim) ve Roll (Yalpa) sapmasını 0.1° hassasiyetle ölçer.\n• Neden Şart: Yürüyüş esnasında çubuğun 2 derece eğilmesi manyetik alanda yapay 50 nT sapma yaratır. BNO085'ten gelen açı verisi kullanılarak trigonometrik eğim düzeltmesi (Tilt Compensation) yapılır.",
            tag = "Eğim Düzeltme",
            tagColor = Color(0xFF10B981)
        )

        HardwareComponentCard(
            category = "5. Kablosuz İletişim (BLE)",
            title = "Nordic Semiconductor nRF52840 / nRF52832 BLE 5.2 Modülü",
            specs = "• Veri Hızı: 2 Mbps PHY destekli yüksek hızlı Bluetooth Low Energy\n• 50 Hz binary telemetri paketlerini telefon uygulamasına sıfır gecikme (latency <15ms) ile aktarır.\n• Düşük güç tüketimi sayesinde bataryayı korur.",
            tag = "Kablosuz Akış",
            tagColor = Color(0xFFA855F7)
        )

        HardwareComponentCard(
            category = "6. Güç Yönetimi & Gürültü İzolasyonu",
            title = "TI TPS7A4700 Ultra Düşük Gürültülü LDO Regülatör & 7.4V Li-Ion",
            specs = "• 4.17 µVRMS ultra-düşük çıkış gürültüsü (Sensör beslemesinde anahtarlamalı SMPS step-down KESİNLİKLE kullanılmamalıdır; SMPS manyetik gürültü yayar!)\n• Batarya: 2S 7.4V 18650 3500mAh Li-Ion pil + Type-C şarj devresi\n• Ayrı analog (AGND) ve dijital (DGND) toprak katmanları.",
            tag = "Temiz Enerji",
            tagColor = Color(0xFFEAB308)
        )

        HardwareComponentCard(
            category = "7. Taşıyıcı Gövde & Şaft Malzemesi",
            title = "%100 Antimanyetik Karbon Fiber veya Delrin Prob Borusu",
            specs = "• Çap: 40-50 mm, Uzunluk: 120-130 cm\n• Çubuk üzerinde ASLA demir, nikel, çelik vida veya metal parça bulunmamalıdır! Tüm vidalar Naylon / Poliamid veya Pirinç olmalıdır.\n• Sensör kabloları çift kat ekranlı (Shielded Twisted Pair) olmalı ve şasiye topraklanmalıdır.",
            tag = "Antimanyetik Yapı",
            tagColor = Color(0xFF64748B)
        )
    }
}

@Composable
private fun HardwareComponentCard(
    category: String,
    title: String,
    specs: String,
    tag: String,
    tagColor: Color
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
                Text(category, color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tagColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tagColor)
                ) {
                    Text(tag, color = tagColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(6.dp))
            Text(specs, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 17.sp)
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
