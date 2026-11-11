package com.frame.app.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.frame.app.domain.model.TiltState
import com.frame.app.ui.theme.FrameYellow
import kotlin.math.cos
import kotlin.math.sin

/**
 * A center horizon/level indicator. Two short reference ticks flank the center;
 * a tilting line rotates with the device roll. When within the level tolerance,
 * everything snaps to green and the line aligns with the reference ticks —
 * giving a clear "you're level" read while composing.
 *
 * Draws nothing when tilt data isn't available.
 */
@Composable
fun LevelIndicator(
    tilt: TiltState,
    modifier: Modifier = Modifier,
    opacity: Float = 0.9f,
) {
    if (!tilt.isAvailable) return

    val level = tilt.isLevel()
    val lineColor by animateColorAsState(
        targetValue = if (level) Color(0xFF4CAF50) else FrameYellow,
        label = "levelColor",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = minOf(size.width, size.height) * 0.16f
        val gap = half * 0.35f
        val tickLen = half * 0.4f

        // Fixed reference ticks (left/right of center), always horizontal.
        val refColor = Color.White.copy(alpha = 0.6f * opacity)
        drawLine(
            color = refColor,
            start = Offset(cx - half - tickLen, cy),
            end = Offset(cx - half, cy),
            strokeWidth = 3f,
        )
        drawLine(
            color = refColor,
            start = Offset(cx + half, cy),
            end = Offset(cx + half + tickLen, cy),
            strokeWidth = 3f,
        )

        // Rolling center line: rotate a horizontal segment by the roll angle.
        val rad = Math.toRadians(tilt.rollDegrees.toDouble())
        val dx = (cos(rad) * (half - gap)).toFloat()
        val dy = (sin(rad) * (half - gap)).toFloat()
        drawLine(
            color = lineColor.copy(alpha = opacity),
            start = Offset(cx - dx, cy - dy),
            end = Offset(cx + dx, cy + dy),
            strokeWidth = 5f,
        )

        // Center dot.
        drawCircle(
            color = lineColor.copy(alpha = opacity),
            radius = 4f,
            center = Offset(cx, cy),
        )
    }
}
