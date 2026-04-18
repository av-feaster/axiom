package com.axiom.ui.model

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
