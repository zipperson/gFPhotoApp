package com.frame.app.domain.model

/** Self-timer delay before the shutter fires. Session-only, not persisted. */
enum class SelfTimer(val seconds: Int) {
    OFF(0),
    THREE(3),
    TEN(10);

    val isOn: Boolean get() = this != OFF

    /** Cycles OFF -> 3s -> 10s -> OFF. */
    fun next(): SelfTimer = when (this) {
        OFF -> THREE
        THREE -> TEN
        TEN -> OFF
    }

    /** Short label for the button, e.g. "3s". */
    val label: String get() = "${seconds}s"
}
