package com.axiom.android.sdk.models

import com.axiom.core.Model
import com.axiom.android.sdk.domain.DownloadState
import kotlinx.coroutines.flow.Flow

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
     * Get list of installed models
     * @return List of installed models
     */
    fun getInstalledModels(): List<Model>
    
    /**
     * Delete a model
     * @param model Model to delete
     * @return true if deletion succeeded
     */
    suspend fun delete(model: Model): Boolean
}
