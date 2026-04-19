package com.axiom.android.sdk.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography system for Axiom SDK
 * Lumina Neural Design System
 */
data class AxiomTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val title: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
    val label: TextStyle
)

/**
 * Default typography for Axiom SDK
 * Lumina Neural Design System
 * Headings: Space Grotesk (Bold/Medium) - Technical, modern, high-impact
 * Body & UI: Inter - Optimized for readability and clarity
 */
val defaultAxiomTypography = AxiomTypography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default, // TODO: Replace with Space Grotesk
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.75).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default, // TODO: Replace with Space Grotesk
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp
    ),
    title = TextStyle(
        fontFamily = FontFamily.Default, // TODO: Replace with Space Grotesk
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = (-0.25).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, // TODO: Replace with Inter
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default, // TODO: Replace with Inter
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default, // TODO: Replace with Inter
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.Default, // TODO: Replace with Inter
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.sp
    ),
    label = TextStyle(
        fontFamily = FontFamily.Default, // TODO: Replace with Inter
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.sp
    )
)
