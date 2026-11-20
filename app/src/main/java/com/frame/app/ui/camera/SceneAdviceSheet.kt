package com.frame.app.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frame.app.domain.scene.SceneAdvice
import com.frame.app.domain.scene.SceneAnalysisResult
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.viewmodel.SceneAdviceUiState

/**
 * Bottom sheet for the AI photo coach: a loading state, then the structured
 * advice (camera movement, lighting, background, overall) with a "Clear advice"
 * action, or a friendly offline/error state.
 *
 * Advisory and dismissable. Dismissing keeps the advice (and its on-screen
 * target); only [onClear] removes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneAdviceSheet(
    state: SceneAdviceUiState,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is SceneAdviceUiState.Idle) return

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = FrameYellow)
                Text(
                    text = "AI photo coach",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            when (state) {
                SceneAdviceUiState.Idle -> Unit
                SceneAdviceUiState.Loading -> LoadingRow()
                is SceneAdviceUiState.Ready -> when (val r = state.result) {
                    is SceneAnalysisResult.Success -> AdviceBody(r.advice, onClear)
                    is SceneAnalysisResult.Offline -> Notice(
                        Icons.Outlined.CloudOff,
                        "You're offline",
                        "AI coaching needs a connection. The on-screen guides still work offline.",
                    )
                    is SceneAnalysisResult.Error -> Notice(
                        Icons.Outlined.ErrorOutline,
                        "Couldn't get advice",
                        r.message,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = FrameYellow, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        Text(
            text = "Reading your scene…",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun AdviceBody(advice: SceneAdvice, onClear: () -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        advice.cameraMovement?.let { AdviceRow(Icons.Outlined.PhotoCamera, "Move the camera", it) }
        advice.lighting?.let { AdviceRow(Icons.Filled.WbSunny, "Lighting", it) }
        advice.background?.let { AdviceRow(Icons.Outlined.Landscape, "Background", it) }
        advice.overall?.let { AdviceRow(Icons.Filled.AutoAwesome, "Overall", it) }

        // If the model returned only prose, show it.
        if (advice.cameraMovement == null && advice.lighting == null &&
            advice.background == null && advice.overall == null
        ) {
            Text(advice.rawText, style = MaterialTheme.typography.bodyMedium)
        }

        if (advice.suggestedTarget != null) {
            Text(
                text = "Line your subject up with the cyan box on the preview.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Explicit, comfortably tappable clear action.
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Clear advice", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** One piece of advice: a tinted icon badge, a small caps label, and the text. */
@Composable
private fun AdviceRow(icon: ImageVector, label: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            // Read label + text together as one item for screen readers.
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FrameYellow.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = FrameYellow, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = FrameYellow,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun Notice(icon: ImageVector, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null)
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
