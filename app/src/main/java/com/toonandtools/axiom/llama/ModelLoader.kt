package com.toonandtools.axiom.llama

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ModelLoader {
    
    /**
     * Prepare model file by copying from assets to internal storage
     * @param context Android context
     * @param assetName Name of the model file in assets
     * @return Absolute path to the model file in internal storage
     */
    fun prepare(context: Context, assetName: String): String {
        val modelFile = File(context.filesDir, assetName)
        
        // If model already exists, return its path
        if (modelFile.exists()) {
            android.util.Log.d("ModelLoader", "Model already exists at: ${modelFile.absolutePath}")
            return modelFile.absolutePath
        }
        
        // Copy model from assets to internal storage
        try {
            context.assets.open("models/$assetName").use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            android.util.Log.d("ModelLoader", "Model copied to: ${modelFile.absolutePath}")
            return modelFile.absolutePath
        } catch (e: IOException) {
            android.util.Log.e("ModelLoader", "Failed to copy model from assets", e)
            throw RuntimeException("Failed to prepare model: ${e.message}", e)
        }
    }
    
    /**
     * Get the size of the model file in internal storage
     * @param context Android context
     * @param assetName Name of the model file
     * @return Size in bytes, or -1 if file doesn't exist
     */
    fun getModelSize(context: Context, assetName: String): Long {
        val modelFile = File(context.filesDir, assetName)
        return if (modelFile.exists()) modelFile.length() else -1
    }
    
    /**
     * Check if model exists in internal storage
     * @param context Android context
     * @param assetName Name of the model file
     * @return true if model exists, false otherwise
     */
    fun modelExists(context: Context, assetName: String): Boolean {
        return File(context.filesDir, assetName).exists()
    }
    
    /**
     * Delete model from internal storage
     * @param context Android context
     * @param assetName Name of the model file
     * @return true if deletion successful, false otherwise
     */
    fun deleteModel(context: Context, assetName: String): Boolean {
        val modelFile = File(context.filesDir, assetName)
        return if (modelFile.exists()) {
            val deleted = modelFile.delete()
            if (deleted) {
                android.util.Log.d("ModelLoader", "Model deleted: ${modelFile.absolutePath}")
            } else {
                android.util.Log.w("ModelLoader", "Failed to delete model: ${modelFile.absolutePath}")
            }
            deleted
        } else {
            true // Already doesn't exist
        }
    }
}
