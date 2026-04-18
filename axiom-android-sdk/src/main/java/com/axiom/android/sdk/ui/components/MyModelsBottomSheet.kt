package com.axiom.android.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axiom.android.sdk.domain.ModelUIItem
import com.axiom.android.sdk.ui.theme.AxiomTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyModelsBottomSheet(
    models: List<ModelUIItem>,
    onImportLocalModel: () -> Unit = {},
    onOptimizeStorage: () -> Unit = {},
    onModelPauseResume: (String) -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Blurred background content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AxiomTheme.colors.background)
        )
        
        // Bottom sheet
        BottomSheetScaffold(
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B1B23))
                ) {
                    // Drag handle area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(4.dp)
                                    .background(
                                        Color(0xFF464654).copy(alpha = 0.3f),
                                        RoundedCornerShape(999.dp)
                                    )
                            )
                        }
                        
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "My Models",
                                color = Color(0xFFE4E1ED),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = (-0.6).sp
                            )
                            
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFFE4E1ED),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        // Technical Stats Bar
                        TechnicalStatsBar()
                        
                        // Model List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            models.forEach { model ->
                                EnhancedModelCard(
                                    model = model,
                                    onPauseResume = { onModelPauseResume(model.id) }
                                )
                            }
                        }
                        
                        // Action Buttons
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Import Local Model - Gradient Button
                            Button(
                                onClick = onImportLocalModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFC0C1FF),
                                                    Color(0xFF8083FF)
                                                )
                                            ),
                                            shape = RoundedCornerShape(999.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color(0xFF07006C),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Import Local Model",
                                            color = Color(0xFF07006C),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            
                            // Optimize Model Storage - Outlined Button
                            OutlinedButton(
                                onClick = onOptimizeStorage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFC7C4D7)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = Color(0xFF464654).copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = null,
                                        tint = Color(0xFFC7C4D7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Optimize Model Storage",
                                        color = Color(0xFFC7C4D7),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            sheetPeekHeight = 600.dp,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = Color.Transparent,
            scaffoldState = rememberBottomSheetScaffoldState()
        ) {
            // Main content area (transparent to show blurred background)
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun TechnicalStatsBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Storage Card
        StatCard(
            label = "STORAGE",
            value = "42.8\nGB",
            valueColor = Color(0xFFC0C1FF),
            modifier = Modifier.weight(1f)
        )
        
        // Active Card
        StatCard(
            label = "ACTIVE",
            value = "3 Units",
            valueColor = Color(0xFF4EDea3),
            modifier = Modifier.weight(1f)
        )
        
        // Inference Card
        StatCard(
            label = "INFERENCE",
            value = "Local",
            valueColor = Color(0xFFDDB7FF),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F1F27)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFFC7C4D7),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
        }
    }
}

@Composable
fun EnhancedModelCard(
    model: ModelUIItem,
    onPauseResume: () -> Unit = {}
) {
    val isUpdating = model.downloadState is com.axiom.android.sdk.domain.ModelDownloadState.Updating
    val isDownloading = model.downloadState is com.axiom.android.sdk.domain.ModelDownloadState.Downloading
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F1F27)
        ),
        border = if (isUpdating) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color(0xFFC0C1FF).copy(alpha = 0.1f)
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Model name with status indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val statusColor = when {
                                isUpdating || isDownloading -> Color(0xFF4EDea3)
                                model.downloadState is com.axiom.android.sdk.domain.ModelDownloadState.Installed -> Color(0xFF4EDea3)
                                else -> Color(0xFFC0C1FF)
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, RoundedCornerShape(4.dp))
                            )
                        }
                        Text(
                            text = model.name,
                            color = Color(0xFFE4E1ED),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    
                    // Description
                    Text(
                        text = model.description,
                        color = Color(0xFFC7C4D7),
                        fontSize = 14.sp
                    )
                    
                    // Tags
                    if (!isUpdating) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Format tag
                            TagBadge(
                                text = "FORMAT: GGUF",
                                textColor = Color(0xFFC0C1FF)
                            )
                            // Size tag
                            TagBadge(
                                text = "SIZE: ${model.size}",
                                textColor = Color(0xFFC7C4D7)
                            )
                        }
                    }
                    
                    // Progress bar for updating/downloading
                    if (isUpdating || isDownloading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val progress = when (val state = model.downloadState) {
                            is com.axiom.android.sdk.domain.ModelDownloadState.Updating -> state.progress
                            is com.axiom.android.sdk.domain.ModelDownloadState.Downloading -> state.progress
                            else -> 0f
                        }
                        
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = Color(0xFF4EDea3),
                            trackColor = Color(0xFF34343D)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when {
                                    isUpdating -> "UPDATING... ${(progress * 100).toInt()}%"
                                    isDownloading -> "DOWNLOADING ${(progress * 100).toInt()}%"
                                    else -> ""
                                },
                                color = Color(0xFF4EDea3),
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = when (val state = model.downloadState) {
                                    is com.axiom.android.sdk.domain.ModelDownloadState.Updating -> state.downloadSpeed
                                    is com.axiom.android.sdk.domain.ModelDownloadState.Downloading -> state.downloadSpeed
                                    else -> ""
                                },
                                color = Color(0xFFC7C4D7),
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                
                // Action buttons
                if (!isUpdating) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isDownloading) {
                            IconButton(
                                onClick = onPauseResume,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = Color(0xFFC7C4D7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else if (model.downloadState is com.axiom.android.sdk.domain.ModelDownloadState.Paused) {
                            IconButton(
                                onClick = onPauseResume,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = Color(0xFFC7C4D7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = {},
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color(0xFFC7C4D7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFFE4E1ED),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagBadge(
    text: String,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(Color(0xFF34343D), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}
