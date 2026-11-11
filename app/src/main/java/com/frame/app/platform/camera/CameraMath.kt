package com.frame.app.platform.camera

/**
 * Pure clamping helpers for camera controls. Extracted so the clamping rules
 * are unit-testable without a device, and reused by [CameraXController].
 */
object CameraMath {

    /** Clamp a zoom ratio into [min, max]; tolerant of inverted/empty ranges. */
    fun clampZoom(ratio: Float, min: Float, max: Float): Float {
        if (max <= min) return min
        return ratio.coerceIn(min, max)
    }

    /** Clamp a linear-zoom value into [0, 1]. */
    fun clampLinearZoom(value: Float): Float = value.coerceIn(0f, 1f)

    /** Clamp an exposure index into [min, max]; empty range means unsupported -> 0. */
    fun clampExposure(index: Int, min: Int, max: Int): Int {
        if (max <= min) return 0
        return index.coerceIn(min, max)
    }

    /** Clamp a normalized coordinate component into [0, 1]. */
    fun clampNormalized(v: Float): Float = v.coerceIn(0f, 1f)
}
