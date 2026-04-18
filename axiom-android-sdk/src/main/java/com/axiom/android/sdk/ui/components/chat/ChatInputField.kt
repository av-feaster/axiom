package com.axiom.android.sdk.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.axiom.android.sdk.ui.theme.AxiomTheme

/**
 * Chat input field component
 * @param text Current input text
 * @param onTextChange Callback when text changes
 * @param onSend Callback when send button is clicked
 * @param enabled Whether the input is enabled
 * @param modifier Modifier for the component
 */
@Composable
fun ChatInputField(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Type a message...",
                    color = AxiomTheme.colors.textSecondary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AxiomTheme.colors.primary,
                unfocusedBorderColor = AxiomTheme.colors.divider,
                cursorColor = AxiomTheme.colors.primary,
                focusedTextColor = AxiomTheme.colors.textPrimary,
                unfocusedTextColor = AxiomTheme.colors.textPrimary
            ),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4,
            enabled = enabled
        )
        
        IconButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (enabled && text.isNotBlank()) {
                    AxiomTheme.colors.primary
                } else {
                    AxiomTheme.colors.divider
                }
            )
        }
    }
}
