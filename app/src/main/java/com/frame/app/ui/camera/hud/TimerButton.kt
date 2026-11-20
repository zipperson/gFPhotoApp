package com.frame.app.ui.camera.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.frame.app.domain.model.SelfTimer
import com.frame.app.ui.components.GlassCircleButton
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Sizes

/**
 * Self-timer control. Off: glass circle with a timer icon. On: solid yellow
 * circle showing the delay ("3s" / "10s"). Each tap cycles OFF -> 3s -> 10s.
 */
@Composable
fun TimerButton(
    timer: SelfTimer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!timer.isOn) {
        GlassCircleButton(
            icon = Icons.Filled.Timer,
            contentDescription = "Self-timer off",
            onClickLabel = "Set self-timer",
            onClick = onClick,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier
                .size(Sizes.touchTarget)
                .clip(CircleShape)
                .background(FrameYellow)
                .semantics { contentDescription = "Self-timer ${timer.seconds} seconds" }
                .clickable(onClickLabel = "Change self-timer", role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = timer.label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Black,
            )
        }
    }
}
