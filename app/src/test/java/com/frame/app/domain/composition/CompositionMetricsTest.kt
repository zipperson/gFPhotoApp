package com.frame.app.domain.composition

import com.frame.app.domain.model.LandmarkType
import com.frame.app.domain.model.NormalizedLandmark
import com.frame.app.domain.model.NormalizedPoint
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionMetricsTest {

    private val eps = 1e-4f

    private fun subject(
        box: NormalizedRect,
        types: List<LandmarkType> = emptyList(),
    ): SubjectFrame {
        val lms = types.map {
            NormalizedLandmark(it, NormalizedPoint(box.centerX, box.centerY), 0.9f)
        }
        return SubjectFrame(boundingBox = box, landmarks = lms, timestampMs = 0L)
    }

    @Test
    fun headroom_isTopOfBox() {
        val s = subject(NormalizedRect(0.3f, 0.15f, 0.7f, 0.9f))
        assertEquals(0.15f, CompositionMetrics.headroomRatio(s)!!, eps)
    }

    @Test
    fun headroom_nullWhenNoSubject() {
        assertNull(CompositionMetrics.headroomRatio(SubjectFrame.empty(0L)))
    }

    @Test
    fun horizontalCenter_isBoxCenterX() {
        val s = subject(NormalizedRect(0.2f, 0.1f, 0.6f, 0.9f))
        assertEquals(0.4f, CompositionMetrics.horizontalCenter(s)!!, eps)
    }

    @Test
    fun sizeRatio_isLargerDimension() {
        val s = subject(NormalizedRect(0.4f, 0.1f, 0.6f, 0.9f)) // w=0.2 h=0.8
        assertEquals(0.8f, CompositionMetrics.sizeRatio(s)!!, eps)
    }

    @Test
    fun distanceToThird_zeroWhenOnThirdLine() {
        val s = subject(NormalizedRect(0.28333f, 0.1f, 0.38333f, 0.9f)) // cx = 1/3
        assertEquals(0f, CompositionMetrics.distanceToNearestVerticalThird(s)!!, 1e-3f)
    }

    @Test
    fun distanceToThird_largeWhenCentered() {
        val s = subject(NormalizedRect(0.45f, 0.1f, 0.55f, 0.9f)) // cx = 0.5
        // 0.5 is 1/6 from either third.
        assertEquals(1f / 6f, CompositionMetrics.distanceToNearestVerticalThird(s)!!, 1e-3f)
    }

    @Test
    fun framing_fullBody_whenAnklesPresent() {
        val s = subject(
            NormalizedRect(0.3f, 0.05f, 0.7f, 0.98f),
            types = listOf(
                LandmarkType.NOSE, LandmarkType.LEFT_SHOULDER, LandmarkType.LEFT_HIP,
                LandmarkType.LEFT_KNEE, LandmarkType.LEFT_ANKLE,
            ),
        )
        assertEquals(FramingType.FULL_BODY, CompositionMetrics.framing(s))
    }

    @Test
    fun framing_headshot_whenOnlyHead() {
        val s = subject(NormalizedRect(0.35f, 0.2f, 0.65f, 0.7f), types = listOf(LandmarkType.NOSE))
        assertEquals(FramingType.HEADSHOT, CompositionMetrics.framing(s))
    }

    @Test
    fun framing_upperBody_whenHeadAndShoulders() {
        val s = subject(
            NormalizedRect(0.3f, 0.1f, 0.7f, 0.8f),
            types = listOf(LandmarkType.NOSE, LandmarkType.LEFT_SHOULDER, LandmarkType.RIGHT_SHOULDER),
        )
        assertEquals(FramingType.UPPER_BODY, CompositionMetrics.framing(s))
    }

    @Test
    fun framing_none_whenNoSubject() {
        assertEquals(FramingType.NONE, CompositionMetrics.framing(SubjectFrame.empty(0L)))
    }

    @Test
    fun edgeClipping_detectsEdges() {
        val s = subject(NormalizedRect(0.0f, 0.3f, 0.5f, 0.6f)) // touches left
        val clip = CompositionMetrics.edgeClipping(s)
        assertTrue(clip.left)
        assertFalse(clip.right)
        assertTrue(clip.any)
    }

    @Test
    fun edgeClipping_noneWhenCentered() {
        val s = subject(NormalizedRect(0.3f, 0.3f, 0.7f, 0.7f))
        assertFalse(CompositionMetrics.edgeClipping(s).any)
    }

    @Test
    fun negativeSpace_isComplementOfArea() {
        val s = subject(NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f)) // area 0.25
        assertEquals(0.75f, CompositionMetrics.negativeSpaceRatio(s)!!, eps)
    }
}
