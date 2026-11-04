package com.lelesoftware.app.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lelesoftware.app.data.local.AnomalyEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modal dialog displaying anomalies persisted in the local SQLite Room database.
 * Supports searching, filtering, offline inspection, and deletion.
 */
@Composable
fun SavedAnomaliesDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    savedAnomalies: List<AnomalyEntity>,
    onSelectAnomaly: (AnomalyEntity) -> Unit,
    onDeleteAnomaly: (String) -> Unit,
    onClearAll: () -> Unit
) {
    if (!isOpen) return

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredList = savedAnomalies.filter { item ->
        val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.locationName.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "PRECIOUS" -> item.classification == "PRECIOUS"
            "VOID" -> item.classification == "VOID"
            "FERROUS" -> item.classification == "FERROUS"
            else -> true
        }
        matchesQuery && matchesFilter
    }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(680.dp)
                .testTag("saved_anomalies_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "KAYITLI ANOMALİLER (SQLite / ROOM)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Yerel SQLite Veritabanı • Çevrimdışı İnceleme",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Hedef adı, saha veya açıklama ara...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Tümü (${savedAnomalies.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E293B),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "PRECIOUS",
                        onClick = { selectedFilter = "PRECIOUS" },
                        label = { Text("Altın/Metal", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFF59E0B)
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "VOID",
                        onClick = { selectedFilter = "VOID" },
                        label = { Text("Boşluk/Mezar", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF00E5FF)
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "FERROUS",
                        onClick = { selectedFilter = "FERROUS" },
                        label = { Text("Demir", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFEF4444)
                        )
                    )
                }

                // List of Saved Anomalies
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (savedAnomalies.isEmpty()) "Veritabanında kayıtlı anomali bulunmuyor." else "Aramanızla eşleşen anomali bulunamadı.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Saha taraması veya analiz ekranından 'Kaydet' butonuna basarak kaydedebilirsiniz.",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            SavedAnomalyItem(
                                item = item,
                                dateFormat = dateFormat,
                                onSelect = {
                                    onSelectAnomaly(item)
                                    onClose()
                                },
                                onDelete = { onDeleteAnomaly(item.id) }
                            )
                        }
                    }
                }

                // Bottom Actions Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (savedAnomalies.isNotEmpty()) {
                        Button(
                            onClick = onClearAll,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tümünü Temizle", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Kapat", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedAnomalyItem(
    item: AnomalyEntity,
    dateFormat: SimpleDateFormat,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(item.colorHex))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(Color(item.colorHex), CircleShape))
                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(item.colorHex).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(item.colorHex))
                    ) {
                        Text(
                            text = "%${item.signalStrength}",
                            color = Color(item.colorHex),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(item.description, color = Color(0xFF94A3B8), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Derinlik: ${String.format(Locale.US, "%.2f", item.depthMeters)}m [±${String.format(Locale.US, "%.2f", (item.maxDepthMeters - item.minDepthMeters) / 2f)}m]",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "Yoğunluk: ${String.format(Locale.US, "%.2f", item.soilDensityGcm3)} g/cm³",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Konum: (${String.format(Locale.US, "%.1f", item.xMeters)}m, ${String.format(Locale.US, "%.1f", item.yMeters)}m)",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.locationName} • ${dateFormat.format(Date(item.recordedTimestamp))}",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
                Text(
                    text = "3D Görünüme Yükle ➔",
                    color = Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
