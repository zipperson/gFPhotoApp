package com.frame.app.ui.camera

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frame.app.ui.components.GlassPill
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Spacing
import java.util.Locale

/**
 * Small readout of the current zoom ratio, e.g. "2.4x". Shown only while zoomed
 * past the minimum so it doesn't clutter the default view.
 */
@Composable
fun ZoomIndicator(
    zoomRatio: Float,
    modifier: Modifier = Modifier,
) {
    GlassPill(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.sm),
    ) {
        Text(
            text = String.format(Locale.US, "%.1fx", zoomRatio),
            style = MaterialTheme.typography.labelLarge,
            color = FrameYellow,
        )
    }
}
