package com.frame.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the camera screen's state and the logic that transitions it.
 *
 * Deliberately free of Android framework types (no Context, no permission APIs):
 * the UI layer observes system state and reports *results* in via the
 * [onPermissionResult]/[onRationaleRequired] callbacks, and the ViewModel
 * decides the resulting [CameraUiState]. That keeps this class unit-testable
 * with plain JVM tests and keeps platform concerns at the edges.
 */
class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /**
     * Report the outcome of a camera-permission check or request.
     *
     * @param granted            true if CAMERA is currently granted
     * @param shouldShowRationale Android's "shouldShowRequestPermissionRationale"
     *                            — true means the user denied before but we may
     *                            ask again with an explanation; only meaningful
     *                            when [granted] is false.
     */
    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        val next = when {
            granted -> PermissionState.Granted
            shouldShowRationale -> PermissionState.NeedsRationale
            else -> PermissionState.Denied
        }
        _uiState.update { it.copy(permission = next) }
    }

    /**
     * Explicitly move to the rationale state (e.g. before firing the first
     * request when we want to show context up front).
     */
    fun onRationaleRequired() {
        _uiState.update { it.copy(permission = PermissionState.NeedsRationale) }
    }

    /** Reset to pending, e.g. to re-trigger a request after the user returns. */
    fun resetPermission() {
        _uiState.update { it.copy(permission = PermissionState.Pending) }
    }
}
