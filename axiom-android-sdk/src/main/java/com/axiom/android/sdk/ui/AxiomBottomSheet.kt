package com.axiom.android.sdk.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.axiom.android.sdk.AxiomState
import com.axiom.android.sdk.AxiomSDK
import com.axiom.android.sdk.ui.components.chat.ChatInputField
import com.axiom.android.sdk.ui.components.chat.ChatMessageItem
import com.axiom.android.sdk.ui.components.chat.ChatMessage
import com.axiom.android.sdk.ui.components.chat.StopGenerationButton
import com.axiom.android.sdk.ui.theme.AxiomTheme
import kotlinx.coroutines.launch

/**
 * Configuration for AxiomBottomSheet
 */
data class AxiomUIConfig(
    val collapsedHeight: Float = 60f,
    val expandedHeight: Float = 500f
)

/**
 * Main AxiomBottomSheet component
 * Provides a draggable bottom sheet with collapsed (floating pill) and expanded (chat UI) states
 * @param modifier Modifier for the component
 * @param config Configuration for the bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxiomBottomSheet(
    modifier: Modifier = Modifier,
    config: AxiomUIConfig = AxiomUIConfig()
) {
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false
        )
    )
    
    val sdkState by AxiomSDK.observeState().collectAsState()
    val isGenerating = sdkState == AxiomState.Generating
    
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val inputText = remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    // Handle drag gestures for custom drag behavior
    val dragOffset = remember { mutableStateOf(0f) }
    
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
                                CircleShape
                            )
                    )
                }
                
                // Chat messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageItem(message = message)
                    }
                }
                
                // Stop generation button (visible only during generation)
                if (isGenerating) {
                    StopGenerationButton(
                        onClick = {
                            AxiomSDK.getEngine().cancel()
                        }
                    )
                }
                
                // Chat input
                ChatInputField(
                    text = inputText.value,
                    onTextChange = { inputText.value = it },
                    onSend = {
                        if (inputText.value.isNotBlank()) {
                            val userMessage = ChatMessage(
                                id = System.currentTimeMillis().toString(),
                                content = inputText.value,
                                isUser = true
                            )
                            messages.add(userMessage)
                            inputText.value = ""
                            
                            // Generate AI response
                            scope.launch {
                                try {
                                    val engine = AxiomSDK.getEngine()
                                    val aiMessage = ChatMessage(
                                        id = (System.currentTimeMillis() + 1).toString(),
                                        content = "",
                                        isUser = false
                                    )
                                    messages.add(aiMessage)
                                    
                                    engine.generate(userMessage.content).collect { token ->
                                        val lastIndex = messages.lastIndex
                                        messages[lastIndex] = messages[lastIndex].copy(
                                            content = messages[lastIndex].content + token
                                        )
                                    }
                                } catch (e: Exception) {
                                    val errorMessage = ChatMessage(
                                        id = System.currentTimeMillis().toString(),
                                        content = "Error: ${e.message}",
                                        isUser = false
                                    )
                                    messages.add(errorMessage)
                                }
                            }
                        }
                    },
                    enabled = !isGenerating
                )
            }
        },
        sheetPeekHeight = config.collapsedHeight.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, _ ->
                                scope.launch {
                                    sheetState.bottomSheetState.expand()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Open Chat",
                        tint = AxiomTheme.colors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
