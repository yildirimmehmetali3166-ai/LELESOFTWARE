package com.lelesoftware.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lelesoftware.app.model.CsvBandSummary
import com.lelesoftware.app.model.CsvClusterFinding
import com.lelesoftware.app.model.CsvDeepAnalysisReport
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.util.ReportExporter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CsvDeepAnalysisDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    activeGrid: ScanGrid,
    anomalies: List<DetectedAnomaly>,
    beyazCiflikGrid: ScanGrid?,
    ekimGrid: ScanGrid?,
    onSelectGrid: (ScanGrid) -> Unit,
    onFocusFinding: (CsvClusterFinding) -> Unit,
    onSaveAllToDatabase: (() -> Unit)? = null
) {
    if (!isOpen) return

    val context = LocalContext.current
    var selectedTab by remember(activeGrid.id) { mutableIntStateOf(0) } // 0 = Active Analysis, 1 = Comparison
    val report = activeGrid.csvReport

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .testTag("csv_deep_analysis_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Derin Jeofizik CSV Analiz & Raporu",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = activeGrid.title,
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_csv_deep_analysis_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Switcher Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Aktif Dosya Derin Analizi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f).testTag("csv_tab_active"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF),
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        leadingIcon = {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Beyaz Çiftlik vs Ekim", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f).testTag("csv_tab_compare"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B),
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 0 && report != null) {
                        ActiveCsvReportContent(
                            report = report,
                            onFocusFinding = { finding ->
                                onFocusFinding(finding)
                                onClose()
                            }
                        )

                        if (onSaveAllToDatabase != null) {
                            var isSaved by remember(activeGrid.id) { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    onSaveAllToDatabase()
                                    isSaved = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("csv_save_db_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSaved) Color(0xFF10B981) else Color(0xFF00E5FF)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSaved) "CSV Anomalileri SQLite Veritabanına Kaydedildi" else "Tüm Bulguları SQLite Veritabanına Kaydet",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val csv = ReportExporter.generateCSV(activeGrid, anomalies)
                                    ReportExporter.shareReport(context, csv, "${activeGrid.id}_derin_analiz.csv")
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("csv_export_share_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CSV Raporu Paylaş", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val json = ReportExporter.generateJSON(activeGrid, anomalies)
                                    ReportExporter.shareReport(context, json, "${activeGrid.id}_derin_analiz.json")
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("csv_export_json_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("JSON Dışa Aktar", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else if (selectedTab == 0) {
                        Text(
                            text = "Bu tarama sentetik preset verisidir. Derin CSV analizi için yukarıdan Beyaz Çiftlik veya Ekim dosyasını seçin.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    } else {
                        CsvComparisonContent(
                            beyazCiflikGrid = beyazCiflikGrid,
                            ekimGrid = ekimGrid,
                            activeGridId = activeGrid.id,
                            onSelectGrid = {
                                onSelectGrid(it)
                                selectedTab = 0
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveCsvReportContent(
    report: CsvDeepAnalysisReport,
    onFocusFinding: (CsvClusterFinding) -> Unit
) {
    // Executive Verdict Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Yönetici Özeti & Fiziksel Karakteristik",
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = report.executiveSummary,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Text(
                text = String.format(
                    Locale.US,
                    "Ham Span: X [%.1f–%.1f] (ΔX≈%.2f) • Y [%.1f–%.1f] (ΔY=%.1f)",
                    report.rawXRange.first, report.rawXRange.second, report.stepXRaw,
                    report.rawYRange.first, report.rawYRange.second, report.stepYRaw
                ),
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
        }
    }

    // 6-KPI Grid (2 rows of 3 cards)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CsvKpiBox(
            label = "Matris & Geçerli",
            value = "${report.validRows}/${report.totalRows}",
            sub = "${report.width}×${report.height} • ${report.deadCalibrationCells} sıfır",
            accent = Color(0xFF38BDF8),
            modifier = Modifier.weight(1f)
        )
        CsvKpiBox(
            label = "Value Medyan / Ort.",
            value = fmt2(report.valueMedian),
            sub = "Ort ${fmt2(report.valueMean)} • σ=${fmt1(report.valueStdDev)}",
            accent = if (report.valueMedian < 0f) Color(0xFF60A5FA) else Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
        CsvKpiBox(
            label = "Tepe Empedans (Z)",
            value = fmt1(report.zMax),
            sub = "Medyan ${fmt1(report.zMedian)} • ${report.highImpedanceCount} Z>150",
            accent = Color(0xFFA855F7),
            modifier = Modifier.weight(1f)
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CsvKpiBox(
            label = "Value Aralığı",
            value = "${fmt1(report.valueMin)}..${fmt1(report.valueMax)}",
            sub = "MAD=${fmt1(report.valueMad)}",
            accent = Color(0xFF22C55E),
            modifier = Modifier.weight(1f)
        )
        CsvKpiBox(
            label = "Polarite Dengesi",
            value = "${report.positiveCount}+ / ${report.negativeCount}-",
            sub = "${report.polarityFlipCount} işaret kırılması",
            accent = Color(0xFF00E5FF),
            modifier = Modifier.weight(1f)
        )
        CsvKpiBox(
            label = "Doyum & Kalibrasyon",
            value = "${report.saturatedFloorCount} Doyum",
            sub = "|Z×V| ≈ ${report.calibrationProductMedian.roundToInt()}",
            accent = if (report.saturatedFloorCount > 0) Color(0xFFEF4444) else Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )
    }

    // 6-Band Longitudinal Profile Chart & Breakdown
    if (report.bandSummaries.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "6 Bantlı Boyuna Hat Profili (Medyan Value & Tepe Z)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                BandProfileCanvas(bands = report.bandSummaries)

                for (band in report.bandSummaries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bant ${band.bandIndex} (${band.xRangeLabel})",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = band.dominantBehavior,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Med V: ${fmt1(band.medianValue)}",
                                color = if (band.medianValue < 0f) Color(0xFF60A5FA) else Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Maks Z: ${fmt1(band.maxZ)}",
                                color = Color(0xFFA855F7),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Critical Findings List
    Text(
        text = "Kümelenmiş Kritik Anomali & Yapısal Bulgular (${report.findings.size})",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )

    for (finding in report.findings) {
        val accent = Color(finding.classification.colorHex)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(accent, CircleShape))
                        Text(
                            text = finding.category,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "Derinlik ~%.2fm", finding.estimatedDepthMeters),
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = finding.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = finding.interpretation,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "X: ${finding.xRange} • Y: ${finding.yRange} (${finding.cellCount} hücre)",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                    OutlinedButton(
                        onClick = { onFocusFinding(finding) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                        modifier = Modifier.height(34.dp).testTag("focus_finding_${finding.id}")
                    ) {
                        Icon(
                            Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("3D'de Odakla", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Field Recommendations
    if (report.fieldRecommendations.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Saha & Kalibrasyon Önerileri",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                for (rec in report.fieldRecommendations) {
                    Text(
                        text = "• $rec",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BandProfileCanvas(bands: List<CsvBandSummary>) {
    val maxAbsV = bands.maxOfOrNull { abs(it.medianValue) }?.coerceAtLeast(30f) ?: 60f
    val maxZ = bands.maxOfOrNull { it.maxZ }?.coerceAtLeast(100f) ?: 500f

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF040814))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val w = size.width
                val h = size.height
                val zeroY = h * 0.55f
                drawLine(Color(0xFF475569), Offset(0f, zeroY), Offset(w, zeroY), strokeWidth = 1.2f)

                val slotW = w / bands.size.coerceAtLeast(1).toFloat()
                val zPath = Path()

                bands.forEachIndexed { idx, b ->
                    val cx = idx * slotW + slotW * 0.5f
                    val barW = slotW * 0.44f
                    val normV = (b.medianValue / maxAbsV).coerceIn(-1f, 1f)
                    val barColor = if (b.medianValue >= 0f) Color(0xFFF59E0B) else Color(0xFF38BDF8)

                    if (normV >= 0f) {
                        val barH = normV * (zeroY * 0.85f)
                        drawRect(
                            color = barColor.copy(alpha = 0.78f),
                            topLeft = Offset(cx - barW * 0.5f, zeroY - barH),
                            size = Size(barW, barH.coerceAtLeast(3f))
                        )
                    } else {
                        val barH = abs(normV) * ((h - zeroY) * 0.85f)
                        drawRect(
                            color = barColor.copy(alpha = 0.78f),
                            topLeft = Offset(cx - barW * 0.5f, zeroY),
                            size = Size(barW, barH.coerceAtLeast(3f))
                        )
                    }

                    val zY = h - (b.maxZ / maxZ).coerceIn(0f, 1f) * (h * 0.88f)
                    if (idx == 0) zPath.moveTo(cx, zY) else zPath.lineTo(cx, zY)
                }

                drawPath(zPath, color = Color(0xFFA855F7), style = Stroke(width = 2.5f))
                bands.forEachIndexed { idx, b ->
                    val cx = idx * slotW + slotW * 0.5f
                    val zY = h - (b.maxZ / maxZ).coerceIn(0f, 1f) * (h * 0.88f)
                    drawCircle(Color(0xFFA855F7), radius = 4f, center = Offset(cx, zY))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (b in bands) {
                Text("B${b.bandIndex}", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CsvComparisonContent(
    beyazCiflikGrid: ScanGrid?,
    ekimGrid: ScanGrid?,
    activeGridId: String,
    onSelectGrid: (ScanGrid) -> Unit
) {
    val r1 = beyazCiflikGrid?.csvReport
    val r2 = ekimGrid?.csvReport

    if (r1 == null || r2 == null) {
        Text(
            text = "Karşılaştırma için her iki paketlenmiş tarama dosyasının da yüklü olması gerekir.",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Beyaz Çiftlik vs Ekim — Karşılaştırmalı Jeofizik Sentez",
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            CompareRow("Matris & Adım", "${r1.width}×${r1.height} (ΔY=${fmt1(r1.stepYRaw)})", "${r2.width}×${r2.height} (ΔY=${fmt1(r2.stepYRaw)})")
            CompareRow("Geçerli / Sıfır", "${r1.validRows}/${r1.totalRows} (${r1.deadCalibrationCells} sıfır)", "${r2.validRows}/${r2.totalRows} (${r2.deadCalibrationCells} sıfır)")
            CompareRow("Value Medyan", fmt2(r1.valueMedian), fmt2(r2.valueMedian))
            CompareRow("Value Min..Maks", "${fmt1(r1.valueMin)}..${fmt1(r1.valueMax)}", "${fmt1(r2.valueMin)}..${fmt1(r2.valueMax)}")
            CompareRow("Pozitif / Negatif", "${r1.positiveCount}+ / ${r1.negativeCount}-", "${r2.positiveCount}+ / ${r2.negativeCount}-")
            CompareRow("Tepe Empedans (Z)", "${fmt2(r1.zMax)} (${r1.highImpedanceCount} Z>150)", "${fmt2(r2.zMax)} (${r2.highImpedanceCount} Z>150)")
            CompareRow("Taban Doyumu (-172.8)", "${r1.saturatedFloorCount} hücre", "${r2.saturatedFloorCount} hücre")
            CompareRow("Polarite Kırılması", "${r1.polarityFlipCount} sınır", "${r2.polarityFlipCount} sınır")
            CompareRow("|Z × Value| Kalibr.", "≈ ${r1.calibrationProductMedian.roundToInt()}", "≈ ${r2.calibrationProductMedian.roundToInt()}")

            HorizontalDivider(color = Color(0xFF1E293B))

            Text(
                text = "• Beyaz Çiftlik (%96 pozitif) yüksek çözünürlüklü yapısal kırılmalar (Bant 4 Z=735.63 ve Bant 5 Z=907.80) ile Bant 6'da +57.78'e ulaşan temiz iletken odaklar sunar.\n" +
                    "• Ekim (%79 negatif) ise 2–5. bantlarda -172.80 taban doyumuna oturmuş geniş düşük direnç/boşluk platosu içinde Bant 1'de izole pozitif ada (+46.55) ve Bant 6'da 927.54'lük keskin empedans duvarı içerir.",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSelectGrid(beyazCiflikGrid) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeGridId == beyazCiflikGrid.id) Color(0xFF10B981) else Color(0xFF00E5FF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (activeGridId == beyazCiflikGrid.id) "Beyaz Çiftlik Aktif" else "Beyaz Çiftlik'i Aç",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { onSelectGrid(ekimGrid) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeGridId == ekimGrid.id) Color(0xFF10B981) else Color(0xFFF59E0B)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (activeGridId == ekimGrid.id) "Ekim'i Aç (Aktif)" else "Ekim'i Aç",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, v1: String, v2: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F172A))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp, modifier = Modifier.weight(1.1f))
        Text(v1, color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(v2, color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CsvKpiBox(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = Color(0xFF94A3B8), fontSize = 9.sp)
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Text(sub, color = Color(0xFFCBD5E1), fontSize = 9.sp)
        }
    }
}

private fun fmt1(v: Float): String = String.format(Locale.US, "%.1f", v)
private fun fmt2(v: Float): String = String.format(Locale.US, "%.2f", v)
