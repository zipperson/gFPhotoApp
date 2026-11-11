package com.frame.app.domain.model

/**
 * Pure-Kotlin geometry primitives used across the domain layer.
 *
 * Everything here is expressed in a **normalized coordinate space** where
 * (0f, 0f) is the top-left of the frame and (1f, 1f) is the bottom-right,
 * independent of pixels, screen density, or sensor resolution. This keeps the
 * composition math resolution-agnostic and portable (including to a future
 * non-Android target), and defers pixel mapping to the overlay/render layer.
 */

/** A point in normalized frame space, components typically in [0f, 1f]. */
data class NormalizedPoint(
    val x: Float,
    val y: Float,
)

/**
 * An axis-aligned rectangle in normalized frame space.
 *
 * @property left   left edge, normalized
 * @property top    top edge, normalized
 * @property right  right edge, normalized
 * @property bottom bottom edge, normalized
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val area: Float get() = width.coerceAtLeast(0f) * height.coerceAtLeast(0f)

    val center: NormalizedPoint get() = NormalizedPoint(centerX, centerY)

    companion object {
        val FULL = NormalizedRect(0f, 0f, 1f, 1f)
    }
}
