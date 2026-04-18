package com.axiom.core

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Interface for model management (download, list, delete)
 */
interface ModelManager {
    /**
     * Fetch the model registry from remote
     */
    suspend fun fetchRegistry(): List<Model>
    
    /**
     * Download a model
     * @param model Model to download
     * @param progressCallback Callback for download progress (0.0 to 1.0)
     */
    suspend fun download(model: Model, progressCallback: ((Float) -> Unit)? = null): String
    
    /**
     * Get list of installed models
     */
    fun getInstalledModels(): List<Model>
    
    /**
     * Delete a model
     */
    suspend fun delete(model: Model): Boolean
    
    /**
     * Get recommended model for device
     */
    fun recommend(context: Context): Model?
    
    /**
     * Stream download progress as Flow
     */
    fun downloadProgress(model: Model): Flow<Float>
}
