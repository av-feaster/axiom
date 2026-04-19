package com.axiom.android.sdk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axiom.android.sdk.domain.ModelDownloadState
import com.axiom.android.sdk.domain.ModelUIItem
import com.axiom.android.sdk.ui.theme.AxiomTheme
import kotlinx.coroutines.launch

private const val TAG = "DownloadsScreen"

/**
 * Downloads Screen - shows active downloads and download history
 * Based on the HTML design provided
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    models: List<ModelUIItem>,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onNavigateToStore: () -> Unit = {},
    onNavigateToMyModels: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeDownloads = remember(models) {
        models.filter { it.downloadState is ModelDownloadState.Downloading || it.downloadState is ModelDownloadState.Paused }
    }
    val completedDownloads = remember(models) {
        models.filter { it.downloadState is ModelDownloadState.Installed }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AxiomTheme.colors.background)
    ) {
        // Header
        DownloadsHeader()

        // Main Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section
            item {
                HeroSection()
            }

            // Featured Cards
            item {
                FeaturedCardsSection()
            }

            // Downloads Manager (Bottom Sheet style)
            item {
                DownloadsManagerSheet(
                    activeDownloads = activeDownloads,
                    completedDownloads = completedDownloads,
                    onPauseDownload = onPauseDownload,
                    onResumeDownload = onResumeDownload,
                    onCancelDownload = onCancelDownload,
                    onDeleteModel = onDeleteModel
                )
            }
        }

        // Bottom Navigation
        DownloadsBottomNavigationBar(
            selectedTab = BottomNavItem.Downloads,
            onNavigateToStore = onNavigateToStore,
            onNavigateToMyModels = onNavigateToMyModels
        )
    }
}

@Composable
private fun DownloadsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AxiomTheme.colors.background.copy(alpha = 0.8f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: Open menu */ }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = AxiomTheme.colors.primary
                )
            }
            Text(
                text = "The Atelier",
                color = AxiomTheme.colors.primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
        }
        IconButton(onClick = { /* TODO: Open search */ }) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = AxiomTheme.colors.primary
            )
        }
    }
}

@Composable
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(AxiomTheme.colors.backgroundSecondary),
        contentAlignment = Alignment.BottomStart
    ) {
        // Placeholder for hero image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF1F1F27),
                    shape = RoundedCornerShape(24.dp)
                )
        )
        
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Curated Selection",
                color = AxiomTheme.colors.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Neural Architecture v2.4",
                color = AxiomTheme.colors.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Experience the next evolution of multi-modal craftsmanship in our latest synthetic toolset.",
                color = AxiomTheme.colors.textSecondary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeaturedCardsSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large Featured Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(
                containerColor = AxiomTheme.colors.backgroundTertiary
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = AxiomTheme.colors.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "LLAMA-3 70B",
                            color = AxiomTheme.colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "⋮",
                        color = AxiomTheme.colors.textSecondary,
                        fontSize = 20.sp
                    )
                }
                
                Column {
                    Text(
                        text = "Meta-Llama-3-70B-Instruct",
                        color = AxiomTheme.colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Optimized for dialogue use cases and high-complexity reasoning tasks with enhanced safety protocols.",
                        color = AxiomTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Small Featured Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            colors = CardDefaults.cardColors(
                containerColor = AxiomTheme.colors.backgroundTertiary
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Color(0xFF292932),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
                
                Column {
                    Text(
                        text = "GGUF Quantization",
                        color = AxiomTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Run large models on consumer grade hardware with precision-loss mitigation.",
                        color = AxiomTheme.colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadsManagerSheet(
    activeDownloads: List<ModelUIItem>,
    completedDownloads: List<ModelUIItem>,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteModel: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AxiomTheme.colors.backgroundSecondary
        ),
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(6.dp)
                        .background(
                            AxiomTheme.colors.divider.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Downloads",
                        color = AxiomTheme.colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activeDownloads.size} Active • ${completedDownloads.size} Completed",
                        color = AxiomTheme.colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }
                IconButton(onClick = { /* TODO: Close sheet */ }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AxiomTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Active Queue
            if (activeDownloads.isNotEmpty()) {
                ActiveQueueSection(
                    downloads = activeDownloads,
                    onPauseDownload = onPauseDownload,
                    onResumeDownload = onResumeDownload,
                    onCancelDownload = onCancelDownload
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // History
            HistorySection(
                downloads = completedDownloads,
                    onDeleteModel = onDeleteModel
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Bar
            Button(
                onClick = { /* TODO: Optimize parallel downloads */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AxiomTheme.colors.primary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Optimize Parallel Downloads",
                    color = Color(0xFF07006C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ActiveQueueSection(
    downloads: List<ModelUIItem>,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE QUEUE",
                color = AxiomTheme.colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Surface(
                color = AxiomTheme.colors.success.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "AUTO-RESUME ON",
                    color = AxiomTheme.colors.success,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        downloads.forEach { model ->
            ActiveDownloadItem(
                model = model,
                onPauseDownload = onPauseDownload,
                onResumeDownload = onResumeDownload,
                onCancelDownload = onCancelDownload
            )
        }
    }
}

@Composable
private fun ActiveDownloadItem(
    model: ModelUIItem,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit
) {
    val downloadState = model.downloadState as? ModelDownloadState.Downloading 
        ?: model.downloadState as? ModelDownloadState.Paused
    
    val progress = when (downloadState) {
        is ModelDownloadState.Downloading -> downloadState.progress
        is ModelDownloadState.Paused -> downloadState.progress
        else -> 0f
    }
    
    val downloadedBytes = when (downloadState) {
        is ModelDownloadState.Downloading -> downloadState.downloadedBytes
        is ModelDownloadState.Paused -> downloadState.downloadedBytes
        else -> 0L
    }
    
    val totalBytes = when (downloadState) {
        is ModelDownloadState.Downloading -> downloadState.totalBytes
        is ModelDownloadState.Paused -> downloadState.totalBytes
        else -> 0L
    }
    
    val downloadSpeed = when (downloadState) {
        is ModelDownloadState.Downloading -> downloadState.downloadSpeed
        else -> "Queued"
    }

    val isPaused = downloadState is ModelDownloadState.Paused

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            AxiomTheme.colors.backgroundTertiary,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🤖",
                        fontSize = 24.sp
                    )
                }
                Column {
                    Text(
                        text = model.name,
                        color = AxiomTheme.colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)} • $downloadSpeed",
                        color = AxiomTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { if (isPaused) onResumeDownload(model.id) else onPauseDownload(model.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = if (isPaused) "▶" else "⏸",
                        fontSize = 20.sp
                    )
                }
                IconButton(
                    onClick = { onCancelDownload(model.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "✕",
                        fontSize = 20.sp,
                        color = AxiomTheme.colors.error
                    )
                }
            }
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    AxiomTheme.colors.backgroundTertiary,
                    shape = RoundedCornerShape(3.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        AxiomTheme.colors.success,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(progress * 100).toInt()}% Complete",
                color = AxiomTheme.colors.success,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isPaused) "Paused" else "Est. ${estimateTimeRemaining(progress, totalBytes, downloadedBytes)}",
                color = AxiomTheme.colors.textSecondary,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun HistorySection(
    downloads: List<ModelUIItem>,
    onDeleteModel: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HISTORY",
                color = AxiomTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            TextButton(onClick = { /* TODO: Clear all */ }) {
                Text(
                    text = "CLEAR ALL",
                    color = AxiomTheme.colors.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        downloads.forEach { model ->
            HistoryDownloadItem(
                model = model,
                onDeleteModel = onDeleteModel
            )
        }
    }
}

@Composable
private fun HistoryDownloadItem(
    model: ModelUIItem,
    onDeleteModel: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AxiomTheme.colors.background
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            AxiomTheme.colors.backgroundTertiary,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 20.sp,
                        color = AxiomTheme.colors.success
                    )
                }
                Column {
                    Text(
                        text = model.name,
                        color = AxiomTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${model.size} • Completed",
                        color = AxiomTheme.colors.textSecondary,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { /* TODO: Open folder */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "📁",
                        fontSize = 18.sp
                    )
                }
                IconButton(
                    onClick = { onDeleteModel(model.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "🗑",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadsBottomNavigationBar(
    selectedTab: BottomNavItem,
    onNavigateToStore: () -> Unit,
    onNavigateToMyModels: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        color = AxiomTheme.colors.backgroundSecondary.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Store
            BottomNavItemButton(
                icon = "🏪",
                label = "STORE",
                isSelected = selectedTab == BottomNavItem.Store,
                onClick = onNavigateToStore
            )
            
            // Downloads
            BottomNavItemButton(
                icon = "⬇️",
                label = "DOWNLOADS",
                isSelected = selectedTab == BottomNavItem.Downloads,
                onClick = {},
                isHighlighted = true
            )
            
            // My Models
            BottomNavItemButton(
                icon = "📦",
                label = "MY MODELS",
                isSelected = selectedTab == BottomNavItem.MyModels,
                onClick = onNavigateToMyModels
            )
        }
    }
}

@Composable
private fun BottomNavItemButton(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isHighlighted: Boolean = false
) {
    val backgroundColor = if (isHighlighted) {
        AxiomTheme.colors.primary
    } else {
        Color.Transparent
    }
    
    val contentColor = if (isHighlighted) {
        Color(0xFF07006C)
    } else {
        if (isSelected) AxiomTheme.colors.primary else AxiomTheme.colors.textSecondary
    }
    
    val alpha = if (isSelected || isHighlighted) 1f else 0.6f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(
                color = backgroundColor,
                shape = if (isHighlighted) RoundedCornerShape(16.dp) else RoundedCornerShape(12.dp)
            )
            .padding(if (isHighlighted) 16.dp else 8.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
            color = contentColor.copy(alpha = alpha)
        )
        Text(
            text = label,
            color = contentColor.copy(alpha = alpha),
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
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
    // Assume 4 MB/s as average speed (from HTML example)
    val avgSpeed = 4 * 1024 * 1024L
    val remainingSeconds = remainingBytes / avgSpeed
    
    return when {
        remainingSeconds < 60 -> "${remainingSeconds}s remaining"
        remainingSeconds < 3600 -> "${remainingSeconds / 60}m remaining"
        else -> "${remainingSeconds / 3600}h remaining"
    }
}

enum class BottomNavItem {
    Store,
    Downloads,
    MyModels
}
