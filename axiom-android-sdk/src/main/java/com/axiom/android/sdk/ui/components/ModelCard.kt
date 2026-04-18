package com.axiom.android.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axiom.android.sdk.domain.ModelDownloadState
import com.axiom.android.sdk.domain.ModelUIItem

@Composable
fun ModelCard(
    model: ModelUIItem,
    onPauseResume: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF292932)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (model.downloadState) {
                            is ModelDownloadState.Installed -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF4EDea3), RoundedCornerShape(4.dp))
                                )
                            }
                            is ModelDownloadState.Downloading -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF4EDea3), RoundedCornerShape(4.dp))
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFC0C1FF), RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                    Text(
                        text = model.name,
                        color = Color(0xFFE4E1ED),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                when (model.downloadState) {
                    is ModelDownloadState.Installed -> {
                        Text(
                            text = "${model.size} • ${model.version}",
                            color = Color(0xFFC7C4D7),
                            fontSize = 14.sp
                        )
                    }
                    is ModelDownloadState.Downloading -> {
                        Text(
                            text = "Downloading - ${model.downloadState.downloadSpeed}",
                            color = Color(0xFFC7C4D7),
                            fontSize = 14.sp
                        )
                    }
                    is ModelDownloadState.Paused -> {
                        Text(
                            text = "Paused",
                            color = Color(0xFFC7C4D7),
                            fontSize = 14.sp
                        )
                    }
                    is ModelDownloadState.Updating -> {
                        Text(
                            text = model.downloadState.updateMessage,
                            color = Color(0xFFC7C4D7),
                            fontSize = 14.sp
                        )
                    }
                    is ModelDownloadState.Failed -> {
                        Text(
                            text = model.downloadState.error,
                            color = Color(0xFFFF6B6B),
                            fontSize = 14.sp
                        )
                    }
                    is ModelDownloadState.NotStarted -> {
                        Text(
                            text = model.description,
                            color = Color(0xFFC7C4D7),
                            fontSize = 14.sp
                        )
                    }
                }
                
                when (model.downloadState) {
                    is ModelDownloadState.Downloading,
                    is ModelDownloadState.Updating -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val progress = when (val state = model.downloadState) {
                            is ModelDownloadState.Downloading -> state.progress
                            is ModelDownloadState.Updating -> state.progress
                            else -> 0f
                        }
                        
                        LinearProgressIndicator(
                            progress = { progress },
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
                                text = when (model.downloadState) {
                                    is ModelDownloadState.Downloading -> "DOWNLOADING ${(progress * 100).toInt()}%"
                                    is ModelDownloadState.Updating -> "UPDATING ${(progress * 100).toInt()}%"
                                    else -> ""
                                },
                                color = Color(0xFF4EDea3),
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = when (model.downloadState) {
                                    is ModelDownloadState.Downloading -> model.downloadState.downloadSpeed
                                    is ModelDownloadState.Updating -> model.downloadState.downloadSpeed
                                    else -> ""
                                },
                                color = Color(0xFFC7C4D7),
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    else -> {}
                }
            }
            
            when (model.downloadState) {
                is ModelDownloadState.Downloading -> {
                    IconButton(
                        onClick = onPauseResume,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color(0xFFC7C4D7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                is ModelDownloadState.Paused -> {
                    IconButton(
                        onClick = onPauseResume,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = Color(0xFFC7C4D7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
