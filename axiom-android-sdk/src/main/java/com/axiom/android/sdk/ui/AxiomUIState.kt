package com.axiom.android.sdk.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Internal UI state for the Axiom SDK
 * Manages UI state that doesn't belong in the global SDK state
 */
object AxiomUIState {
    var selectedModelId by mutableStateOf<String?>(null)
}
