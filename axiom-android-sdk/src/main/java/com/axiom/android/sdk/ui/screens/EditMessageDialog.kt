package com.axiom.android.sdk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Dialog for editing a chat message
 */
@Composable
fun EditMessageDialog(
    originalContent: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editedContent by remember { mutableStateOf(originalContent) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Message",
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("Edit your message...") },
                    singleLine = false,
                    maxLines = 10
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(editedContent) },
                        enabled = editedContent.isNotBlank()
                    ) {
                        Text("Save & Regenerate")
                    }
                }
            }
        }
    }
}
