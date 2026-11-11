package com.frame.app.ui

import androidx.compose.runtime.Composable
import com.frame.app.ui.camera.CameraScreen
import com.frame.app.ui.permission.CameraPermissionGate

/**
 * Root composable for FRAME.
 *
 * Gates everything behind the CAMERA permission (Task 2); once granted, shows
 * the live camera screen (Task 3). Controls and overlays are added to
 * [CameraScreen] in later tasks.
 */
@Composable
fun FrameApp() {
    CameraPermissionGate {
        CameraScreen()
    }
}
