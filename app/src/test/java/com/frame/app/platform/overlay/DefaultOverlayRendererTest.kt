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
    fun landmarks_hiddenByDefault_shownWhenEnabled() {
        val box = NormalizedRect(0.3f, 0.1f, 0.7f, 0.9f)
        val point = com.frame.app.domain.model.NormalizedLandmark(
            type = com.frame.app.domain.model.LandmarkType.entries.first(),
            position = com.frame.app.domain.model.NormalizedPoint(0.5f, 0.5f),
            confidence = 1f,
        )
        val subject = SubjectFrame(boundingBox = box, landmarks = listOf(point), timestampMs = 0L)

        val off = renderer.buildSpec(
            subject = subject,
            tilt = TiltState.UNAVAILABLE,
            composition = CompositionResult.NONE,
            settings = OverlaySettings.GUIDED,
        )
        assertTrue(off.landmarks.isEmpty())

        val on = renderer.buildSpec(
            subject = subject,
            tilt = TiltState.UNAVAILABLE,
            composition = CompositionResult.NONE,
            settings = OverlaySettings.GUIDED.copy(showLandmarks = true),
        )
        assertEquals(1, on.landmarks.size)
    }

    @Test
    fun aiTarget_supersedesRuleTarget() {
        val rect = NormalizedRect(0.2f, 0.1f, 0.8f, 0.9f)
        val spec = com.frame.app.domain.service.TargetSpec(
            targetRect = rect, subjectCenter = null, aligned = false,
        )
        val withAi = renderer.buildSpec(
            subject = SubjectFrame.empty(0L),
            tilt = TiltState.UNAVAILABLE,
            composition = CompositionResult.NONE,
            settings = OverlaySettings.GUIDED,
            target = spec,
            aiTarget = spec,
        )
        assertNull(withAi.target)
        assertEquals(spec, withAi.aiTarget)

        val ruleOnly = renderer.buildSpec(
            subject = SubjectFrame.empty(0L),
            tilt = TiltState.UNAVAILABLE,
            composition = CompositionResult.NONE,
            settings = OverlaySettings.GUIDED,
            target = spec,
        )
        assertEquals(spec, ruleOnly.target)
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
