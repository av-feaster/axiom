package com.axiom.android.sdk

import android.app.Application
import com.axiom.android.sdk.engine.AxiomEngine
import com.axiom.android.sdk.engine.EngineManager
import com.axiom.android.sdk.models.AxiomModelManager
import com.axiom.android.sdk.models.ModelManagerWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main entry point for the Axiom Android SDK
 * Singleton that manages the SDK lifecycle and provides access to engine and model manager
 */
object AxiomSDK {
    private var isInitialized = false
    private lateinit var engineManager: EngineManager
    private lateinit var modelManagerWrapper: ModelManagerWrapper
    private val _state = MutableStateFlow<AxiomState>(AxiomState.Idle)
    val state: StateFlow<AxiomState> = _state.asStateFlow()

    /**
     * Initialize the SDK with application context and configuration
     * @param application Application context
     * @param config SDK configuration
     * @throws IllegalStateException if SDK is already initialized
     */
    fun initialize(
        application: Application,
        config: AxiomSDKConfig
    ) {
        if (isInitialized) {
            throw IllegalStateException("AxiomSDK already initialized")
        }
        
        // Initialize managers (will be replaced with Hilt injection in later phases)
        engineManager = EngineManager()
        modelManagerWrapper = ModelManagerWrapper(application)
        
        isInitialized = true
    }

    /**
     * Get the engine instance for text generation
     * @throws IllegalStateException if SDK is not initialized
     */
    fun getEngine(): AxiomEngine {
        checkInitialized()
        return engineManager
    }

    /**
     * Get the model manager instance for model operations
     * @throws IllegalStateException if SDK is not initialized
     */
    fun getModelManager(): AxiomModelManager {
        checkInitialized()
        return modelManagerWrapper
    }

    /**
     * Observe the global SDK state
     * @throws IllegalStateException if SDK is not initialized
     */
    fun observeState(): StateFlow<AxiomState> {
        checkInitialized()
        return state
    }

    /**
     * Update the global SDK state (internal use)
     */
    internal fun updateState(newState: AxiomState) {
        _state.value = newState
    }

    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("AxiomSDK not initialized. Call initialize() first.")
        }
    }
}
