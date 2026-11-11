package com.frame.app.viewmodel

/**
 * The complete UI state the camera screen renders from. Exposed as a single
 * immutable value through [CameraViewModel.uiState] so Compose has one source
 * of truth. Grows across Phase 1 tasks (flash, zoom, mode, overlays); Task 2
 * introduces just the permission gate.
 */
data class CameraUiState(
    val permission: PermissionState = PermissionState.Pending,
) {
    /** The live camera surface should only be shown once permission is granted. */
    val canShowCamera: Boolean get() = permission is PermissionState.Granted
}

/**
 * Camera-permission status, mirrored from the Android permission system into a
 * platform-free type the ViewModel and tests can reason about.
 *
 *  Pending       -> not yet determined / request not made
 *  Granted       -> camera is usable
 *  NeedsRationale-> user denied once; we should explain why before re-asking
 *  Denied        -> permanently denied ("don't ask again"); must go to settings
 */
sealed interface PermissionState {
    data object Pending : PermissionState
    data object Granted : PermissionState
    data object NeedsRationale : PermissionState
    data object Denied : PermissionState
}
