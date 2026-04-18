package com.axiom.android.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axiom.android.sdk.domain.ModelDownloadState
import com.axiom.android.sdk.domain.ModelUIItem
import com.axiom.android.sdk.ui.theme.AxiomTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppCard(
    model: ModelUIItem,
    onAddToApp: () -> Unit = {},
    onDelete: () -> Unit = {},
    onLaunchChat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isInstalled = model.downloadState is ModelDownloadState.Installed
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AxiomTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = AxiomTheme.colors.backgroundSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with avatar and info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar/Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            AxiomTheme.colors.primary.copy(alpha = 0.1f),
                            AxiomTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🤖",
                        fontSize = 28.sp
                    )
                }

                // Model Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = model.name,
                        color = AxiomTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = model.version,
                            color = AxiomTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "•",
                            color = AxiomTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = model.size,
                            color = AxiomTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Added on ${formatDate(model.version)}",
                        color = AxiomTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Description
            Text(
                text = model.description,
                color = AxiomTheme.colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // Action Buttons
            if (isInstalled) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Chat Now button (primary)
                    Button(
                        onClick = onLaunchChat,
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth(),
                        shape = AxiomTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AxiomTheme.colors.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Chat Now",
                            color = AxiomTheme.colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Remove button (secondary)
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .height(44.dp)
                            .fillMaxWidth(),
                        shape = AxiomTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AxiomTheme.colors.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            AxiomTheme.colors.error.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Remove",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Add to App button
                Button(
                    onClick = onAddToApp,
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth(),
                    shape = AxiomTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AxiomTheme.colors.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Add to App",
                        color = AxiomTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadProgressCard(
    model: ModelUIItem,
    onDownload: () -> Unit = {},
    onPauseResume: () -> Unit = {},
    onCancel: () -> Unit = {},
    onRemove: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AxiomTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = AxiomTheme.colors.backgroundSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Requirements badge (always shown)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(AxiomTheme.colors.backgroundTertiary, AxiomTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "REQUIREMENTS",
                    color = AxiomTheme.colors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with name and action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = model.name,
                            color = AxiomTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp
                        )
                        Text(
                            text = model.description,
                            color = AxiomTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    // Action buttons
                    ActionButtons(
                        downloadState = model.downloadState,
                        onPauseResume = onPauseResume,
                        onCancel = onCancel,
                        onDownload = onDownload,
                        onRemove = onRemove
                    )
                }

                // Progress Section (only for downloading/paused states)
                when (val state = model.downloadState) {
                    is ModelDownloadState.Downloading,
                    is ModelDownloadState.Paused -> {
                        val progress = when (state) {
                            is ModelDownloadState.Downloading -> state.progress
                            is ModelDownloadState.Paused -> state.progress
                            else -> 0f
                        }
                        val downloadedBytes = when (state) {
                            is ModelDownloadState.Downloading -> state.downloadedBytes
                            is ModelDownloadState.Paused -> state.downloadedBytes
                            else -> 0L
                        }
                        val totalBytes = when (state) {
                            is ModelDownloadState.Downloading -> state.totalBytes
                            is ModelDownloadState.Paused -> state.totalBytes
                            else -> 0L
                        }
                        val downloadSpeed = when (state) {
                            is ModelDownloadState.Downloading -> state.downloadSpeed
                            else -> "Paused"
                        }

                        ProgressSection(
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            downloadSpeed = downloadSpeed
                        )
                    }
                    is ModelDownloadState.Failed -> {
                        ErrorSection(
                            errorMessage = state.error
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ErrorSection(
    errorMessage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AxiomTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = AxiomTheme.colors.error.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Download could not be completed",
                color = AxiomTheme.colors.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = errorMessage,
                color = AxiomTheme.colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ProgressSection(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    downloadSpeed: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(AxiomTheme.colors.divider, AxiomTheme.shapes.full)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        AxiomTheme.colors.success,
                        AxiomTheme.shapes.full
                    )
            )
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}",
                color = AxiomTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = downloadSpeed,
                color = AxiomTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                color = AxiomTheme.colors.accentLuminous,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = estimateTimeRemaining(progress, totalBytes, downloadedBytes),
                color = AxiomTheme.colors.success,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ActionButtons(
    downloadState: ModelDownloadState,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    onDownload: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (downloadState) {
            is ModelDownloadState.Downloading -> {
                IconButton(
                    onClick = onPauseResume,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PauseCircle,
                        contentDescription = "Pause",
                        tint = AxiomTheme.colors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "Cancel",
                        tint = AxiomTheme.colors.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is ModelDownloadState.Paused -> {
                IconButton(
                    onClick = onPauseResume,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Resume",
                        tint = AxiomTheme.colors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "Cancel",
                        tint = AxiomTheme.colors.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is ModelDownloadState.NotStarted -> {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Download",
                        tint = AxiomTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is ModelDownloadState.Installed -> {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "Remove",
                        tint = AxiomTheme.colors.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is ModelDownloadState.Failed -> {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Retry",
                        tint = AxiomTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is ModelDownloadState.Updating -> {
                // No buttons in updating state
            }
        }
    }
}

// Helper functions
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun estimateTimeRemaining(progress: Float, totalBytes: Long, downloadedBytes: Long): String {
    if (progress <= 0f || progress >= 1f) return "Calculating..."
    
    val remainingBytes = totalBytes - downloadedBytes
    // Assume 850 KB/s as average speed (from Figma example)
    val avgSpeed = 850 * 1024L
    val remainingSeconds = remainingBytes / avgSpeed
    
    return when {
        remainingSeconds < 60 -> "${remainingSeconds}s remaining"
        remainingSeconds < 3600 -> "${remainingSeconds / 60}m remaining"
        else -> "${remainingSeconds / 3600}h remaining"
    }
}

private fun formatDate(dateString: String): String {
    // Try to parse the date string, if it's not a date format, return as is
    return try {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val currentDate = Date()
        dateFormat.format(currentDate)
    } catch (e: Exception) {
        dateString
    }
}
