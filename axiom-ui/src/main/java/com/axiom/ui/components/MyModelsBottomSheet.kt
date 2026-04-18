package com.axiom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axiom.ui.model.ModelUIItem

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
                .background(Color(0xFF13131B))
        )
        
        // Bottom sheet
        BottomSheetScaffold(
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(20.dp)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .background(Color(0xFF464654).copy(alpha = 0.3f), CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(17.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFC0C1FF),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "The Atelier",
                                color = Color(0xFFC0C1FF),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = (-0.75).sp
                            )
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFFC0C1FF),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Model list
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        models.forEach { model ->
                            ModelCard(
                                model = model,
                                onPauseResume = { onModelPauseResume(model.id) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onImportLocalModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC0C1FF)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            )
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
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .height(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Settings icon placeholder
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(2.dp)
                                            .background(Color(0xFFC7C4D7))
                                    )
                                }
                                Text(
                                    text = "Optimize Model Storage",
                                    color = Color(0xFFC7C4D7),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
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
