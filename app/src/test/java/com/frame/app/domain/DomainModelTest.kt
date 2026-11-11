package com.frame.app.domain

import com.frame.app.domain.model.AspectCheck
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the pure domain layer. These are plain JVM unit tests with no
 * Android dependencies — if the domain package ever pulls in an Android type,
 * this test source set stops compiling, which is exactly the guard we want.
 */
class DomainModelTest {

    @Test
    fun normalizedRect_computesGeometry() {
        val rect = NormalizedRect(left = 0.2f, top = 0.1f, right = 0.6f, bottom = 0.9f)
        assertEquals(0.4f, rect.width, EPS)
        assertEquals(0.8f, rect.height, EPS)
        assertEquals(0.4f, rect.centerX, EPS)
        assertEquals(0.5f, rect.centerY, EPS)
        assertEquals(0.32f, rect.area, EPS)
    }

    @Test
    fun normalizedRect_fullCoversWholeFrame() {
        assertEquals(1f, NormalizedRect.FULL.area, EPS)
    }

    @Test
    fun subjectFrame_empty_hasNoSubject() {
        val frame = SubjectFrame.empty(timestampMs = 123L)
        assertFalse(frame.hasSubject)
        assertTrue(frame.landmarks.isEmpty())
        assertEquals(123L, frame.timestampMs)
    }

    @Test
    fun subjectFrame_findsLandmarkByType() {
        val nose = NormalizedLandmark(
            type = LandmarkType.NOSE,
            position = NormalizedPoint(0.5f, 0.3f),
            confidence = 0.9f,
        )
        val frame = SubjectFrame(
            boundingBox = NormalizedRect(0.3f, 0.1f, 0.7f, 0.95f),
            landmarks = listOf(nose),
            timestampMs = 1L,
        )
        assertTrue(frame.hasSubject)
        assertEquals(nose, frame.landmark(LandmarkType.NOSE))
        assertNull(frame.landmark(LandmarkType.LEFT_ANKLE))
    }

    @Test
    fun tiltState_levelWithinTolerance() {
        assertTrue(TiltState(rollDegrees = 1.0f, pitchDegrees = 0f, isAvailable = true).isLevel())
        assertFalse(TiltState(rollDegrees = 5.0f, pitchDegrees = 0f, isAvailable = true).isLevel())
    }

    @Test
    fun tiltState_unavailableIsNeverLevel() {
        assertFalse(TiltState.UNAVAILABLE.isLevel())
    }

    @Test
    fun compositionResult_noneIsNeutral() {
        val none = CompositionResult.NONE
        assertEquals(CompositionRating.UNKNOWN, none.overall)
        assertNull(none.score)
        assertTrue(none.aspects.isEmpty())
        assertTrue(none.suggestions.isEmpty())
    }

    @Test
    fun aspectCheck_constructs() {
        val check = AspectCheck(
            aspect = CompositionAspect.HEADROOM,
            verdict = AspectVerdict.NEEDS_ADJUSTMENT,
            detail = "Leave more space above the subject.",
        )
        assertEquals(CompositionAspect.HEADROOM, check.aspect)
        assertEquals(AspectVerdict.NEEDS_ADJUSTMENT, check.verdict)
    }

    private companion object {
        const val EPS = 1e-4f
    }
}
