package com.frame.app.platform.overlay

import com.frame.app.domain.model.CompositionResult
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState
import com.frame.app.domain.service.OverlaySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure tests for the "what to draw" overlay logic.
 */
class DefaultOverlayRendererTest {

    private val renderer = DefaultOverlayRenderer()

    @Test
    fun guidedSettings_showsThirdsCrosshairHorizon() {
        val spec = renderer.buildSpec(
            subject = SubjectFrame.empty(0L),
            tilt = TiltState(rollDegrees = 0f, pitchDegrees = 0f, isAvailable = true),
            composition = CompositionResult.NONE,
            settings = OverlaySettings.GUIDED,
        )
        assertTrue(spec.showThirds)
        assertTrue(spec.showCenterCrosshair)
        assertEquals(true, spec.horizon?.isLevel)
    }

    @Test
    fun minimalSettings_isHorizonOnly() {
        val spec = renderer.buildSpec(
            subject = SubjectFrame.empty(0L),
            tilt = TiltState(rollDegrees = 0f, pitchDegrees = 0f, isAvailable = true),
            composition = CompositionResult.NONE,
            settings = OverlaySettings.MINIMAL,
        )
        // Minimal is clean: horizon only, no grid or crosshair.
        assertFalse(spec.showThirds)
        assertFalse(spec.showCenterCrosshair)
        assertEquals(true, spec.horizon?.isLevel)
    }

    @Test
    fun horizon_omittedWhenTiltUnavailable() {
        val spec = renderer.buildSpec(
            subject = SubjectFrame.empty(0L),
            tilt = TiltState.UNAVAILABLE,
            composition = CompositionResult.NONE,
            settings = OverlaySettings.GUIDED,
        )
        assertNull(spec.horizon)
    }

    @Test
    fun subjectBox_passedThroughOnlyWhenEnabledAndPresent() {
        val box = NormalizedRect(0.3f, 0.1f, 0.7f, 0.9f)
        val subject = SubjectFrame(boundingBox = box, landmarks = emptyList(), timestampMs = 0L)

        val shown = renderer.buildSpec(
            subject = subject,
            tilt = TiltState.UNAVAILABLE,
            composition = CompositionResult.NONE,
            settings = OverlaySettings.GUIDED, // showSubjectBox = true
        )
        assertEquals(box, shown.subjectBox)

        val hidden = renderer.buildSpec(
            subject = subject,
            tilt = TiltState.UNAVAILABLE,
            composition = CompositionResult.NONE,
            settings = OverlaySettings.MINIMAL, // showSubjectBox = false
        )
        assertNull(hidden.subjectBox)
    }

    @Test
    fun opacity_isClampedToUnitInterval() {
        val spec = renderer.buildSpec(
            subject = SubjectFrame.empty(0L),
            tilt = TiltState.UNAVAILABLE,
            composition = CompositionResult.NONE,
            settings = OverlaySettings.GUIDED.copy(opacity = 2.5f),
        )
        assertEquals(1f, spec.opacity, 1e-4f)
    }
}
