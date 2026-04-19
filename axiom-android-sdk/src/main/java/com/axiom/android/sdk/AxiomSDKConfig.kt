package com.axiom.android.sdk

import android.content.Context

/**
 * Configuration for AxiomSDK initialization
 */
data class AxiomSDKConfig(
    val context: Context,
    val mode: AxiomMode = AxiomMode.Managed,
    val modelsDir: String? = null,
    val enableLogging: Boolean = true,
    val defaultModelId: String? = null,
    val maxCacheSize: Long = 512 * 1024 * 1024L // 512MB default
)
