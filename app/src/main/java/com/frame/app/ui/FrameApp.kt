package com.frame.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frame.app.ui.camera.CameraScreen
import com.frame.app.ui.permission.CameraPermissionGate
import com.frame.app.ui.style.StyleSelectionScreen
import com.frame.app.viewmodel.StyleViewModel
import com.frame.app.viewmodel.StyleViewModelFactory

/**
 * Root composable for FRAME.
 *
 * Flow: CAMERA permission gate -> first-run style picker (if the user hasn't
 * chosen a style yet) -> live camera. The camera can reopen the style picker at
 * any time, which is tracked by [reopenStyle].
 */
@Composable
fun FrameApp() {
    val context = LocalContext.current
    val styleViewModel: StyleViewModel = viewModel(
        factory = StyleViewModelFactory(context.applicationContext),
    )
    val styleState by styleViewModel.state.collectAsStateWithLifecycle()

    // Lets the camera reopen the picker after a style has already been chosen.
    var reopenStyle by remember { mutableStateOf(false) }

    CameraPermissionGate {
        when {
            !styleState.hasChosen -> StyleSelectionScreen(
                current = null,
                onStyleChosen = { styleViewModel.setStyle(it) },
            )

            reopenStyle -> StyleSelectionScreen(
                current = styleState.style,
                onStyleChosen = {
                    styleViewModel.setStyle(it)
                    reopenStyle = false
                },
                showBack = true,
                onBack = { reopenStyle = false },
            )

            else -> CameraScreen(
                onOpenStylePicker = { reopenStyle = true },
            )
        }
    }
}
