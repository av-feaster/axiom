package com.axiom.android.sdk

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global state store for the Axiom SDK
 * Manages the overall SDK state across all components
 */
object AxiomStateStore {
    private val _state = MutableStateFlow<AxiomState>(AxiomState.Idle)
    val state: StateFlow<AxiomState> = _state

    /**
     * Update the global SDK state
     * @param newState The new state to set
     */
    fun setState(newState: AxiomState) {
        _state.value = newState
    }
}
