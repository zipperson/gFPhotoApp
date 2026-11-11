package com.frame.app.ui.camera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background

/**
 * Classic camera shutter: a white ring with a filled inner disc. Presses down
 * with a subtle scale, and dims while a capture is in flight so the user gets
 * immediate feedback that the tap registered.
 *
 * @param enabled   false while the camera isn't ready or a capture is running
 * @param onCapture invoked on tap
 */
@Composable
fun ShutterButton(
    enabled: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.92f,
        animationSpec = tween(durationMillis = 120),
        label = "shutterScale",
    )

    Box(
        modifier = modifier
            .size(76.dp)
            .scale(scale)
            .semantics { contentDescription = "Take photo" }
            .clip(CircleShape)
            .border(BorderStroke(4.dp, Color.White), CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onCapture,
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) Color.White
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                ),
        )
    }
}
