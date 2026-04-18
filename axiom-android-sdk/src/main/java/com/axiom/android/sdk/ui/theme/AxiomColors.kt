package com.axiom.android.sdk.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for Axiom SDK
 * Extracted from existing UI components
 */
data class AxiomColors(
    val primary: Color,
    val background: Color,
    val backgroundSecondary: Color,
    val backgroundTertiary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val error: Color,
    val divider: Color
)

/**
 * Default color scheme for Axiom SDK
 */
val defaultAxiomColors = AxiomColors(
    primary = Color(0xFFC0C1FF),
    background = Color(0xFF13131B),
    backgroundSecondary = Color(0xFF1B1B23),
    backgroundTertiary = Color(0xFF292932),
    textPrimary = Color(0xFFE4E1ED),
    textSecondary = Color(0xFFC7C4D7),
    success = Color(0xFF4EDea3),
    error = Color(0xFFFF6B6B),
    divider = Color(0xFF464654)
)
