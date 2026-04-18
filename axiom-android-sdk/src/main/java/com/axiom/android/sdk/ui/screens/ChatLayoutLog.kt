package com.axiom.android.sdk.ui.screens

import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow

/**
 * Throttled layout / inset logging for [ChatScreen] IME investigations.
 * Filter Logcat: `ChatLayout`
 */
internal object ChatLayoutLog {
    const val TAG = "ChatLayout"
    private var lastGlobalLogMs = 0L

    fun throttled(message: String, minIntervalMs: Long = 450L) {
        val now = SystemClock.uptimeMillis()
        if (now - lastGlobalLogMs >= minIntervalMs) {
            lastGlobalLogMs = now
            Log.i(TAG, message)
        }
    }

    fun layoutLine(
        label: String,
        coords: LayoutCoordinates,
        extra: String = "",
    ) {
        if (!coords.isAttached) return
        val size = coords.size
        val pos = coords.positionInWindow()
        val bottom = pos.y + size.height
        throttled(
            "[$label] size=${size.width}x${size.height}px " +
                "windowY=${pos.y.toInt()}..${bottom.toInt()}px $extra"
        )
    }
}
