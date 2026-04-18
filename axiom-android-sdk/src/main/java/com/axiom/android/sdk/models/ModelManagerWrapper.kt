package com.axiom.android.sdk.models

import android.content.Context
import com.axiom.android.sdk.AxiomSDK
import com.axiom.android.sdk.AxiomState
import com.axiom.core.Model
import com.axiom.core.ModelManager
import com.axiom.android.sdk.domain.DownloadState
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
class ModelManagerWrapper(private val context: Context) : AxiomModelManager {
    
    private val internalModelManager: ModelManager = DefaultModelManager(context)
    private var downloadJob: Job? = null
    private var currentModelId: String? = null
    private var isPaused = false
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState
    
    override fun download(modelId: String) {
        if (downloadJob?.isActive == true) {
            cancel()
        }

        currentModelId = modelId
        isPaused = false

        downloadJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val registry = internalModelManager.fetchRegistry()
                val model = registry.find { it.id == modelId }

                if (model == null) {
                    _downloadState.value = DownloadState.Failed("Model not found: $modelId")
                    AxiomSDK.updateState(AxiomState.Error("Model not found: $modelId"))
                    return@launch
                }

                _downloadState.value = DownloadState.Downloading(0f, 0L, model.size)
                AxiomSDK.updateState(AxiomState.Downloading(0f))

                val downloadedPath = internalModelManager.download(model) { progress ->
                    _downloadState.value = DownloadState.Downloading(progress, (progress * model.size).toLong(), model.size)
                    AxiomSDK.updateState(AxiomState.Downloading(progress))
                }

                _downloadState.value = DownloadState.Completed(downloadedPath)
                AxiomSDK.updateState(AxiomState.Ready)
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Failed(e.message ?: "Download failed")
                AxiomSDK.updateState(AxiomState.Error(e.message ?: "Download failed"))
            }
        }
    }
    
    override fun pause() {
        if (downloadJob?.isActive == true) {
            isPaused = true
            downloadJob?.cancel()
            _downloadState.value = DownloadState.Paused
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
        _downloadState.value = DownloadState.Idle
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
    
    override fun getInstalledModels(): List<Model> {
        return internalModelManager.getInstalledModels()
    }
    
    override suspend fun delete(model: Model): Boolean = withContext(Dispatchers.IO) {
        internalModelManager.delete(model)
    }
}
