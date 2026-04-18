package com.axiom.android.sdk.domain

/**
 * Download state for the domain layer (used by ModelManagerWrapper)
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState()
    object Paused : DownloadState()
    data class Completed(val downloadedPath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

/**
 * Download state for UI layer (used by ModelUIItem)
 */
sealed class ModelDownloadState {
    data object NotStarted : ModelDownloadState()
    data class Downloading(
        val progress: Float,
        val downloadSpeed: String,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : ModelDownloadState()
    data class Paused(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : ModelDownloadState()
    data class Updating(
        val progress: Float,
        val downloadSpeed: String,
        val updateMessage: String
    ) : ModelDownloadState()
    data object Installed : ModelDownloadState()
    data class Failed(val error: String) : ModelDownloadState()
}

data class ModelUIItem(
    val id: String,
    val name: String,
    val description: String,
    val size: String,
    val version: String,
    val downloadState: ModelDownloadState,
    val imageUrl: String? = null
)
