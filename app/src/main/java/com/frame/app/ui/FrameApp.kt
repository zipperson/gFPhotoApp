package com.frame.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frame.app.ui.permission.CameraPermissionGate
import com.frame.app.ui.theme.FrameYellow

/**
 * Root composable for FRAME.
 *
 * Task 2: gate the app behind the CAMERA permission. Once granted we render the
 * camera placeholder below; Task 3 swaps that placeholder for the live
 * [com.frame.app.ui.camera.CameraScreen].
 */
@Composable
fun FrameApp() {
    CameraPermissionGate {
        CameraPlaceholder()
    }
}

/**
 * Temporary stand-in shown after permission is granted, until Task 3 wires the
 * live CameraX preview. Confirms the permission gate opens correctly.
 */
@Composable
private fun CameraPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "FRAME",
            color = FrameYellow,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Camera ready. Live preview arrives next.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
