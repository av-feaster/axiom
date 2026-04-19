package com.axiom.android.sdk

/**
 * Operational modes for the Axiom SDK
 */
enum class AxiomMode {
    /**
     * SDK handles UI + model lifecycle
     * Use AxiomBottomSheet for complete UI experience
     */
    Managed,
    
    /**
     * Host app controls everything, no SDK UI
     * Use AxiomEngine and AxiomModelManager directly
     */
    Headless
}
