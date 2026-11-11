package com.frame.app.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import com.frame.app.ui.theme.FrameYellow

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
    modifier: Modifier = Modifier,
) {
    // Focus-ring position in pixels within this overlay; null = hidden.
    var ringCenter by remember { mutableStateOf<Offset?>(null) }
    val ringAlpha = remember { Animatable(0f) }
    val ringScale = remember { Animatable(1.4f) }

    // Target ring color per focus outcome; the animated value tweens between
    // them so success eases from yellow into green rather than snapping.
    val targetColor = when (focusResult) {
        com.frame.app.platform.camera.FocusResult.SUCCESS -> Color(0xFF4CAF50)
        com.frame.app.platform.camera.FocusResult.FAILED,
        com.frame.app.platform.camera.FocusResult.UNSUPPORTED -> Color(0xFFE53935)
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
            ringScale.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(180))
            // Hold long enough to show the focus-result color settle.
            kotlinx.coroutines.delay(1400)
            ringAlpha.animateTo(0f, animationSpec = androidx.compose.animation.core.tween(220))
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
                        val nx = (offset.x / size.width).coerceIn(0f, 1f)
                        val ny = (offset.y / size.height).coerceIn(0f, 1f)
                        onFocus(nx, ny)
                    },
                )
            }
            // Pinch-to-zoom: only reacts to multi-finger scale changes, so it
            // won't interfere with single-finger taps handled above.
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) onZoomBy(zoom)
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
