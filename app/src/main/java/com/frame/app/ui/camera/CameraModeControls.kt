package com.frame.app.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frame.app.domain.model.CameraMode
import com.frame.app.ui.theme.FrameYellow

/**
 * Top-left controls: a Minimal/Guided mode pill and an overlay-settings button.
 * The mode pill shows the current mode and flips on tap; the tune icon opens the
 * guides/opacity sheet.
 */
@Composable
fun CameraModeControls(
    mode: CameraMode,
    onToggleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mode pill.
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onToggleMode)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (mode == CameraMode.GUIDED) "Guided" else "Minimal",
                color = if (mode == CameraMode.GUIDED) FrameYellow else Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Settings (tune) button.
        Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = "Overlay settings",
            tint = Color.White,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onOpenSettings)
                .padding(10.dp),
        )
    }
}
