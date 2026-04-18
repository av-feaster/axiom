package com.axiom.core

/**
 * Represents an AI model with metadata
 */
data class Model(
    val id: String,
    val name: String,
    val description: String,
    val size: Long,
    val downloadUrl: String,
    val checksum: String,
    val architecture: String,
    val quantization: String,
    val minRam: Long,
    val recommended: Boolean = false
)
