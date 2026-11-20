package com.frame.app.ui.camera

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Bridges hardware volume keys from the Activity to whichever screen wants
 * them (the camera). While no handler is registered, [onKeyDown] returns false
 * so the system handles the key normally (volume works in the gallery, etc.).
 */
class ShutterKeyDispatcher {
    private var handler: (() -> Unit)? = null

    /** Registers [newHandler]; returns a function that unregisters it. */
    fun register(newHandler: () -> Unit): () -> Unit {
        handler = newHandler
        return { if (handler === newHandler) handler = null }
    }

    /**
     * @return true if the key press was consumed. Auto-repeat presses
     * (repeatCount > 0) are consumed but do not fire the handler again.
     */
    fun onKeyDown(repeatCount: Int): Boolean {
        val h = handler ?: return false
        if (repeatCount == 0) h()
        return true
    }

    /** Consume the matching key-up only while a handler is registered. */
    fun onKeyUp(): Boolean = handler != null
}

/** Default is a no-op dispatcher that never consumes keys. */
val LocalShutterKeys = staticCompositionLocalOf { ShutterKeyDispatcher() }
