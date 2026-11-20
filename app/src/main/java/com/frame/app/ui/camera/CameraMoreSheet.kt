package com.frame.app.ui.camera

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.frame.app.domain.service.OverlaySettings
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Sizes
import com.frame.app.ui.theme.Spacing

/**
 * The "More" sheet: everything that used to crowd the top bar. Grouped as
 * Camera (torch, composition guidance) and Guides (which overlays to
 * draw, and how strongly). Every row is a full-width, 48dp+ tap target.
 *
 * Stateless: changes are reported up and persisted by the ViewModels, so the
 * live overlay reacts immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraMoreSheet(
    settings: OverlaySettings,
    onSettingsChange: ((OverlaySettings) -> OverlaySettings) -> Unit,
    analysisEnabled: Boolean,
    onAnalysisChange: (Boolean) -> Unit,
    hasFlashUnit: Boolean,
    torchEnabled: Boolean,
    onTorchChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
        ) {
            Text(
                text = "Camera settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Spacing.md),
            )

            SectionLabel("Camera")
            if (hasFlashUnit) {
                SettingSwitchRow(
                    title = "Torch",
                    subtitle = "Keep the light on while you compose",
                    checked = torchEnabled,
                    onCheckedChange = onTorchChange,
                )
            }
            SettingSwitchRow(
                title = "Composition guidance",
                subtitle = "Turn off to save battery. The AI coach still works",
                checked = analysisEnabled,
                onCheckedChange = onAnalysisChange,
            )

            SectionLabel("Guides", modifier = Modifier.padding(top = Spacing.lg))
            SettingSwitchRow("Rule of thirds", null, settings.showThirds) { on ->
                onSettingsChange { it.copy(showThirds = on) }
            }
            SettingSwitchRow("Center crosshair", null, settings.showCenterCrosshair) { on ->
                onSettingsChange { it.copy(showCenterCrosshair = on) }
            }
            SettingSwitchRow("Horizon / level", null, settings.showHorizon) { on ->
                onSettingsChange { it.copy(showHorizon = on) }
            }
            SettingSwitchRow("Subject box", null, settings.showSubjectBox) { on ->
                onSettingsChange { it.copy(showSubjectBox = on) }
            }
            SettingSwitchRow("Body points", null, settings.showLandmarks) { on ->
                onSettingsChange { it.copy(showLandmarks = on) }
            }

            Text(
                text = "Guide opacity",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = Spacing.sm),
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
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = modifier.padding(bottom = Spacing.xxs),
    )
}

/**
 * A whole-row toggle: the entire row (not just the switch) is the tap target and
 * is announced as one switch with its title and subtitle.
 */
@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Sizes.touchTarget + Spacing.xs)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        // The row handles the tap; the switch is purely visual.
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FrameYellow,
                checkedTrackColor = FrameYellow.copy(alpha = 0.4f),
            ),
        )
    }
}
