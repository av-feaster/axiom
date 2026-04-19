package com.axiom.android.sdk.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axiom.android.sdk.ui.theme.AxiomTheme

/**
 * Data class representing a chat message
 */
data class ChatMessage(
    val role: String = "user",
    val content: String,
    val tokenCount: Int = 0,
    val generationTimeMs: Long = 0,
    val isStreaming: Boolean = false
)

/**
 * Chat message item component
 * @param message Chat message to display
 * @param onRegenerate Callback for regenerating assistant messages
 * @param onEdit Callback for editing user messages
 * @param onCopy Callback for copying message content
 * @param modifier Modifier for the component
 */
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onRegenerate: () -> Unit = {},
    onEdit: () -> Unit = {},
    onCopy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (message.role == "user") Alignment.End else Alignment.Start
        ) {
            // Message bubble (only render if content is not blank)
            if (message.content.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(
                            color = if (message.role == "user") {
                                AxiomTheme.colors.primary
                            } else {
                                AxiomTheme.colors.backgroundTertiary
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.content.take(10000),
                        color = if (message.role == "user") {
                            AxiomTheme.colors.textPrimary
                        } else {
                            AxiomTheme.colors.textPrimary
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 100,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (message.role == "assistant") {
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                if (message.role == "user") {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
