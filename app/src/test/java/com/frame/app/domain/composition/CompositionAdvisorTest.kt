package com.frame.app.domain.composition

import com.frame.app.domain.model.AspectCheck
import com.frame.app.domain.model.AspectVerdict
import com.frame.app.domain.model.CompositionAspect
import com.frame.app.domain.model.LandmarkType
import com.frame.app.domain.model.NormalizedLandmark
import com.frame.app.domain.model.NormalizedPoint
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionAdvisorTest {

    private val advisor = CompositionAdvisor(CompositionThresholds.AESTHETIC_FULL_BODY)

    private fun subject(box: NormalizedRect): SubjectFrame {
        val nose = NormalizedLandmark(LandmarkType.NOSE, NormalizedPoint(box.centerX, box.centerY), 0.9f)
        return SubjectFrame(boundingBox = box, landmarks = listOf(nose), timestampMs = 0L)
    }

    private fun fail(a: CompositionAspect) = AspectCheck(a, AspectVerdict.NEEDS_ADJUSTMENT)
    private fun ok(a: CompositionAspect) = AspectCheck(a, AspectVerdict.GOOD)
    private fun level() = TiltState(0f, 0f, true)

    @Test
    fun noFailingChecks_noSuggestions() {
        val out = advisor.suggest(
            listOf(ok(CompositionAspect.HEADROOM), ok(CompositionAspect.BODY_FRAMING)),
            subject(NormalizedRect(0.3f, 0.3f, 0.5f, 0.7f)),
            level(),
        )
        assertTrue(out.isEmpty())
    }

    @Test
    fun subjectLeftOfThird_advisesMoveRight() {
        // cx = 0.15, nearest third is 1/3=0.333; cx < target -> "move right".
        val out = advisor.suggest(
            listOf(fail(CompositionAspect.SUBJECT_POSITION)),
            subject(NormalizedRect(0.1f, 0.3f, 0.2f, 0.7f)),
            level(),
        )
        assertEquals(listOf("Move slightly right"), out)
    }

    @Test
    fun subjectRightOfThird_advisesMoveLeft() {
        // cx = 0.85, nearest third 2/3=0.667; cx > target -> "move left".
        val out = advisor.suggest(
            listOf(fail(CompositionAspect.SUBJECT_POSITION)),
            subject(NormalizedRect(0.8f, 0.3f, 0.9f, 0.7f)),
            level(),
        )
        assertEquals(listOf("Move slightly left"), out)
    }

    @Test
    fun tooTight_advisesStepBack() {
        val out = advisor.suggest(
            listOf(fail(CompositionAspect.BODY_FRAMING)),
            subject(NormalizedRect(0.02f, 0.02f, 0.98f, 0.98f)), // size ~0.96 > tooLarge
            level(),
        )
        assertEquals(listOf("Step back to fit the whole shot"), out)
    }

    @Test
    fun tooFar_advisesMoveCloser() {
        val out = advisor.suggest(
            listOf(fail(CompositionAspect.BODY_FRAMING)),
            subject(NormalizedRect(0.48f, 0.48f, 0.53f, 0.55f)), // size ~0.07 < tooSmall 0.12
            level(),
        )
        assertEquals(listOf("Move a bit closer"), out)
    }

    @Test
    fun lowHeadroom_advisesMoreSpaceAbove() {
        val out = advisor.suggest(
            listOf(fail(CompositionAspect.HEADROOM)),
            subject(NormalizedRect(0.3f, 0.0f, 0.5f, 0.6f)), // top 0 < headroomMin
            level(),
        )
        assertEquals(listOf("Leave more space above the head"), out)
    }

    @Test
    fun highSubject_advisesLowerPhone() {
        // vc = 0.25 < idealVerticalCenter 0.6 -> lower phone.
        val out = advisor.suggest(
            listOf(fail(CompositionAspect.PERSPECTIVE)),
            subject(NormalizedRect(0.3f, 0.1f, 0.5f, 0.4f)),
            level(),
        )
        assertEquals(listOf("Lower your phone for a taller look"), out)
    }

    @Test
    fun tiltRight_advisesStraightenRight() {
        val out = advisor.suggest(
            listOf(fail(CompositionAspect.CAMERA_TILT)),
            subject(NormalizedRect(0.3f, 0.3f, 0.5f, 0.7f)),
            TiltState(rollDegrees = 10f, pitchDegrees = 0f, isAvailable = true),
        )
        assertEquals(listOf("Straighten the camera (tilting right)"), out)
    }

    @Test
    fun prioritizesAndCaps_toThree() {
        val checks = listOf(
            fail(CompositionAspect.NEGATIVE_SPACE),
            fail(CompositionAspect.CAMERA_TILT),
            fail(CompositionAspect.PERSPECTIVE),
            fail(CompositionAspect.HEADROOM),
            fail(CompositionAspect.SUBJECT_POSITION),
            fail(CompositionAspect.BODY_FRAMING),
        )
        val out = advisor.suggest(
            checks,
            subject(NormalizedRect(0.02f, 0.0f, 0.98f, 0.98f)),
            TiltState(5f, 0f, true),
        )
        assertEquals(3, out.size)
        // Highest priority is BODY_FRAMING, then SUBJECT_POSITION, then HEADROOM.
        assertEquals("Step back to fit the whole shot", out[0])
    }
}
