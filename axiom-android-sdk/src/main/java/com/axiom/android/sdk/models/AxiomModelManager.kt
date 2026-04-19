package com.axiom.android.sdk.models

import com.axiom.android.sdk.domain.ActiveDownloadInfo
import com.axiom.android.sdk.domain.DownloadState
import com.axiom.android.sdk.domain.ModelUIItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Public interface for model management
 * Wraps the internal ModelManager to provide a clean SDK API
 */
interface AxiomModelManager {
    /**
     * Download a model by ID
     * @param modelId ID of the model to download
     */
    fun download(modelId: String)
    
    /**
     * Pause the current download
     */
    fun pause()
    
    /**
     * Resume a paused download
     */
    fun resume()
    
    /**
     * Cancel the current download
     */
    fun cancel()
    
    /**
     * Observe download progress as Flow
     * @return Flow of download state updates
     */
    fun observeProgress(): Flow<DownloadState>

    /**
     * Emits the current in-flight download (model id + [DownloadState]) for UI that should update on every progress tick.
     * Null when idle or no active session.
     */
    fun getActiveDownloadFlow(): StateFlow<ActiveDownloadInfo?>
    
    /**
     * Get list of installed models
     * @return List of installed model UI items
     */
    fun getInstalledModels(): List<ModelUIItem>
    
    /**
     * Get list of available models from registry
     * @return List of available model UI items (including both installed and not installed)
     */
    suspend fun getAvailableModels(): List<ModelUIItem>
    
    /**
     * Delete a model by ID
     * @param modelId ID of the model to delete
     * @return true if deletion succeeded
     */
    suspend fun delete(modelId: String): Boolean
}
