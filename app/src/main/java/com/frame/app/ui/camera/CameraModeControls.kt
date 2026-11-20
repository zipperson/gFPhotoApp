package com.frame.app.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/**
 * Compact style control: a small pill showing the current style that opens a
 * dropdown menu of all styles (stock-camera-app style), so it stays out of the
 * way yet lets the user switch quickly. Also offers "More…" to open the full
 * selection screen.
 */
@Composable
fun StyleDropdown(
    styles: List<com.frame.app.domain.model.PhotoStyle>,
    currentStyle: com.frame.app.domain.model.PhotoStyle,
    onSelect: (com.frame.app.domain.model.PhotoStyle) -> Unit,
    onOpenFullPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Change style",
                tint = FrameYellow,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = currentStyle.displayName,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = Color.White,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            styles.forEach { style ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = style.displayName,
                            fontWeight = if (style == currentStyle) FontWeight.Bold else FontWeight.Normal,
                            color = if (style == currentStyle) FrameYellow else Color.Unspecified,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(style)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("More…") },
                onClick = {
                    expanded = false
                    onOpenFullPicker()
                },
            )
        }
    }
}
