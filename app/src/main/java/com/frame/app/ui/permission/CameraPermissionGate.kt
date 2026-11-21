package com.frame.app.ui.permission

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frame.app.viewmodel.CameraViewModel
import com.frame.app.viewmodel.PermissionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Gates [content] behind the CAMERA runtime permission.
 *
 * Architecture: Accompanist owns the Android permission handshake, but all
 * *decisions* about what screen to show live in [CameraViewModel] via
 * [CameraUiState]. This composable just translates Accompanist's status into
 * ViewModel callbacks and renders the right branch — keeping the testable
 * state logic out of the UI.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionGate(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    viewModel: CameraViewModel = viewModel(),
    content: @Composable () -> Unit,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Mirror Accompanist's permission status into the ViewModel whenever it
    // changes (initial check + after the system dialog resolves).
    LaunchedEffect(cameraPermission.status) {
        viewModel.onPermissionResult(
            granted = cameraPermission.status.isGranted,
            shouldShowRationale = cameraPermission.status.shouldShowRationale,
        )
    }

    // API 26-28 only: saving to and reading the gallery needs a runtime storage
    // grant (scoped storage on 29+ doesn't). Ask once the camera is allowed, so
    // the user isn't hit with two dialogs at once. Granting WRITE also covers
    // READ (same permission group on these versions).
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val storagePermission = rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val cameraGranted = cameraPermission.status.isGranted
        LaunchedEffect(cameraGranted) {
            if (cameraGranted && !storagePermission.status.isGranted) {
                storagePermission.launchPermissionRequest()
            }
        }
    }

    // On first composition, if we've never asked, fire the request.
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted && !cameraPermission.status.shouldShowRationale) {
            cameraPermission.launchPermissionRequest()
        }
    }

    when (uiState.permission) {
        is PermissionState.Granted -> content()

        is PermissionState.NeedsRationale -> CameraPermissionRationale(
            modifier = modifier,
            isPermanentlyDenied = false,
            onRequest = { cameraPermission.launchPermissionRequest() },
        )

        is PermissionState.Denied -> CameraPermissionRationale(
            modifier = modifier,
            isPermanentlyDenied = true,
            onRequest = { cameraPermission.launchPermissionRequest() },
        )

        is PermissionState.Pending -> {
            // Brief transient state while the first request is in flight; show
            // the rationale card as a neutral waiting surface.
            CameraPermissionRationale(
                modifier = modifier,
                isPermanentlyDenied = false,
                onRequest = { cameraPermission.launchPermissionRequest() },
            )
        }
    }
}
