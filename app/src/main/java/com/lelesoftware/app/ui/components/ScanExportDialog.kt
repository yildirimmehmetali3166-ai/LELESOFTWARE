package com.lelesoftware.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lelesoftware.app.model.DetectedAnomaly
import com.lelesoftware.app.model.ScanGrid
import com.lelesoftware.app.ui.theme.AmberPrimary
import com.lelesoftware.app.ui.theme.CyanAccent
import com.lelesoftware.app.ui.theme.DisplayFontFamily
import com.lelesoftware.app.ui.theme.EmeraldSuccess
import com.lelesoftware.app.ui.theme.MonoFontFamily
import com.lelesoftware.app.ui.theme.SlateCardBorder
import com.lelesoftware.app.ui.theme.SlateCardSurface
import com.lelesoftware.app.ui.theme.SlateDarkBackground
import com.lelesoftware.app.ui.theme.SlateElevatedSurface
import com.lelesoftware.app.ui.theme.TextMuted
import com.lelesoftware.app.util.ExportFileFormat
import com.lelesoftware.app.util.GridExportConfig
import com.lelesoftware.app.util.ReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScanExportDialog(
    isOpen: Boolean,
    initialFormat: ExportFileFormat = ExportFileFormat.PDF,
    grid: ScanGrid,
    anomalies: List<DetectedAnomaly>,
    config: GridExportConfig,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFormat by remember(isOpen, initialFormat) { mutableStateOf(initialFormat) }
    var smoothExport by remember(isOpen, config.smoothGrid) { mutableStateOf(config.smoothGrid) }
    var showValuesExport by remember(isOpen, config.showCellValues) { mutableStateOf(config.showCellValues) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastSavedBanner by remember { mutableStateOf<String?>(null) }
    var pendingSaveBytes by remember { mutableStateOf<ByteArray?>(null) }

    val effectiveConfig = remember(config, smoothExport, showValuesExport) {
        config.copy(smoothGrid = smoothExport, showCellValues = showValuesExport)
    }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(isOpen, grid, anomalies, effectiveConfig) {
        previewBitmap = withContext(Dispatchers.Default) {
            ReportExporter.renderGridBitmap(
                grid = grid,
                anomalies = anomalies,
                config = effectiveConfig,
                widthPx = 960,
                heightPx = 732
            )
        }
    }

    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val bytes = pendingSaveBytes
        pendingSaveBytes = null
        if (uri != null && bytes != null) {
            scope.launch(Dispatchers.IO) {
                val ok = ReportExporter.writeToUri(context, uri, bytes)
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    if (ok) {
                        lastSavedBanner = "${selectedFormat.label} seçilen konuma kaydedildi."
                        Toast.makeText(context, lastSavedBanner, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Dosya seçilen konuma yazılamadı.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            isProcessing = false
        }
    }

    fun generateExportBytes(format: ExportFileFormat): ByteArray {
        return when (format) {
            ExportFileFormat.PDF -> ReportExporter.exportPdfReportBytes(grid, anomalies, effectiveConfig)
            ExportFileFormat.PNG -> ReportExporter.exportGridImagePngBytes(grid, anomalies, effectiveConfig)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 540.dp)
                .testTag("scan_export_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = SlateCardSurface,
            border = BorderStroke(1.dp, SlateCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberPrimary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = if (selectedFormat == ExportFileFormat.PDF) Icons.Default.PictureAsPdf else Icons.Default.Image,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Izgara Görseli & Rapor Dışa Aktar",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${grid.title} (${grid.width}×${grid.height})",
                            fontSize = 11.sp,
                            color = CyanAccent,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).testTag("export_dialog_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }
                }

                // Format Switcher (PDF vs PNG)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == ExportFileFormat.PDF,
                        onClick = { selectedFormat = ExportFileFormat.PDF },
                        leadingIcon = {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text("PDF Rapor (.pdf)", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        },
                        modifier = Modifier.weight(1f).testTag("export_format_pdf_chip"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = SlateDarkBackground,
                            selectedLeadingIconColor = SlateDarkBackground
                        )
                    )
                    FilterChip(
                        selected = selectedFormat == ExportFileFormat.PNG,
                        onClick = { selectedFormat = ExportFileFormat.PNG },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text("PNG Görsel (.png)", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        },
                        modifier = Modifier.weight(1f).testTag("export_format_png_chip"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = SlateDarkBackground,
                            selectedLeadingIconColor = SlateDarkBackground
                        )
                    )
                }

                // Live Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(960f / 732f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF040814))
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = previewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Dışa aktarılacak ızgara önizlemesi",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = AmberPrimary, modifier = Modifier.size(30.dp))
                    }
                }

                // Render Options Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = smoothExport,
                        onClick = { smoothExport = !smoothExport },
                        label = { Text("Yumuşatılmış", fontSize = 11.sp, maxLines = 1) },
                        modifier = Modifier.testTag("export_option_smooth_chip")
                    )
                    FilterChip(
                        selected = showValuesExport,
                        onClick = { showValuesExport = !showValuesExport },
                        label = { Text("Hücre Değerleri", fontSize = 11.sp, maxLines = 1) },
                        modifier = Modifier.testTag("export_option_values_chip")
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${anomalies.size} Hedef",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        color = AmberPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Info Line for PDF vs PNG
                Text(
                    text = if (selectedFormat == ExportFileFormat.PDF) {
                        "A4 çok sayfalı PDF rapor; ızgara haritası, KPI metrikleri, sinyal aralığı ve tüm anomali tablosunu içerir."
                    } else {
                        "1600×1220 yüksek çözünürlüklü PNG görsel; aktif renk paleti, metre eksenleri ve anomali halkalarını içerir."
                    },
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                lastSavedBanner?.let { msg ->
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Text(text = msg, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Action Buttons Row 1: Save Directly & Save As (SAF)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        enabled = !isProcessing,
                        onClick = {
                            isProcessing = true
                            val fmt = selectedFormat
                            val fileName = ReportExporter.suggestedFileName(grid, fmt)
                            scope.launch(Dispatchers.IO) {
                                val bytes = generateExportBytes(fmt)
                                val savedUri = ReportExporter.saveToDevicePublicCollection(
                                    context = context,
                                    fileName = fileName,
                                    mimeType = fmt.mimeType,
                                    bytes = bytes
                                )
                                withContext(Dispatchers.Main) {
                                    if (savedUri != null) {
                                        isProcessing = false
                                        val folder = if (fmt == ExportFileFormat.PNG) "Resimler/LELE SOFTWARE" else "İndirilenler/LELE SOFTWARE"
                                        lastSavedBanner = "$folder/$fileName kaydedildi"
                                        Toast.makeText(context, lastSavedBanner, Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Android 8–9 fallback: launch zero-permission Storage Access Framework picker
                                        pendingSaveBytes = bytes
                                        saveAsLauncher.launch(fileName)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("export_save_device_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = SlateDarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cihaza Kaydet", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        enabled = !isProcessing,
                        onClick = {
                            isProcessing = true
                            val fmt = selectedFormat
                            val fileName = ReportExporter.suggestedFileName(grid, fmt)
                            scope.launch(Dispatchers.IO) {
                                val bytes = generateExportBytes(fmt)
                                withContext(Dispatchers.Main) {
                                    pendingSaveBytes = bytes
                                    saveAsLauncher.launch(fileName)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_save_as_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Farklı Kaydet", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }
                }

                // Action Buttons Row 2: Open Preview & Share via FileProvider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = !isProcessing,
                        onClick = {
                            isProcessing = true
                            val fmt = selectedFormat
                            val fileName = ReportExporter.suggestedFileName(grid, fmt)
                            scope.launch(Dispatchers.IO) {
                                val bytes = generateExportBytes(fmt)
                                val file = ReportExporter.writeCacheFile(context, fileName, bytes)
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    val opened = ReportExporter.openFile(context, file, fmt.mimeType)
                                    if (!opened) {
                                        Toast.makeText(context, "Bu dosya türünü açacak uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_open_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateCardBorder)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aç", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                    }

                    Button(
                        enabled = !isProcessing,
                        onClick = {
                            isProcessing = true
                            val fmt = selectedFormat
                            val fileName = ReportExporter.suggestedFileName(grid, fmt)
                            scope.launch(Dispatchers.IO) {
                                val bytes = generateExportBytes(fmt)
                                val file = ReportExporter.writeCacheFile(context, fileName, bytes)
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    ReportExporter.shareFile(
                                        context = context,
                                        file = file,
                                        mimeType = fmt.mimeType,
                                        chooserTitle = "${fmt.label} Paylaş"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("export_share_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor = SlateDarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paylaş", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
