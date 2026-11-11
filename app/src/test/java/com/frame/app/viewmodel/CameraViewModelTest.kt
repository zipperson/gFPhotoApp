package com.frame.app.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the permission state machine. The ViewModel has no Android
 * dependencies, so these run without Robolectric or an emulator — exactly the
 * separation Task 2 is meant to establish.
 */
class CameraViewModelTest {

    @Test
    fun initialState_isPendingAndCannotShowCamera() {
        val vm = CameraViewModel()
        val state = vm.uiState.value
        assertEquals(PermissionState.Pending, state.permission)
        assertFalse(state.canShowCamera)
    }

    @Test
    fun granted_allowsCamera() {
        val vm = CameraViewModel()
        vm.onPermissionResult(granted = true, shouldShowRationale = false)
        val state = vm.uiState.value
        assertEquals(PermissionState.Granted, state.permission)
        assertTrue(state.canShowCamera)
    }

    @Test
    fun deniedWithRationale_movesToNeedsRationale() {
        val vm = CameraViewModel()
        vm.onPermissionResult(granted = false, shouldShowRationale = true)
        assertEquals(PermissionState.NeedsRationale, vm.uiState.value.permission)
        assertFalse(vm.uiState.value.canShowCamera)
    }

    @Test
    fun deniedWithoutRationale_movesToDenied() {
        val vm = CameraViewModel()
        vm.onPermissionResult(granted = false, shouldShowRationale = false)
        assertEquals(PermissionState.Denied, vm.uiState.value.permission)
    }

    @Test
    fun grantedThenRevoked_reflectsLatestResult() {
        val vm = CameraViewModel()
        vm.onPermissionResult(granted = true, shouldShowRationale = false)
        assertEquals(PermissionState.Granted, vm.uiState.value.permission)

        // Simulate the user revoking in settings and returning to the app.
        vm.onPermissionResult(granted = false, shouldShowRationale = false)
        assertEquals(PermissionState.Denied, vm.uiState.value.permission)
    }

    @Test
    fun onRationaleRequired_setsNeedsRationale() {
        val vm = CameraViewModel()
        vm.onRationaleRequired()
        assertEquals(PermissionState.NeedsRationale, vm.uiState.value.permission)
    }

    @Test
    fun resetPermission_returnsToPending() {
        val vm = CameraViewModel()
        vm.onPermissionResult(granted = true, shouldShowRationale = false)
        vm.resetPermission()
        assertEquals(PermissionState.Pending, vm.uiState.value.permission)
    }

    @Test
    fun uiState_emitsTransitionsInOrder() = runTest {
        val vm = CameraViewModel()
        vm.uiState.test {
            assertEquals(PermissionState.Pending, awaitItem().permission)

            vm.onPermissionResult(granted = false, shouldShowRationale = true)
            assertEquals(PermissionState.NeedsRationale, awaitItem().permission)

            vm.onPermissionResult(granted = true, shouldShowRationale = false)
            assertEquals(PermissionState.Granted, awaitItem().permission)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
