package com.axiom.android.sdk.models

import android.app.Application
import com.axiom.android.sdk.AxiomSDKConfig
import com.axiom.android.sdk.AxiomState
import com.axiom.android.sdk.AxiomStateStore
import com.axiom.android.sdk.domain.ActiveDownloadInfo
import com.axiom.android.sdk.domain.DownloadState
import com.axiom.android.sdk.domain.ModelDownloadState
import com.axiom.android.sdk.domain.ModelUIItem
import com.axiom.android.sdk.domain.withActiveDownload
import com.axiom.core.ModelManager
import com.axiom.models.DefaultModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wrapper implementation of AxiomModelManager
 * Manages the internal ModelManager and provides Flow-based download progress
 */
object ModelManagerWrapper : AxiomModelManager {

    private lateinit var internalModelManager: ModelManager
    private var downloadJob: Job? = null
    private var currentModelId: String? = null
    private var isPaused = false
    private lateinit var config: AxiomSDKConfig

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    /** Last [DownloadState.Downloading] metrics; used for [DownloadState.Paused] and [ActiveDownloadInfo]. */
    private var lastProgressSnapshot: Triple<Float, Long, Long>? = null

    private val _activeDownload = MutableStateFlow<ActiveDownloadInfo?>(null)

    /**
     * Initialize the model manager wrapper with application and config
     * @param application Application context
     * @param config SDK configuration
     */
    fun init(application: Application, config: AxiomSDKConfig) {
        this.config = config
        internalModelManager = DefaultModelManager(application)
    }

    /**
     * Get the singleton instance
     * @return ModelManagerWrapper instance
     */
    fun get(): ModelManagerWrapper = this

    override fun getActiveDownloadFlow(): StateFlow<ActiveDownloadInfo?> = _activeDownload

    private fun publishActiveDownload() {
        val id = currentModelId
        val s = _downloadState.value
        _activeDownload.value = when {
            id == null -> null
            s is DownloadState.Idle -> null
            else -> ActiveDownloadInfo(
                modelId = id,
                state = s,
                progressSnapshot = lastProgressSnapshot
            )
        }
    }

    private fun setDownloadState(state: DownloadState) {
        if (state is DownloadState.Downloading) {
            lastProgressSnapshot = Triple(state.progress, state.downloadedBytes, state.totalBytes)
        }
        _downloadState.value = state
        publishActiveDownload()
    }

    override fun download(modelId: String) {
        if (downloadJob?.isActive == true) {
            cancel()
        } else {
            clearTerminalSessionIfNeeded()
        }

        currentModelId = modelId
        isPaused = false
        lastProgressSnapshot = null
        setDownloadState(DownloadState.Downloading(0f, 0L, 0L))
        AxiomStateStore.setState(AxiomState.Downloading(0f))

        downloadJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val registry = internalModelManager.fetchRegistry()
                val model = registry.find { it.id == modelId }

                if (model == null) {
                    setDownloadState(DownloadState.Failed("Model not found: $modelId"))
                    AxiomStateStore.setState(AxiomState.Error("Model not found: $modelId"))
                    return@launch
                }

                setDownloadState(DownloadState.Downloading(0f, 0L, model.size))
                AxiomStateStore.setState(AxiomState.Downloading(0f))

                val downloadedPath = internalModelManager.download(model) { progress ->
                    setDownloadState(
                        DownloadState.Downloading(
                            progress,
                            (progress * model.size).toLong(),
                            model.size
                        )
                    )
                    AxiomStateStore.setState(AxiomState.Downloading(progress))
                }

                setDownloadState(DownloadState.Completed(downloadedPath))
                AxiomStateStore.setState(AxiomState.Ready)
            } catch (e: Exception) {
                setDownloadState(DownloadState.Failed(e.message ?: "Download failed"))
                AxiomStateStore.setState(AxiomState.Error(e.message ?: "Download failed"))
            }
        }
    }

    /** Clears a finished session so a new [download] can start without coalescing [StateFlow] updates. */
    private fun clearTerminalSessionIfNeeded() {
        if (downloadJob?.isActive == true) return
        val s = _downloadState.value
        if (s is DownloadState.Completed || s is DownloadState.Failed) {
            currentModelId = null
            lastProgressSnapshot = null
            _downloadState.value = DownloadState.Idle
            publishActiveDownload()
        }
    }

    override fun pause() {
        if (downloadJob?.isActive == true) {
            isPaused = true
            downloadJob?.cancel()
            setDownloadState(DownloadState.Paused)
        }
    }

    override fun resume() {
        if (isPaused && currentModelId != null) {
            download(currentModelId!!)
        }
    }

    override fun cancel() {
        downloadJob?.cancel()
        downloadJob = null
        currentModelId = null
        isPaused = false
        lastProgressSnapshot = null
        _downloadState.value = DownloadState.Idle
        publishActiveDownload()
    }

    override fun observeProgress(): Flow<DownloadState> = callbackFlow {
        val job = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            _downloadState.collect { state ->
                trySend(state)
            }
        }

        awaitClose {
            job.cancel()
        }
    }.flowOn(Dispatchers.IO)

    override fun getInstalledModels(): List<ModelUIItem> {
        val installedModels = internalModelManager.getInstalledModels()
        return installedModels.map { model ->
            ModelUIItem(
                id = model.id,
                name = model.name,
                description = model.description,
                size = formatFileSize(model.size),
                version = "1.0", // Default version since Model doesn't have version field
                downloadState = ModelDownloadState.Installed
            )
        }
    }

    override suspend fun getAvailableModels(): List<ModelUIItem> = withContext(Dispatchers.IO) {
        val installedModels = internalModelManager.getInstalledModels()
        val registry = internalModelManager.fetchRegistry()

        val base = registry.map { model ->
            val isInstalled = installedModels.any { it.id == model.id }
            ModelUIItem(
                id = model.id,
                name = model.name,
                description = model.description,
                size = formatFileSize(model.size),
                version = "1.0", // Default version since Model doesn't have version field
                downloadState = if (isInstalled) {
                    ModelDownloadState.Installed
                } else {
                    ModelDownloadState.NotStarted
                }
            )
        }
        base.withActiveDownload(_activeDownload.value)
    }

    override suspend fun delete(modelId: String): Boolean = withContext(Dispatchers.IO) {
        val installedModels = internalModelManager.getInstalledModels()
        val model = installedModels.find { it.id == modelId }
        if (model != null) {
            internalModelManager.delete(model)
        } else {
            false
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
