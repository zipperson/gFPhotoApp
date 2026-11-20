package com.frame.app.ui.camera.hud

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.frame.app.domain.model.PhotoStyle
import com.frame.app.platform.camera.FlashMode
import com.frame.app.ui.components.GlassCircleButton
import com.frame.app.ui.theme.Sizes
import com.frame.app.ui.theme.Spacing

/**
 * The slim top bar: flash | style | more. Three controls instead of seven, with
 * the style pill always centred (both sides are 48dp wide, even when the flash
 * button is hidden on a camera with no flash).
 */
@Composable
fun CameraTopBar(
    flashMode: FlashMode,
    hasFlashUnit: Boolean,
    currentStyle: PhotoStyle,
    onCycleFlash: () -> Unit,
    onSelectStyle: (PhotoStyle) -> Unit,
    onOpenStylePicker: () -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasFlashUnit) {
            val (icon, label) = when (flashMode) {
                FlashMode.OFF -> Icons.Filled.FlashOff to "Flash off"
                FlashMode.AUTO -> Icons.Filled.FlashAuto to "Flash auto"
                FlashMode.ON -> Icons.Filled.FlashOn to "Flash on"
            }
            GlassCircleButton(
                icon = icon,
                contentDescription = label,
                onClickLabel = "Change flash mode",
                onClick = onCycleFlash,
                active = flashMode != FlashMode.OFF,
            )
        } else {
            // Keep the style pill centred when there's no flash button.
            Spacer(modifier = Modifier.size(Sizes.touchTarget))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            StyleDropdown(
                styles = PhotoStyle.values().toList(),
                currentStyle = currentStyle,
                onSelect = onSelectStyle,
                onOpenFullPicker = onOpenStylePicker,
            )
        }

        GlassCircleButton(
            icon = Icons.Filled.MoreHoriz,
            contentDescription = "More settings",
            onClick = onOpenMore,
        )
    }
}
