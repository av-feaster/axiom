package com.toonandtools.axiom.llama

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LlamaEngine {
    private var isInitialized = false
    
    init {
        System.loadLibrary("llama_jni")
    }
    
    /**
     * Initialize the llama.cpp engine with a model
     * @param context Android context for accessing assets
     * @param modelAssetName Name of the model file in assets (e.g., "tinyllama.gguf")
     * @return true if initialization successful, false otherwise
     */
    fun init(context: Context, modelAssetName: String): Boolean {
        if (isInitialized) {
            return true
        }
        
        return try {
            val modelPath = ModelLoader.prepare(context, modelAssetName)
            isInitialized = nativeInit(modelPath)
            isInitialized
        } catch (e: Exception) {
            android.util.Log.e("LlamaEngine", "Failed to initialize", e)
            false
        }
    }
    
    /**
     * Generate text using the loaded model
     * @param prompt Input prompt for generation
     * @return Generated text or error message
     */
    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext "Error: LlamaEngine not initialized"
        }
        
        try {
            nativeGenerate(prompt)
        } catch (e: Exception) {
            android.util.Log.e("LlamaEngine", "Generation failed", e)
            "Error: Generation failed - ${e.message}"
        }
    }
    
    /**
     * Check if the engine is initialized
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Clean up native resources
     */
    fun cleanup() {
        if (isInitialized) {
            try {
                nativeCleanup()
                isInitialized = false
            } catch (e: Exception) {
                android.util.Log.e("LlamaEngine", "Cleanup failed", e)
            }
        }
    }
    
    // Native methods
    private external fun nativeInit(modelPath: String): Boolean
    private external fun nativeGenerate(prompt: String): String
    private external fun nativeCleanup()
}
