package com.axiom.android.sdk.ui.theme

/**
 * Design system for Axiom SDK
 * Combines colors, typography, and shapes
 */
data class AxiomDesignSystem(
    val colors: AxiomColors = defaultAxiomColors,
    val typography: AxiomTypography = defaultAxiomTypography,
    val shapes: AxiomShapes = defaultAxiomShapes
)

/**
 * Default design system for Axiom SDK
 */
val defaultDesignSystem = AxiomDesignSystem()
