package com.axiom.android.sdk.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for Axiom SDK
 * Lumina Neural Design System
 */
data class AxiomColors(
    val primary: Color,           // Accent (Primary): Indigo #6366F1
    val accentLuminous: Color,    // Accent (Luminous): Lavender #c0c1ff
    val background: Color,        // Surface (Primary): #13131b
    val backgroundSecondary: Color, // Surface (Secondary): #1b1b23
    val backgroundTertiary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,           // Success: Emerald #10b981
    val error: Color,             // Error: Rose #f43f5e
    val divider: Color
)

/**
 * Default color scheme for Axiom SDK
 * Lumina Neural Design System
 */
val defaultAxiomColors = AxiomColors(
    primary = Color(0xFF6366F1),
    accentLuminous = Color(0xFFC0C1FF),
    background = Color(0xFF13131B),
    backgroundSecondary = Color(0xFF1B1B23),
    backgroundTertiary = Color(0xFF292932),
    textPrimary = Color(0xFFE4E1ED),
    textSecondary = Color(0xFFC7C4D7),
    success = Color(0xFF10B981),
    error = Color(0xFFF43F5E),
    divider = Color(0xFF464654)
)
