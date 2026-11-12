package com.frame.app.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frame.app.platform.camera.CameraLens
import com.frame.app.platform.camera.FlashMode
import com.frame.app.ui.theme.FrameYellow

/**
 * Top control bar: flash-mode cycle, torch toggle, and lens switch. Rendered
 * over the preview. Controls that don't apply to the current camera (e.g. flash
 * on a flash-less front lens) are hidden rather than shown disabled, to keep the
 * surface clean.
 */
@Composable
fun CameraTopControls(
    flashMode: FlashMode,
    torchEnabled: Boolean,
    hasFlashUnit: Boolean,
    lens: CameraLens,
    analysisEnabled: Boolean,
    onCycleFlash: () -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchLens: () -> Unit,
    onToggleAnalysis: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Composition analysis on/off (also the heat/battery saver).
        ControlChip(
            icon = if (analysisEnabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
            description = if (analysisEnabled) "Composition guidance on" else "Composition guidance off",
            active = analysisEnabled,
            onClick = onToggleAnalysis,
        )
        if (hasFlashUnit) {
            val (flashIcon, flashLabel) = when (flashMode) {
                FlashMode.OFF -> Icons.Filled.FlashOff to "Flash off"
                FlashMode.ON -> Icons.Filled.FlashOn to "Flash on"
                FlashMode.AUTO -> Icons.Filled.FlashAuto to "Flash auto"
            }
            ControlChip(
                icon = flashIcon,
                description = flashLabel,
                active = flashMode != FlashMode.OFF,
                onClick = onCycleFlash,
            )
            ControlChip(
                icon = if (torchEnabled) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                description = if (torchEnabled) "Torch on" else "Torch off",
                active = torchEnabled,
                onClick = onToggleTorch,
            )
        }
        ControlChip(
            icon = Icons.Filled.Cameraswitch,
            description = if (lens == CameraLens.BACK) "Switch to front camera" else "Switch to back camera",
            active = false,
            onClick = onSwitchLens,
        )
    }
}

@Composable
private fun ControlChip(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = if (active) FrameYellow else Color.White,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(10.dp),
    )
}
