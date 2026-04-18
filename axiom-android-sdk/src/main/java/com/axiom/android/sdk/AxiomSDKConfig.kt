package com.axiom.android.sdk

import android.content.Context

/**
 * Configuration for AxiomSDK initialization
 */
data class AxiomSDKConfig(
    val context: Context,
    val enableLogging: Boolean = true,
    val maxCacheSize: Long = 512 * 1024 * 1024L // 512MB default
)
