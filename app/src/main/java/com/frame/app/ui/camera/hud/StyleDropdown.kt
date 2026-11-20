package com.frame.app.ui.camera.hud

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.frame.app.domain.model.PhotoStyle
import com.frame.app.ui.components.GlassPill
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Sizes
import com.frame.app.ui.theme.Spacing

/**
 * Compact style control: a glass pill showing the current style that opens a
 * dropdown of all styles (plus "More..." for the full picker). The label
 * ellipsizes on narrow screens instead of crowding its neighbours.
 */
@Composable
fun StyleDropdown(
    styles: List<PhotoStyle>,
    currentStyle: PhotoStyle,
    onSelect: (PhotoStyle) -> Unit,
    onOpenFullPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        GlassPill(
            onClick = { expanded = true },
            onClickLabel = "Change photo style",
            contentPadding = PaddingValues(start = Spacing.sm, end = Spacing.xs),
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = FrameYellow,
                modifier = Modifier.size(Sizes.iconSmall),
            )
            Text(
                text = currentStyle.displayName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(Sizes.icon),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            styles.forEach { style ->
                val selected = style == currentStyle
                DropdownMenuItem(
                    text = {
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) FrameYellow else Color.Unspecified,
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
                text = { Text("More…", style = MaterialTheme.typography.labelLarge) },
                onClick = {
                    expanded = false
                    onOpenFullPicker()
                },
            )
        }
    }
}
