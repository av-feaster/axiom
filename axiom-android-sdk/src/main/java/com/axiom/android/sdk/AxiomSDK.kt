package com.axiom.android.sdk

import android.app.Application
import com.axiom.android.sdk.engine.AxiomEngine
import com.axiom.android.sdk.engine.EngineManager
import com.axiom.android.sdk.models.AxiomModelManager
import com.axiom.android.sdk.models.ModelManagerWrapper
import kotlinx.coroutines.flow.StateFlow

/**
 * Main entry point for the Axiom Android SDK
 * Singleton that manages the SDK lifecycle and provides access to engine and model manager
 */
object AxiomSDK {
    private var initialized = false
    private lateinit var config: AxiomSDKConfig

    /**
     * Initialize the SDK with application context and configuration
     * @param application Application context
     * @param config SDK configuration
     */
    fun initialize(application: Application, config: AxiomSDKConfig) {
        if (initialized) return
        this.config = config

        // init engine + model manager wrappers
        EngineManager.init(application, config)
        ModelManagerWrapper.init(application, config)

        initialized = true
    }

    /**
     * Get the engine instance for text generation
     * @throws IllegalStateException if SDK is not initialized
     */
    fun getEngine(): AxiomEngine = EngineManager.get()

    /**
     * Get the model manager instance for model operations
     * @throws IllegalStateException if SDK is not initialized
     */
    fun getModelManager(): AxiomModelManager = ModelManagerWrapper.get()

    /**
     * Observe the global SDK state
     * @throws IllegalStateException if SDK is not initialized
     */
    fun observeState(): StateFlow<AxiomState> = AxiomStateStore.state

    /**
     * Get the SDK configuration
     * @throws IllegalStateException if SDK is not initialized
     */
    fun getConfig(): AxiomSDKConfig = config
}
