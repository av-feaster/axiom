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

/**
 * Active HTTP download session for a single registry model.
 * [progressSnapshot] holds the last [DownloadState.Downloading] metrics and is used when [state] is [DownloadState.Paused].
 */
data class ActiveDownloadInfo(
    val modelId: String,
    val state: DownloadState,
    val progressSnapshot: Triple<Float, Long, Long>? = null
)

fun ActiveDownloadInfo.toModelDownloadState(): ModelDownloadState {
    return when (val s = state) {
        is DownloadState.Downloading ->
            ModelDownloadState.Downloading(s.progress, "—", s.downloadedBytes, s.totalBytes)
        is DownloadState.Paused -> {
            val snap = progressSnapshot ?: Triple(0f, 0L, 0L)
            ModelDownloadState.Paused(snap.first, snap.second, snap.third)
        }
        is DownloadState.Failed -> ModelDownloadState.Failed(s.error)
        is DownloadState.Completed -> ModelDownloadState.Installed
        is DownloadState.Idle -> ModelDownloadState.NotStarted
    }
}

fun List<ModelUIItem>.withActiveDownload(active: ActiveDownloadInfo?): List<ModelUIItem> {
    if (active == null) return this
    return map { item ->
        if (item.id != active.modelId) item
        else item.copy(downloadState = active.toModelDownloadState())
    }
}
