package com.axiom.android.sdk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axiom.android.sdk.domain.ModelDownloadState
import com.axiom.android.sdk.domain.ModelUIItem
import com.axiom.android.sdk.models.AxiomModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Internal ViewModel for model management
 * Depends on AxiomModelManager (not ModelManager) to maintain clean architecture
 */
internal class ModelManagementViewModel(
    private val modelManager: AxiomModelManager
) : ViewModel() {
    
    private val _models = MutableStateFlow<List<ModelUIItem>>(emptyList())
    val models: StateFlow<List<ModelUIItem>> = _models.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(BottomNavItem.MyModels)
    val selectedTab: StateFlow<BottomNavItem> = _selectedTab.asStateFlow()
    
    init {
        loadModels()
    }
    
    private fun loadModels() {
        viewModelScope.launch {
            try {
                val installedModels = modelManager.getInstalledModels()
                val modelUIItems = installedModels.map { model ->
                    ModelUIItem(
                        id = model.id,
                        name = model.name,
                        description = model.description,
                        size = formatSize(model.size),
                        version = "1.0",
                        downloadState = ModelDownloadState.Installed
                    )
                }
                _models.value = modelUIItems
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            try {
                updateModelState(modelId, ModelDownloadState.Downloading(0f, "0 MB/s", 0L, 0L))
                modelManager.download(modelId)
                
                // Observe download progress
                modelManager.observeProgress().collect { state ->
                    when (state) {
                        is com.axiom.android.sdk.domain.DownloadState.Downloading -> {
                            updateModelState(modelId, ModelDownloadState.Downloading(
                                state.progress,
                                "0 MB/s",
                                state.downloadedBytes,
                                state.totalBytes
                            ))
                        }
                        is com.axiom.android.sdk.domain.DownloadState.Completed -> {
                            updateModelState(modelId, ModelDownloadState.Installed)
                        }
                        is com.axiom.android.sdk.domain.DownloadState.Failed -> {
                            updateModelState(modelId, ModelDownloadState.Failed(state.error))
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                updateModelState(modelId, ModelDownloadState.Failed(e.message ?: "Download failed"))
            }
        }
    }
    
    fun pauseDownload(modelId: String) {
        modelManager.pause()
    }
    
    fun resumeDownload(modelId: String) {
        modelManager.resume()
    }
    
    fun cancelDownload(modelId: String) {
        modelManager.cancel()
        updateModelState(modelId, ModelDownloadState.NotStarted)
    }
    
    fun updateModel(modelId: String) {
        viewModelScope.launch {
            updateModelState(modelId, ModelDownloadState.Updating(0f, "0 MB/s", "Checking for updates..."))
            // TODO: Implement model update logic
        }
    }
    
    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            try {
                // For now, just remove from UI since we don't have Model objects
                _models.value = _models.value.filter { it.id != modelId }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun selectTab(tab: BottomNavItem) {
        _selectedTab.value = tab
    }
    
    private fun updateModelState(modelId: String, newState: ModelDownloadState) {
        _models.value = _models.value.map { model ->
            if (model.id == modelId) {
                model.copy(downloadState = newState)
            } else {
                model
            }
        }
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}

enum class BottomNavItem {
    Store,
    Downloads,
    MyModels
}
