package com.axiom.android.sdk.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Shape system for Axiom SDK
 * Lumina Neural Design System
 */
data class AxiomShapes(
    val small: RoundedCornerShape,
    val medium: RoundedCornerShape,
    val large: RoundedCornerShape,
    val extraLarge: RoundedCornerShape,
    val full: RoundedCornerShape,
    val circle: RoundedCornerShape
)

/**
 * Default shapes for Axiom SDK
 * Lumina Neural Design System - ROUND_EIGHT (8px to 12px)
 */
val defaultAxiomShapes = AxiomShapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
    full = RoundedCornerShape(999.dp),
    circle = RoundedCornerShape(50) // Approximates a circle
)
