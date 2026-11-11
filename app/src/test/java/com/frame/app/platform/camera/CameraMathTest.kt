package com.frame.app.platform.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure unit tests for camera control clamping. These mirror the exact logic the
 * CameraXController uses, so edge cases (out-of-range, inverted/empty ranges)
 * are covered without a device.
 */
class CameraMathTest {

    private val eps = 1e-4f

    @Test
    fun clampZoom_withinRange_isUnchanged() {
        assertEquals(2.5f, CameraMath.clampZoom(2.5f, 1f, 8f), eps)
    }

    @Test
    fun clampZoom_belowMin_clampsToMin() {
        assertEquals(1f, CameraMath.clampZoom(0.2f, 1f, 8f), eps)
    }

    @Test
    fun clampZoom_aboveMax_clampsToMax() {
        assertEquals(8f, CameraMath.clampZoom(99f, 1f, 8f), eps)
    }

    @Test
    fun clampZoom_emptyRange_returnsMin() {
        // Device with no zoom: min == max == 1.
        assertEquals(1f, CameraMath.clampZoom(5f, 1f, 1f), eps)
    }

    @Test
    fun clampLinearZoom_boundsToUnitInterval() {
        assertEquals(0f, CameraMath.clampLinearZoom(-0.5f), eps)
        assertEquals(1f, CameraMath.clampLinearZoom(2f), eps)
        assertEquals(0.3f, CameraMath.clampLinearZoom(0.3f), eps)
    }

    @Test
    fun clampExposure_withinRange_isUnchanged() {
        assertEquals(2, CameraMath.clampExposure(2, -6, 6))
    }

    @Test
    fun clampExposure_outOfRange_clamps() {
        assertEquals(-6, CameraMath.clampExposure(-20, -6, 6))
        assertEquals(6, CameraMath.clampExposure(20, -6, 6))
    }

    @Test
    fun clampExposure_unsupportedRange_returnsZero() {
        // min == max means exposure compensation unsupported.
        assertEquals(0, CameraMath.clampExposure(3, 0, 0))
    }

    @Test
    fun clampNormalized_boundsToUnitInterval() {
        assertEquals(0f, CameraMath.clampNormalized(-1f), eps)
        assertEquals(1f, CameraMath.clampNormalized(5f), eps)
        assertEquals(0.75f, CameraMath.clampNormalized(0.75f), eps)
    }
}
