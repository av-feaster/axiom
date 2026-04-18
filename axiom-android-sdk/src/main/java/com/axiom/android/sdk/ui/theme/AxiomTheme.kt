package com.axiom.android.sdk.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal for AxiomDesignSystem
 */
val LocalAxiomDesignSystem = staticCompositionLocalOf<AxiomDesignSystem> {
    error("No AxiomDesignSystem provided")
}

/**
 * Axiom Theme - Provides design system to the composition
 * @param designSystem Custom design system (uses default if not provided)
 * @param content Content to be wrapped with the theme
 */
@Composable
fun AxiomTheme(
    designSystem: AxiomDesignSystem = defaultDesignSystem,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAxiomDesignSystem provides designSystem,
        content = content
    )
}

/**
 * Object to access the current design system
 */
object AxiomTheme {
    val colors: AxiomColors
        @Composable
        get() = LocalAxiomDesignSystem.current.colors

    val typography: AxiomTypography
        @Composable
        get() = LocalAxiomDesignSystem.current.typography

    val shapes: AxiomShapes
        @Composable
        get() = LocalAxiomDesignSystem.current.shapes
}
