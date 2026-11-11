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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frame.app.ui.theme.FrameTheme
import com.frame.app.ui.theme.FrameYellow

/**
 * Root composable for FRAME.
 *
 * Phase 1 / Task 1 placeholder: proves the project builds, launches, and renders
 * Compose. Task 2 replaces this with the permission gate, and Task 3 with the
 * live [com.frame.app.ui.camera.CameraScreen].
 */
@Composable
fun FrameApp() {
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
            text = "Compose your shot before you take it.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FrameAppPreview() {
    FrameTheme {
        FrameApp()
    }
}
