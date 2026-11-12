package com.frame.app.ui.camera.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.frame.app.domain.service.OverlaySpec
import com.frame.app.ui.theme.FrameYellow
import kotlin.math.cos
import kotlin.math.sin

private val LevelGreen = Color(0xFF4CAF50)

/**
 * Single Canvas that renders the whole composition overlay from an [OverlaySpec].
 * Everything is drawn in normalized space mapped to the canvas via
 * [OverlayMapping], so the same code will place Phase 3 subject boxes and Phase 5
 * reference outlines correctly. White/yellow with [OverlaySpec.opacity].
 */
@Composable
fun CompositionOverlay(
    spec: OverlaySpec,
    modifier: Modifier = Modifier,
) {
    // Smooth the horizon color between yellow and green as level is gained/lost.
    val horizonColor by animateColorAsState(
        targetValue = if (spec.horizon?.isLevel == true) LevelGreen else FrameYellow,
        label = "horizonColor",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val map = OverlayMapping.forCanvas(size.width, size.height)
        val guide = Color.White.copy(alpha = spec.opacity)
        val stroke = 2f

        // Draw order = z-order (back to front): grid, subject box, horizon, then
        // the center crosshair last so it sits on top of everything.

        if (spec.showThirds) {
            // Two vertical and two horizontal lines at 1/3 and 2/3.
            listOf(1f / 3f, 2f / 3f).forEach { f ->
                drawLine(guide, Offset(map.x(f), 0f), Offset(map.x(f), size.height), stroke)
                drawLine(guide, Offset(0f, map.y(f)), Offset(size.width, map.y(f)), stroke)
            }
        }

        spec.subjectBox?.let { box ->
            val left = map.x(box.left)
            val top = map.y(box.top)
            val right = map.x(box.right)
            val bottom = map.y(box.bottom)
            drawSubjectBrackets(
                left, top, right, bottom,
                color = FrameYellow.copy(alpha = spec.opacity),
            )
        }

        // Landmark dots (head/shoulders/hips/etc.) over the subject.
        if (spec.landmarks.isNotEmpty()) {
            val dotColor = FrameYellow.copy(alpha = spec.opacity)
            spec.landmarks.forEach { lm ->
                drawCircle(
                    color = dotColor,
                    radius = 5f,
                    center = Offset(map.x(lm.position.x), map.y(lm.position.y)),
                )
            }
        }

        spec.horizon?.let { horizon ->
            drawHorizon(horizon.rollDegrees, horizonColor.copy(alpha = spec.opacity))
        }

        if (spec.showCenterCrosshair) {
            val cx = map.x(0.5f)
            val cy = map.y(0.5f)
            val arm = size.minDimension * 0.03f
            val cross = FrameYellow.copy(alpha = spec.opacity)
            drawLine(cross, Offset(cx - arm, cy), Offset(cx + arm, cy), 3f)
            drawLine(cross, Offset(cx, cy - arm), Offset(cx, cy + arm), 3f)
        }
    }
}

/**
 * Draw the subject box as four corner brackets (viewfinder "Framed" look) rather
 * than a full rectangle, so it frames the subject without boxing them in.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSubjectBrackets(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    color: Color,
) {
    val w = right - left
    val h = bottom - top
    // Corner arm length scales with box size but is clamped for small subjects.
    val arm = (minOf(w, h) * 0.22f).coerceIn(16f, 60f)
    val stroke = 4f

    // Top-left
    drawLine(color, Offset(left, top), Offset(left + arm, top), stroke)
    drawLine(color, Offset(left, top), Offset(left, top + arm), stroke)
    // Top-right
    drawLine(color, Offset(right, top), Offset(right - arm, top), stroke)
    drawLine(color, Offset(right, top), Offset(right, top + arm), stroke)
    // Bottom-left
    drawLine(color, Offset(left, bottom), Offset(left + arm, bottom), stroke)
    drawLine(color, Offset(left, bottom), Offset(left, bottom - arm), stroke)
    // Bottom-right
    drawLine(color, Offset(right, bottom), Offset(right - arm, bottom), stroke)
    drawLine(color, Offset(right, bottom), Offset(right, bottom - arm), stroke)
}

/** Draw the center level/horizon indicator: fixed ticks + a rolling line. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHorizon(
    rollDegrees: Float,
    color: Color,
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val half = size.minDimension * 0.16f
    val gap = half * 0.35f
    val tickLen = half * 0.4f

    val refColor = Color.White.copy(alpha = 0.6f)
    drawLine(refColor, Offset(cx - half - tickLen, cy), Offset(cx - half, cy), 3f)
    drawLine(refColor, Offset(cx + half, cy), Offset(cx + half + tickLen, cy), 3f)

    val rad = Math.toRadians(rollDegrees.toDouble())
    val dx = (cos(rad) * (half - gap)).toFloat()
    val dy = (sin(rad) * (half - gap)).toFloat()
    drawLine(color, Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy), 5f)
    drawCircle(color, radius = 4f, center = Offset(cx, cy))
}
