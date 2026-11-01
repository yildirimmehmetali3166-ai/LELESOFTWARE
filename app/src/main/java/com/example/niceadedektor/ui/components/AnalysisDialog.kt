package com.example.niceadedektor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.niceadedektor.model.DetectedAnomaly
import com.example.niceadedektor.model.ScanGrid
import com.example.niceadedektor.model.TargetClassification
import com.example.niceadedektor.util.ReportExporter
import java.util.Locale

@Composable
fun AnalysisDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    grid: ScanGrid,
    anomalies: List<DetectedAnomaly>
) {
    if (!isOpen) return
    val context = LocalContext.current

    val totalArea = (grid.width * grid.stepDistanceMeters) * (grid.height * grid.stepDistanceMeters)
    val maxDepth = anomalies.maxOfOrNull { it.depthMeters } ?: 0f

    val voidCount = anomalies.count { it.classification == TargetClassification.VOID }
    val preciousCount = anomalies.count { it.classification == TargetClassification.PRECIOUS }
    val ferrousCount = anomalies.count { it.classification == TargetClassification.FERROUS }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(640.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Jeofizik Saha Analiz Raporu",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = grid.title,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }
                }

                // Summary KPIs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Tarama Alanı",
                        value = "${totalArea.toInt()} m²",
                        subtitle = "${grid.width}x${grid.height} Nokta",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Maks. Derinlik",
                        value = String.format(Locale.US, "%.2f m", maxDepth),
                        subtitle = "Penetrasyon",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Anomali Sayısı",
                        value = "${anomalies.size} Hedef",
                        subtitle = "Tespit Edildi",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Target Classification Breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hedef Sınıflandırma Dağılımı", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Boşluk & Mezar Odaları:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("$voidCount Adet", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Değerli Metal (Altın/Bronz):", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("$preciousCount Adet", color = Color(0xFFEAB308), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ferromanyetik Metal (Demir):", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("$ferrousCount Adet", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Detailed Target List
                Text("Tespit Detayları", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                for (a in anomalies) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(a.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    "${a.signalStrength}%",
                                    color = Color(a.classification.colorHex),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(a.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Koordinat: (${a.xMeters}m, ${a.yMeters}m) | Derinlik: ${a.depthMeters}m | Hacim: ${a.volumeM3} m³",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Export Options Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val csv = ReportExporter.generateCSV(grid, anomalies)
                            ReportExporter.shareReport(context, csv, "${grid.id}_saha_raporu.csv")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV Paylaş", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val json = ReportExporter.generateJSON(grid, anomalies)
                            ReportExporter.shareReport(context, json, "${grid.id}_veri_paketi.json")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("JSON Dışa Aktar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color(0xFF64748B), fontSize = 10.sp)
        }
    }
}
