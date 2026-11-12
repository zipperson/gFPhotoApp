package com.frame.app.domain.composition

import com.frame.app.domain.model.AspectVerdict
import com.frame.app.domain.model.CompositionAspect
import com.frame.app.domain.model.CompositionRating
import com.frame.app.domain.model.CompositionResult
import com.frame.app.domain.model.LandmarkType
import com.frame.app.domain.model.NormalizedLandmark
import com.frame.app.domain.model.NormalizedPoint
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCompositionEngineTest {

    // Pin the classic preset for strict-rule tests (no vertical-placement check,
    // conventional thresholds) so expectations are deterministic.
    private val classic = DefaultCompositionEngine(CompositionThresholds.CLASSIC_PORTRAIT)
    private val aesthetic = DefaultCompositionEngine(CompositionThresholds.AESTHETIC_FULL_BODY)
    private val aspect = 0.75f

    private fun fullBodySubject(box: NormalizedRect): SubjectFrame {
        val types = listOf(
            LandmarkType.NOSE, LandmarkType.LEFT_SHOULDER, LandmarkType.RIGHT_SHOULDER,
            LandmarkType.LEFT_HIP, LandmarkType.LEFT_KNEE, LandmarkType.LEFT_ANKLE,
        )
        val lms = types.map { NormalizedLandmark(it, NormalizedPoint(box.centerX, box.centerY), 0.9f) }
        return SubjectFrame(boundingBox = box, landmarks = lms, timestampMs = 0L)
    }

    private fun level() = TiltState(rollDegrees = 0f, pitchDegrees = 0f, isAvailable = true)
    private fun tilted() = TiltState(rollDegrees = 12f, pitchDegrees = 0f, isAvailable = true)

    private fun verdict(result: CompositionResult, a: CompositionAspect) =
        result.aspects.firstOrNull { it.aspect == a }?.verdict

    @Test
    fun noSubject_isUnknownWithNullScore() {
        val result = classic.evaluate(SubjectFrame.empty(0L), level(), aspect)
        assertEquals(CompositionRating.UNKNOWN, result.overall)
        assertNull(result.score)
    }

    @Test
    fun classic_wellComposedOnThirds_scoresHigh() {
        val box = NormalizedRect(0.23f, 0.08f, 0.44f, 0.9f) // cx ~0.335, top 0.08, size 0.82
        val result = classic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.GOOD, verdict(result, CompositionAspect.SUBJECT_POSITION))
        assertEquals(AspectVerdict.GOOD, verdict(result, CompositionAspect.HEADROOM))
        assertEquals(AspectVerdict.GOOD, verdict(result, CompositionAspect.CAMERA_TILT))
        assertTrue("score=${result.score}", result.score!! >= 80)
        assertEquals(CompositionRating.GOOD, result.overall)
    }

    @Test
    fun classic_centeredSubject_isAcceptablePosition() {
        val box = NormalizedRect(0.42f, 0.08f, 0.58f, 0.9f) // cx 0.5
        val result = classic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.GOOD, verdict(result, CompositionAspect.SUBJECT_POSITION))
    }

    @Test
    fun classic_offThirds_flagsPosition() {
        val box = NormalizedRect(0.37f, 0.08f, 0.47f, 0.9f) // cx 0.42
        val result = classic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.NEEDS_ADJUSTMENT, verdict(result, CompositionAspect.SUBJECT_POSITION))
    }

    @Test
    fun classic_headCroppedAtTop_flagsHeadroom() {
        val box = NormalizedRect(0.3f, 0.0f, 0.5f, 0.85f) // top 0.0 < min
        val result = classic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.NEEDS_ADJUSTMENT, verdict(result, CompositionAspect.HEADROOM))
    }

    @Test
    fun classic_tooMuchHeadroom_flagsHeadroom() {
        val box = NormalizedRect(0.35f, 0.4f, 0.5f, 0.7f) // top 0.4 > classic tooMuch 0.30
        val result = classic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.NEEDS_ADJUSTMENT, verdict(result, CompositionAspect.HEADROOM))
    }

    @Test
    fun classic_tooTight_flagsFraming() {
        val box = NormalizedRect(0.02f, 0.02f, 0.98f, 0.99f) // size ~0.97 > classic 0.92
        val result = classic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.NEEDS_ADJUSTMENT, verdict(result, CompositionAspect.BODY_FRAMING))
    }

    @Test
    fun classic_tooFar_flagsFraming() {
        val box = NormalizedRect(0.45f, 0.45f, 0.55f, 0.6f) // size ~0.15 < classic 0.25
        val result = classic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.NEEDS_ADJUSTMENT, verdict(result, CompositionAspect.BODY_FRAMING))
    }

    @Test
    fun tiltedCamera_flagsTilt() {
        val box = NormalizedRect(0.23f, 0.08f, 0.44f, 0.9f)
        val result = classic.evaluate(fullBodySubject(box), tilted(), aspect)
        assertEquals(AspectVerdict.NEEDS_ADJUSTMENT, verdict(result, CompositionAspect.CAMERA_TILT))
    }

    @Test
    fun tiltReportedEvenWithoutSubject() {
        val result = classic.evaluate(SubjectFrame.empty(0L), tilted(), aspect)
        assertEquals(AspectVerdict.NEEDS_ADJUSTMENT, verdict(result, CompositionAspect.CAMERA_TILT))
    }

    @Test
    fun classic_score_isFractionOfGoodApplicableChecks() {
        val box = NormalizedRect(0.23f, 0.08f, 0.44f, 0.9f) // good except tilt
        val result = classic.evaluate(fullBodySubject(box), tilted(), aspect)
        // 5 applicable checks (no vertical-placement in classic), 4 good -> 80.
        assertEquals(80, result.score)
    }

    // --- Aesthetic preset: the TikTok full-body look ---

    @Test
    fun aesthetic_smallSubjectIsNotFlaggedAsTooFar() {
        // size ~0.2: would be "too far" under classic (0.25), fine under aesthetic (0.12).
        val box = NormalizedRect(0.28f, 0.55f, 0.42f, 0.75f) // size 0.2, low in frame
        val result = aesthetic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.GOOD, verdict(result, CompositionAspect.BODY_FRAMING))
    }

    @Test
    fun aesthetic_generousHeadroomIsGood() {
        // top 0.28: flagged "too much" under classic (0.30 would pass, but 0.4 fails);
        // comfortably within aesthetic tooMuch (0.45).
        val box = NormalizedRect(0.30f, 0.28f, 0.44f, 0.8f)
        val result = aesthetic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.GOOD, verdict(result, CompositionAspect.HEADROOM))
    }

    @Test
    fun aesthetic_includesVerticalPlacementCheck() {
        val box = NormalizedRect(0.28f, 0.35f, 0.42f, 0.85f) // cy 0.6 = ideal
        val result = aesthetic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.GOOD, verdict(result, CompositionAspect.PERSPECTIVE))
    }

    @Test
    fun aesthetic_highSubjectFlaggedForPlacement() {
        val box = NormalizedRect(0.28f, 0.05f, 0.42f, 0.4f) // cy 0.225, well above ideal 0.6
        val result = aesthetic.evaluate(fullBodySubject(box), level(), aspect)
        assertEquals(AspectVerdict.NEEDS_ADJUSTMENT, verdict(result, CompositionAspect.PERSPECTIVE))
    }
}
