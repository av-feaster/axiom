package com.axiom.android.sdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.axiom.android.sdk.AxiomMode
import com.axiom.android.sdk.AxiomSDK
import com.axiom.android.sdk.ui.screens.ChatScreen
import com.axiom.android.sdk.ui.screens.ModelHubScreen
import com.axiom.android.sdk.ui.theme.AxiomTheme
import kotlinx.coroutines.launch

/**
 * Main AxiomBottomSheet component - Single UI entry point for the SDK
 * 
 * Behavior:
 * - IF config.mode == Headless: Renders NOTHING (UI not available in headless mode)
 * - IF config.mode == Managed:
 *   - If no model selected: Shows ModelHubScreen
 *   - If model selected: Shows ChatScreen
 *   - If config.defaultModelId != null: Skips ModelHub, opens ChatScreen directly
 * 
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxiomBottomSheet(
    modifier: Modifier = Modifier
) {
    val config = AxiomSDK.getConfig()
    
    // Headless mode: render nothing
    if (config.mode == AxiomMode.Headless) {
        return
    }
    
    // Managed mode: show UI based on state
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false
        )
    )
    
    val selectedModelId = AxiomUIState.selectedModelId
    val defaultModelId = config.defaultModelId
    
    // Determine which screen to show
    val shouldShowChat = selectedModelId != null || defaultModelId != null
    val currentModelId = selectedModelId ?: defaultModelId
    
    val scope = rememberCoroutineScope()
    
    BottomSheetScaffold(
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(AxiomTheme.colors.background)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                AxiomTheme.colors.divider.copy(alpha = 0.3f),
                                AxiomTheme.shapes.circle
                            )
                    )
                }
                
                // State machine: Show ModelHub or Chat
                if (shouldShowChat && currentModelId != null) {
                    ChatScreen(
                        modelId = currentModelId,
                        onNavigateBack = {
                            AxiomUIState.selectedModelId = null
                        }
                    )
                } else {
                    ModelHubScreen(
                        onModelSelected = { modelId ->
                            AxiomUIState.selectedModelId = modelId
                        },
                        onImportModel = {
                            // Handle import model action
                        }
                    )
                }
            }
        },
        sheetPeekHeight = 80.dp,
        sheetShape = AxiomTheme.shapes.extraLarge,
        containerColor = Color.Transparent,
        scaffoldState = sheetState,
        modifier = modifier
    ) {
        // Collapsed state - floating pill overlay
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Floating pill (visible when collapsed)
            if (sheetState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(
                            AxiomTheme.colors.primary,
                            AxiomTheme.shapes.circle
                        )
                        .size(56.dp)
                        .clickable {
                            scope.launch {
                                sheetState.bottomSheetState.expand()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Open Chat",
                        tint = AxiomTheme.colors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
