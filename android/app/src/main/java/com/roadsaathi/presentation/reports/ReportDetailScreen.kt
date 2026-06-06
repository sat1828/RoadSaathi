package com.roadsaathi.presentation.reports

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.roadsaathi.data.local.entity.HazardReportEntity
import com.roadsaathi.presentation.common.components.LoadingIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    localId: String,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<HazardReportEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load report - in a real app this would come from a ViewModel
    // For now, we show a loading state with the detail structure

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            LoadingIndicator(
                modifier = Modifier.padding(padding),
                fullScreen = true
            )
        } else if (report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Report not found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val r = report!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Photo
                if (r.photoLocalPath != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = r.photoLocalPath),
                            contentDescription = "Hazard photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Type and confidence badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getTypeColor(r.hazardType).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = r.classificationLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = getTypeColor(r.hazardType),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getConfidenceColor(r.confidenceScore).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${(r.confidenceScore * 100).toInt()}% confidence",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = getConfidenceColor(r.confidenceScore),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info cards
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        infoRow(Icons.Default.LocationOn, "Location", "${"%.6f".format(r.latitude)}, ${"%.6f".format(r.longitude)}")
                        infoRow(Icons.Default.AccessTime, "Reported", formatTimestamp(r.reportedAt))
                        infoRow(Icons.Default.Info, "Status", r.syncStatus.name)
                        infoRow(Icons.Default.Route, "NH Corridor", r.nhCorridor ?: "N/A")
                        infoRow(Icons.Default.Warning, "Severity", "${r.severity}/5")
                        if (r.remoteId != null) {
                            infoRow(Icons.Default.Circle, "Remote ID", r.remoteId)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sync timeline
                Text(
                    text = "Sync Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                syncTimelineItem(r.syncStatus)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun infoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun syncTimelineItem(status: com.roadsaathi.data.local.SyncStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            Icons.Default.Circle,
            contentDescription = null,
            tint = getSyncStatusColor(status),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when (status) {
                com.roadsaathi.data.local.SyncStatus.PENDING -> "Pending sync"
                com.roadsaathi.data.local.SyncStatus.IN_FLIGHT -> "Syncing..."
                com.roadsaathi.data.local.SyncStatus.SYNCED -> "Synced to server"
                com.roadsaathi.data.local.SyncStatus.FAILED -> "Sync failed"
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun getTypeColor(type: String): Color {
    return when {
        type.contains("POTHOLE") || type.contains("pothole") -> Color(0xFF795548)
        type.contains("WATER") || type.contains("water") -> Color(0xFF1565C0)
        type.contains("ACCIDENT") || type.contains("accident") -> Color(0xFFD32F2F)
        type.contains("SIGNAGE") || type.contains("signage") -> Color(0xFFF9A825)
        type.contains("COLLAPSE") || type.contains("collapse") -> Color(0xFF4E342E)
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getConfidenceColor(confidence: Float): Color {
    return when {
        confidence >= 0.8f -> Color(0xFF43A047)
        confidence >= 0.5f -> Color(0xFFFFA000)
        else -> Color(0xFFE53935)
    }
}

private fun getSyncStatusColor(status: com.roadsaathi.data.local.SyncStatus): Color {
    return when (status) {
        com.roadsaathi.data.local.SyncStatus.PENDING -> Color(0xFFFFA000)
        com.roadsaathi.data.local.SyncStatus.IN_FLIGHT -> Color(0xFF1E88E5)
        com.roadsaathi.data.local.SyncStatus.SYNCED -> Color(0xFF43A047)
        com.roadsaathi.data.local.SyncStatus.FAILED -> Color(0xFFE53935)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
