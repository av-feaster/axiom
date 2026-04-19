package com.axiom.core

/**
 * Reason why generation stopped
 */
enum class FinishReason {
    /** Generation stopped naturally (EOS token or stop word encountered) */
    STOP,
    
    /** Generation stopped because max tokens limit was reached */
    LENGTH,
    
    /** Generation stopped due to an error */
    ERROR,
    
    /** Generation was cancelled by user */
    CANCELLED,
    
    /** Generation stopped due to safety safeguards (hallucination, garbage, etc.) */
    SAFETY
}
