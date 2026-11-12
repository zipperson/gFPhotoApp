package com.frame.app.domain.composition

import com.frame.app.domain.model.LandmarkType
import com.frame.app.domain.model.NormalizedLandmark
import com.frame.app.domain.model.NormalizedPoint
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionTargetTest {

    private val eps = 1e-3f

    private fun subjectAt(box: NormalizedRect) =
        SubjectFrame(
            boundingBox = box,
            landmarks = listOf(NormalizedLandmark(LandmarkType.NOSE, NormalizedPoint(box.centerX, box.centerY), 0.9f)),
            timestampMs = 0L,
        )

    @Test
    fun aestheticTarget_isOnAThirdAndLow() {
        val t = CompositionTarget.idealRect(
            CompositionThresholds.AESTHETIC_FULL_BODY,
            preferLeftThird = true,
        )
        // Center x near the left third, center y near the style's ideal (0.6).
        assertEquals(CompositionMetrics.THIRD_LOW, t.centerX, 0.02f)
        assertEquals(0.6f, t.centerY, 0.02f)
    }

    @Test
    fun centeredStyleTarget_isCentered() {
        val t = CompositionTarget.idealRect(
            CompositionThresholds.CENTERED_SYMMETRICAL,
            preferLeftThird = true, // ignored for centered styles
        )
        assertEquals(0.5f, t.centerX, 0.02f)
    }

    @Test
    fun idealRectFor_picksNearerThird() {
        // Subject on the right half -> target should be the right third.
        val right = CompositionTarget.idealRectFor(
            CompositionThresholds.AESTHETIC_FULL_BODY,
            subjectAt(NormalizedRect(0.7f, 0.4f, 0.85f, 0.8f)), // cx ~0.775
        )
        assertEquals(CompositionMetrics.THIRD_HIGH, right.centerX, 0.03f)

        val left = CompositionTarget.idealRectFor(
            CompositionThresholds.AESTHETIC_FULL_BODY,
            subjectAt(NormalizedRect(0.15f, 0.4f, 0.3f, 0.8f)), // cx ~0.225
        )
        assertEquals(CompositionMetrics.THIRD_LOW, left.centerX, 0.03f)
    }

    @Test
    fun target_staysWithinFrame() {
        val t = CompositionTarget.idealRect(CompositionThresholds.LOW_ANGLE, preferLeftThird = true)
        assertTrue(t.left >= 0f)
        assertTrue(t.top >= 0f)
        assertTrue(t.right <= 1f)
        assertTrue(t.bottom <= 1f)
    }

    @Test
    fun offsetToTarget_pointsFromSubjectToTarget() {
        val target = NormalizedRect(0.25f, 0.4f, 0.45f, 0.8f) // center (0.35, 0.6)
        val subject = subjectAt(NormalizedRect(0.5f, 0.4f, 0.7f, 0.8f)) // center (0.6, 0.6)
        val off = CompositionTarget.offsetToTarget(subject, target)!!
        assertEquals(-0.25f, off.x, eps) // move left
        assertEquals(0f, off.y, eps)
    }

    @Test
    fun isAligned_trueWhenSubjectMatchesTarget() {
        val target = CompositionTarget.idealRect(CompositionThresholds.AESTHETIC_FULL_BODY, true)
        // A subject essentially equal to the target is aligned.
        val subject = subjectAt(target)
        assertTrue(CompositionTarget.isAligned(subject, target))
    }

    @Test
    fun isAligned_falseWhenFarOff() {
        val target = CompositionTarget.idealRect(CompositionThresholds.AESTHETIC_FULL_BODY, true)
        val subject = subjectAt(NormalizedRect(0.7f, 0.05f, 0.95f, 0.3f)) // far, high, small
        assertFalse(CompositionTarget.isAligned(subject, target))
    }

    @Test
    fun noSubject_offsetAndAlignmentHandleGracefully() {
        val target = CompositionTarget.idealRect(CompositionThresholds.AESTHETIC_FULL_BODY, true)
        assertEquals(null, CompositionTarget.offsetToTarget(SubjectFrame.empty(0L), target))
        assertFalse(CompositionTarget.isAligned(SubjectFrame.empty(0L), target))
    }
}
