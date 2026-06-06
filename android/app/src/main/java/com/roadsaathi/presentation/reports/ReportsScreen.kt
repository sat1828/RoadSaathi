package com.roadsaathi.presentation.reports

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadsaathi.data.local.SyncStatus
import com.roadsaathi.data.local.entity.HazardReportEntity
import com.roadsaathi.presentation.common.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.reports.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No reports yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Reports",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                items(
                    items = uiState.reports,
                    key = { it.localId }
                ) { report ->
                    val dismissState = rememberDismissState(
                        confirmValueChange = { value ->
                            if (value == DismissValue.DismissedToStart) {
                                viewModel.deleteReport(report.localId)
                                true
                            } else false
                        }
                    )

                    SwipeToDismiss(
                        state = dismissState,
                        directions = setOf(DismissDirection.EndToStart),
                        background = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Color(0xFFE53935),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White
                                )
                            }
                        },
                        dismissContent = {
                            ReportCard(
                                report = report,
                                onClick = { onNavigateToDetail(report.localId) }
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: HazardReportEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Surface(
                shape = CircleShape,
                color = getTypeColor(report.hazardType).copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = getTypeColor(report.hazardType),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.classificationLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${"%.4f".format(report.latitude)}, ${"%.4f".format(report.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTimeAgo(report.reportedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sync status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = null,
                    tint = getSyncStatusColor(report.syncStatus),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = report.syncStatus.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = getSyncStatusColor(report.syncStatus)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Confidence
            Text(
                text = "${(report.confidenceScore * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = getConfidenceColor(report.confidenceScore)
            )
        }
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

private fun getSyncStatusColor(status: SyncStatus): Color {
    return when (status) {
        SyncStatus.PENDING -> Color(0xFFFFA000)
        SyncStatus.IN_FLIGHT -> Color(0xFF1E88E5)
        SyncStatus.SYNCED -> Color(0xFF43A047)
        SyncStatus.FAILED -> Color(0xFFE53935)
    }
}

private fun getConfidenceColor(confidence: Float): Color {
    return when {
        confidence >= 0.8f -> Color(0xFF43A047)
        confidence >= 0.5f -> Color(0xFFFFA000)
        else -> Color(0xFFE53935)
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
