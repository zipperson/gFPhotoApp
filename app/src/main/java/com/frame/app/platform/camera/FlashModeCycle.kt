package com.frame.app.platform.camera

/**
 * Advance the flash mode in the user-facing cycle: OFF -> AUTO -> ON -> OFF.
 * Pure function, unit-tested, so the UI just calls it on each tap.
 */
fun FlashMode.next(): FlashMode = when (this) {
    FlashMode.OFF -> FlashMode.AUTO
    FlashMode.AUTO -> FlashMode.ON
    FlashMode.ON -> FlashMode.OFF
}
