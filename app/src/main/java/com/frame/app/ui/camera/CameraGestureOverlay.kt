package com.frame.app.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import com.frame.app.ui.camera.gesture.SwipeDirection
import com.frame.app.ui.camera.gesture.classifySwipe
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.frame.app.ui.theme.FrameGreen
import com.frame.app.ui.theme.FrameYellow

/** Focus-ring timing (ms). Shared with the exposure slider so both fade together. */
const val FOCUS_RING_SETTLE_MS = 180
const val FOCUS_RING_HOLD_MS = 1400
const val FOCUS_RING_FADE_MS = 220

/** Horizontal travel needed before a one-finger drag counts as a mode swipe. */
private val SWIPE_THRESHOLD = 64.dp

/**
 * Transparent interaction layer over the preview that handles:
 *  - pinch-to-zoom (transform gesture) -> [onZoomBy] with the scale delta
 *  - tap-to-focus (tap gesture) -> [onFocus] with the NORMALIZED tap point and
 *    draws a short-lived focus ring where the user tapped
 *
 * Kept separate from [CameraScreen] so the gesture/ring logic is self-contained
 * and the screen just wires callbacks to the controller.
 */
@Composable
fun CameraGestureOverlay(
    focusResult: com.frame.app.platform.camera.FocusResult,
    onZoomBy: (scaleFactor: Float) -> Unit,
    onFocus: (normalizedX: Float, normalizedY: Float) -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    /** Reports the tap position in pixels (for placing the exposure slider). */
    onTapAt: (Offset) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentOnTapAt by rememberUpdatedState(onTapAt)
    // The pointer handlers are keyed on Unit, so read the latest callbacks.
    val currentOnZoomBy by rememberUpdatedState(onZoomBy)
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val currentOnFocus by rememberUpdatedState(onFocus)
    // Focus-ring position in pixels within this overlay; null = hidden.
    var ringCenter by remember { mutableStateOf<Offset?>(null) }
    val ringAlpha = remember { Animatable(0f) }
    val ringScale = remember { Animatable(1.4f) }

    // Target ring color per focus outcome; the animated value tweens between
    // them so success eases from yellow into green rather than snapping.
    val targetColor = when (focusResult) {
        com.frame.app.platform.camera.FocusResult.SUCCESS -> FrameGreen
        // Red is not part of the palette; a white ring just fades out unlocked.
        com.frame.app.platform.camera.FocusResult.FAILED,
        com.frame.app.platform.camera.FocusResult.UNSUPPORTED -> Color.White
        else -> FrameYellow
    }
    val ringColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 450),
        label = "focusRingColor",
    )

    LaunchedEffect(ringCenter) {
        if (ringCenter != null) {
            ringAlpha.snapTo(1f)
            ringScale.snapTo(1.4f)
            // Quick settle, then hold briefly, then fade.
            ringScale.animateTo(1f, animationSpec = tween(FOCUS_RING_SETTLE_MS))
            // Hold long enough to show the focus-result color settle.
            kotlinx.coroutines.delay(FOCUS_RING_HOLD_MS.toLong())
            ringAlpha.animateTo(0f, animationSpec = tween(FOCUS_RING_FADE_MS))
            ringCenter = null
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            // Tap-to-focus first. Keeping tap detection in its own pointerInput
            // that runs before the transform detector ensures single-finger taps
            // are delivered as taps instead of being consumed as (no-op) pans.
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        ringCenter = offset
                        currentOnTapAt(offset)
                        val nx = (offset.x / size.width).coerceIn(0f, 1f)
                        val ny = (offset.y / size.height).coerceIn(0f, 1f)
                        currentOnFocus(nx, ny)
                    },
                )
            }
            // Pinch-to-zoom (2+ fingers) and horizontal swipe (1 finger) share one
            // handler so a pinch can never be mistaken for a swipe.
            .pointerInput(Unit) {
                val thresholdPx = SWIPE_THRESHOLD.toPx()
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var dx = 0f
                    var dy = 0f
                    var swiped = false
                    var pinched = false
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2) {
                            pinched = true
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) currentOnZoomBy(zoom)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (!pinched && !swiped) {
                            val change = event.changes.firstOrNull() ?: break
                            val delta = change.positionChange()
                            dx += delta.x
                            dy += delta.y
                            val direction = classifySwipe(dx, dy, thresholdPx)
                            if (direction != null) {
                                swiped = true
                                change.consume()
                                currentOnSwipe(direction)
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .drawBehind {
                val center = ringCenter ?: return@drawBehind
                val radius = 44.dp.toPx() * ringScale.value
                // ringColor tweens between outcome colors (see animateColorAsState),
                // so the yellow -> green transition on a successful lock is gradual.
                drawCircle(
                    color = ringColor.copy(alpha = ringAlpha.value),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx()),
                )
            },
    )
}
