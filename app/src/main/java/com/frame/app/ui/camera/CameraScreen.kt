package com.frame.app.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frame.app.platform.camera.CameraXController

/**
 * The live camera screen. Task 3 renders a full-screen CameraX preview.
 *
 * The CameraX preview is a classic Android [PreviewView] hosted inside Compose
 * via [AndroidView]; overlays (Task 8) will be a Compose Canvas layered on top
 * within this same [Box], which is why the preview sits in a sizing container
 * rather than filling the window directly.
 *
 * The [CameraXController] is remembered for the screen's lifetime and bound to
 * the current [LocalLifecycleOwner], so CameraX owns start/stop and teardown.
 */
@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // One controller per screen instance.
    val controller = remember { CameraXController(context.applicationContext) }
    val cameraState by controller.state.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    // PERFORMANCE compat gives the broadest device support;
                    // FILL_CENTER matches a full-screen camera UI.
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller.bind(lifecycleOwner, this)
                }
            },
        )

        // Surface a binding error if the camera failed to start.
        cameraState.error?.let { message ->
            CameraErrorNotice(message = message)
        }
    }
}
