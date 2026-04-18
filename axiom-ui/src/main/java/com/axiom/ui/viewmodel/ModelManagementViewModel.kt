package com.axiom.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.axiom.core.Model
import com.axiom.core.ModelManager
import com.axiom.ui.model.ModelDownloadState
import com.axiom.ui.model.ModelUIItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelManagementViewModel(
    private val modelManager: ModelManager
) : ViewModel() {
    
    companion object {
        fun Factory(modelManager: ModelManager): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ModelManagementViewModel::class.java)) {
                        return ModelManagementViewModel(modelManager) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
    
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
                val registry = modelManager.fetchRegistry()
                val modelUIItems = registry.map { model ->
                    ModelUIItem(
                        id = model.id,
                        name = model.name,
                        description = model.description,
                        size = formatSize(model.size),
                        version = "1.0",
                        downloadState = ModelDownloadState.NotStarted
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
                val model = _models.value.find { it.id == modelId } ?: return@launch
                updateModelState(modelId, ModelDownloadState.Downloading(0f, "0 MB/s", 0L, 0L))
                
                // TODO: Integrate with actual download from modelManager
                // This is a placeholder for the actual implementation
                // modelManager.downloadModel(modelId, progressCallback)
                
            } catch (e: Exception) {
                updateModelState(modelId, ModelDownloadState.Failed(e.message ?: "Download failed"))
            }
        }
    }
    
    fun pauseDownload(modelId: String) {
        viewModelScope.launch {
            val currentModel = _models.value.find { it.id == modelId }
            if (currentModel?.downloadState is ModelDownloadState.Downloading) {
                val state = currentModel.downloadState as ModelDownloadState.Downloading
                updateModelState(modelId, ModelDownloadState.Paused(state.progress, state.downloadedBytes, state.totalBytes))
            }
        }
    }
    
    fun resumeDownload(modelId: String) {
        viewModelScope.launch {
            val currentModel = _models.value.find { it.id == modelId }
            if (currentModel?.downloadState is ModelDownloadState.Paused) {
                val state = currentModel.downloadState as ModelDownloadState.Paused
                updateModelState(modelId, ModelDownloadState.Downloading(state.progress, "0 MB/s", state.downloadedBytes, state.totalBytes))
                // TODO: Resume actual download
            }
        }
    }
    
    fun cancelDownload(modelId: String) {
        viewModelScope.launch {
            updateModelState(modelId, ModelDownloadState.NotStarted)
            // TODO: Cancel actual download
        }
    }
    
    fun updateModel(modelId: String) {
        viewModelScope.launch {
            updateModelState(modelId, ModelDownloadState.Updating(0f, "0 MB/s", "Checking for updates..."))
            // TODO: Implement model update logic
        }
    }
    
    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            // TODO: Implement delete model logic when ModelManager supports it
            updateModelState(modelId, ModelDownloadState.NotStarted)
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
