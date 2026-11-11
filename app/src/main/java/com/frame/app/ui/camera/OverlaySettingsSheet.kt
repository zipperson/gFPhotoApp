package com.frame.app.ui.camera

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frame.app.domain.service.OverlaySettings
import com.frame.app.ui.theme.FrameYellow

/**
 * Bottom sheet to toggle individual guides and adjust overlay opacity. Changes
 * are reported up via [onSettingsChange] (persisted by the ViewModel), so the
 * sheet stays stateless and the live overlay updates immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySettingsSheet(
    settings: OverlaySettings,
    onSettingsChange: ((OverlaySettings) -> OverlaySettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Overlay guides",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            GuideToggle("Rule of thirds", settings.showThirds) { on ->
                onSettingsChange { it.copy(showThirds = on) }
            }
            GuideToggle("Center crosshair", settings.showCenterCrosshair) { on ->
                onSettingsChange { it.copy(showCenterCrosshair = on) }
            }
            GuideToggle("Horizon / level", settings.showHorizon) { on ->
                onSettingsChange { it.copy(showHorizon = on) }
            }
            GuideToggle("Subject box", settings.showSubjectBox) { on ->
                onSettingsChange { it.copy(showSubjectBox = on) }
            }

            Text(
                text = "Opacity",
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            Slider(
                value = settings.opacity,
                onValueChange = { v -> onSettingsChange { it.copy(opacity = v) } },
                valueRange = 0.1f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = FrameYellow,
                    activeTrackColor = FrameYellow,
                ),
            )
        }
    }
}

@Composable
private fun GuideToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FrameYellow,
                checkedTrackColor = FrameYellow.copy(alpha = 0.4f),
            ),
        )
    }
}
