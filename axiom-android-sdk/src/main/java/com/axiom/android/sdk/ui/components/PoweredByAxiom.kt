package com.axiom.android.sdk.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.axiom.android.sdk.ui.theme.AxiomTheme

/**
 * Display mode for PoweredByAxiom branding
 */
enum class BrandingMode {
    TEXT_ONLY,
    TEXT_AND_IMAGE,
    IMAGE_ONLY
}

/**
 * Reusable component for "Powered by Axiom AI" branding
 * Can be used across SDK UI components for consistent branding
 */
@Composable
fun PoweredByAxiom(
    mode: BrandingMode = BrandingMode.TEXT_ONLY,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (mode) {
            BrandingMode.TEXT_ONLY -> {
                Text(
                    text = "Powered by Axiom AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = AxiomTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
            BrandingMode.TEXT_AND_IMAGE -> {
                Text(
                    text = "Powered by",
                    style = MaterialTheme.typography.bodySmall,
                    color = AxiomTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Axiom AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = AxiomTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
            BrandingMode.IMAGE_ONLY -> {
                // Icon would be loaded from resources
                // For now, showing text as placeholder
                Text(
                    text = "Axiom AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = AxiomTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
