package com.axiom.android.sdk.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Chat message item component
 * @param message Chat message to display
 * @param modifier Modifier for the component
 */
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (message.isUser) {
                        AxiomTheme.colors.primary
                    } else {
                        AxiomTheme.colors.backgroundTertiary
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                color = if (message.isUser) {
                    AxiomTheme.colors.textPrimary
                } else {
                    AxiomTheme.colors.textPrimary
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
