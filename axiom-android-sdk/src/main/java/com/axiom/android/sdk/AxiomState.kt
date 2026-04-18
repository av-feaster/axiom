package com.axiom.android.sdk

/**
 * Global state for the AxiomSDK
 */
sealed class AxiomState {
    object Idle : AxiomState()
    data class Downloading(val progress: Float) : AxiomState()
    object Ready : AxiomState()
    object Generating : AxiomState()
    data class Error(val message: String) : AxiomState()
}
