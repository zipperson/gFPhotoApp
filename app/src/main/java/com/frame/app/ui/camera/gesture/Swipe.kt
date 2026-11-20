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
 * Mode strip reads `GUIDED  MINIMAL`. Swiping left moves to the right-hand
 * neighbour (MINIMAL); swiping right moves to the left-hand one (GUIDED).
 * Idempotent at the ends.
 */
fun CameraMode.afterSwipe(direction: SwipeDirection): CameraMode = when (direction) {
    SwipeDirection.LEFT -> CameraMode.MINIMAL
    SwipeDirection.RIGHT -> CameraMode.GUIDED
}
