package com.frame.app.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.frame.app.ui.theme.FrameYellow
import java.util.Locale

/**
 * Small pill showing the current zoom ratio, e.g. "1.0x" / "2.4x". Shown while
 * zoom is above the minimum so it doesn't clutter the default view.
 */
@Composable
fun ZoomIndicator(
    zoomRatio: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = String.format(Locale.US, "%.1fx", zoomRatio),
            color = FrameYellow,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
