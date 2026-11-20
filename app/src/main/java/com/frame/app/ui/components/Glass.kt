package com.frame.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Sizes
import com.frame.app.ui.theme.Spacing

/**
 * The one translucent surface used for every floating control over the camera
 * preview. A single opacity keeps the HUD visually consistent (it used to use
 * six different values).
 */
object Glass {
    val Surface = Color.Black.copy(alpha = 0.45f)
}

/**
 * A fully rounded chip over the preview.
 *
 *  - Idle: translucent dark glass with white content.
 *  - [active]: solid yellow with black content, so state never depends on a
 *    subtle colour shift alone.
 *
 * When [onClick] is set the pill is announced as a button, and its tappable area
 * is at least 48dp even though it draws 40dp tall. Without [onClick] it is a
 * non-interactive readout.
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.md),
    content: @Composable RowScope.() -> Unit,
) {
    val container = if (active) FrameYellow else Glass.Surface
    val contentColor = if (active) Color.Black else Color.White

    // Outer box: owns the tap target (>= 48dp) so the visible pill can be 40dp.
    val outer = if (onClick != null) {
        modifier
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = Sizes.touchTarget, minHeight = Sizes.touchTarget)
    } else {
        modifier
    }

    Box(modifier = outer, contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier
                    .height(Sizes.pillHeight)
                    .clip(CircleShape)
                    .background(container)
                    .padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                content = content,
            )
        }
    }
}

/**
 * A round icon button over the preview. Always at least 48dp. [contentDescription]
 * is both the screen-reader label and what a long-press tooltip would show.
 */
@Composable
fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color? = null,
    onClickLabel: String? = null,
    size: Dp = Sizes.touchTarget,
) {
    val container = if (active) FrameYellow else Glass.Surface
    val iconTint = tint ?: if (active) Color.Black else Color.White

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(Sizes.icon),
        )
    }
}
