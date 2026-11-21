package com.frame.app.ui.camera.gesture

import com.frame.app.domain.model.CameraMode
import kotlin.math.abs

/** Direction of a completed horizontal swipe. */
enum class SwipeDirection { LEFT, RIGHT }

/**
 * Classifies accumulated single-finger movement as a horizontal swipe.
 *
 * A swipe needs horizontal travel past [thresholdPx] and must be clearly more
 * horizontal than vertical (2x), so diagonal drags and taps are ignored.
 */
fun classifySwipe(dx: Float, dy: Float, thresholdPx: Float): SwipeDirection? {
    if (abs(dx) < thresholdPx) return null
    if (abs(dx) < 2f * abs(dy)) return null
    return if (dx < 0f) SwipeDirection.LEFT else SwipeDirection.RIGHT
}

/**
 * Mode strip reads `GUIDED  GROUP  MINIMAL` (the enum order). Swiping left moves
 * to the right-hand neighbour; swiping right moves to the left-hand one. It
 * stops at the ends rather than wrapping.
 */
fun CameraMode.afterSwipe(direction: SwipeDirection): CameraMode {
    val modes = CameraMode.entries
    val step = if (direction == SwipeDirection.LEFT) 1 else -1
    return modes[(ordinal + step).coerceIn(0, modes.lastIndex)]
}
