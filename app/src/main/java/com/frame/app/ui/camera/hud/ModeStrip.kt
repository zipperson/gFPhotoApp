package com.frame.app.ui.camera.hud

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frame.app.domain.model.CameraMode
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Sizes
import com.frame.app.ui.theme.Spacing

/**
 * `GUIDED  MINIMAL`: a stock-camera-style mode strip. The selected mode is full
 * white with a yellow dot under it; the other is dimmed. Switching animates: the
 * dot scales and fades from one tab to the other and the label cross-fades, so
 * the change reads as one smooth motion instead of a snap.
 *
 * There is deliberately no press highlight box. Each tab is still a 48dp tap
 * target and is announced to screen readers as a selectable tab.
 *
 * The text sits straight on the preview, so it carries a soft shadow for
 * legibility over bright scenes.
 */
@Composable
fun ModeStrip(
    mode: CameraMode,
    onSelect: (CameraMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeTab(
            label = "GUIDED",
            selected = mode == CameraMode.GUIDED,
            onClick = { onSelect(CameraMode.GUIDED) },
        )
        ModeTab(
            label = "MINIMAL",
            selected = mode == CameraMode.MINIMAL,
            onClick = { onSelect(CameraMode.MINIMAL) },
        )
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Slow-in/slow-out so the hand-off between the two tabs feels gradual.
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.6f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "modeLabelAlpha",
    )
    val dot by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "modeDot",
    )

    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = Sizes.touchTarget, minHeight = Sizes.touchTarget)
            .selectable(
                selected = selected,
                // No indication = no highlight box on press.
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.2.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f,
                ),
            ),
            color = Color.White.copy(alpha = labelAlpha),
        )
        // The slot is always reserved (no layout shift); only its scale and
        // opacity animate.
        Box(
            modifier = Modifier
                .padding(top = Spacing.xxs)
                .size(5.dp)
                .graphicsLayer {
                    scaleX = dot
                    scaleY = dot
                    alpha = dot
                }
                .clip(CircleShape)
                .background(FrameYellow),
        )
    }
}
