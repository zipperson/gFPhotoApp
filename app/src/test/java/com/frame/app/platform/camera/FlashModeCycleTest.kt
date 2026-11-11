package com.frame.app.platform.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure unit tests for the flash-mode cycle and CameraState capability flags.
 */
class FlashModeCycleTest {

    @Test
    fun flashCycle_followsOffAutoOn() {
        assertEquals(FlashMode.AUTO, FlashMode.OFF.next())
        assertEquals(FlashMode.ON, FlashMode.AUTO.next())
        assertEquals(FlashMode.OFF, FlashMode.ON.next())
    }

    @Test
    fun flashCycle_isFullLoopOfThree() {
        var mode = FlashMode.OFF
        mode = mode.next().next().next()
        assertEquals(FlashMode.OFF, mode)
    }

    @Test
    fun cameraState_defaultsAreSafe() {
        val state = CameraState()
        assertEquals(CameraLens.BACK, state.lens)
        assertEquals(FlashMode.OFF, state.flashMode)
        assertEquals(false, state.torchEnabled)
        assertEquals(false, state.hasFlashUnit)
    }

    @Test
    fun cameraState_supportsZoomReflectsRange() {
        val noZoom = CameraState(minZoomRatio = 1f, maxZoomRatio = 1f)
        val withZoom = CameraState(minZoomRatio = 1f, maxZoomRatio = 8f)
        assertEquals(false, noZoom.supportsZoom)
        assertEquals(true, withZoom.supportsZoom)
    }
}
