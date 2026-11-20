package com.frame.app.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frame.app.ui.theme.Sizes

/**
 * Classic camera shutter: a ring with a filled inner disc. Presses down with a
 * subtle scale, and dims while a capture is in flight so the user gets
 * immediate feedback that the tap registered.
 *
 * @param enabled   false while the camera isn't ready or a capture is running
 * @param ringColor ring colour; white normally (the "ready" state tints it)
 * @param onCapture invoked on tap
 */
@Composable
fun ShutterButton(
    enabled: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
    ringColor: Color = Color.White,
    countingDown: Boolean = false,
) {
    // Ease between white and green so "ready" settles in rather than snapping.
    val animatedRing by animateColorAsState(
        targetValue = ringColor,
        animationSpec = tween(durationMillis = 250),
        label = "shutterRing",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.92f,
        animationSpec = tween(durationMillis = 120),
        label = "shutterScale",
    )

    Box(
        modifier = modifier
            .size(Sizes.shutter)
            .scale(scale)
            .semantics {
                contentDescription = if (countingDown) "Cancel timer" else "Take photo"
            }
            .clip(CircleShape)
            .border(BorderStroke(4.dp, animatedRing), CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onCapture,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Inner disc morphs into a small rounded square (a "stop" glyph) while
        // a timer is counting down, signalling that tapping cancels.
        val innerSize by animateDpAsState(
            targetValue = if (countingDown) 28.dp else 64.dp,
            animationSpec = tween(durationMillis = 200),
            label = "shutterInnerSize",
        )
        val innerCorner by animateDpAsState(
            targetValue = if (countingDown) 8.dp else 32.dp,
            animationSpec = tween(durationMillis = 200),
            label = "shutterInnerCorner",
        )
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(RoundedCornerShape(innerCorner))
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.4f)),
        )
    }
}
