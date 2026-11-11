package com.frame.app.ui.camera

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.frame.app.ui.theme.FrameYellow

/**
 * Horizontal exposure-compensation control: a sun icon, a slider bounded to the
 * device's supported EV range, and the current index. The caller hides this when
 * the device reports no exposure support (min == max).
 *
 * @param index         current exposure compensation index
 * @param minIndex      device minimum (inclusive)
 * @param maxIndex      device maximum (inclusive)
 * @param onIndexChange invoked with the new (rounded, clamped) index
 */
@Composable
fun ExposureSlider(
    index: Int,
    minIndex: Int,
    maxIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.WbSunny,
            contentDescription = "Exposure",
            tint = if (index != 0) FrameYellow else Color.White,
        )
        Slider(
            value = index.toFloat(),
            onValueChange = { onIndexChange(it.toInt().coerceIn(minIndex, maxIndex)) },
            valueRange = minIndex.toFloat()..maxIndex.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = FrameYellow,
                activeTrackColor = FrameYellow,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        Text(
            text = if (index > 0) "+$index" else "$index",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(28.dp),
        )
    }
}
