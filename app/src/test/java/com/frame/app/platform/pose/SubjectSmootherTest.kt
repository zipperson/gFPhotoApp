package com.frame.app.platform.pose

import com.frame.app.domain.model.LandmarkType
import com.frame.app.domain.model.NormalizedLandmark
import com.frame.app.domain.model.NormalizedPoint
import com.frame.app.domain.model.SubjectFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectSmootherTest {

    private val eps = 1e-4f

    private fun frameWithNose(x: Float, y: Float, ts: Long = 0L): SubjectFrame {
        val lm = NormalizedLandmark(LandmarkType.NOSE, NormalizedPoint(x, y), 0.9f)
        return SubjectFrame(
            boundingBox = PoseMapping.boundingBox(listOf(lm.position)),
            landmarks = listOf(lm),
            timestampMs = ts,
        )
    }

    @Test
    fun firstDetection_passesThroughUnchanged() {
        val s = SubjectSmoother(smoothingFactor = 0.5f)
        val out = s.process(frameWithNose(0.4f, 0.4f))
        assertEquals(0.4f, out.landmark(LandmarkType.NOSE)!!.position.x, eps)
    }

    @Test
    fun secondDetection_isEmaBlended() {
        val s = SubjectSmoother(smoothingFactor = 0.5f)
        s.process(frameWithNose(0.4f, 0.4f))          // seed
        val out = s.process(frameWithNose(0.8f, 0.8f)) // 0.4 + 0.5*(0.8-0.4) = 0.6
        assertEquals(0.6f, out.landmark(LandmarkType.NOSE)!!.position.x, eps)
        assertEquals(0.6f, out.landmark(LandmarkType.NOSE)!!.position.y, eps)
    }

    @Test
    fun higherFactor_isMoreResponsive() {
        val s = SubjectSmoother(smoothingFactor = 0.9f)
        s.process(frameWithNose(0f, 0f))
        val out = s.process(frameWithNose(1f, 1f)) // 0 + 0.9*(1-0) = 0.9
        assertEquals(0.9f, out.landmark(LandmarkType.NOSE)!!.position.x, eps)
    }

    @Test
    fun missWithinHold_keepsLastSubject() {
        val s = SubjectSmoother(holdFrames = 2)
        s.process(frameWithNose(0.5f, 0.5f, ts = 1L))
        val held = s.process(SubjectFrame.empty(2L))
        assertTrue(held.hasSubject)
        assertEquals(0.5f, held.landmark(LandmarkType.NOSE)!!.position.x, eps)
        assertEquals(2L, held.timestampMs) // re-stamped
    }

    @Test
    fun missBeyondHold_emitsEmpty() {
        val s = SubjectSmoother(holdFrames = 1)
        s.process(frameWithNose(0.5f, 0.5f))
        s.process(SubjectFrame.empty(2L)) // 1st miss -> held
        val gone = s.process(SubjectFrame.empty(3L)) // 2nd miss -> empty
        assertFalse(gone.hasSubject)
    }

    @Test
    fun reset_clearsHistory() {
        val s = SubjectSmoother(smoothingFactor = 0.5f)
        s.process(frameWithNose(0.2f, 0.2f))
        s.reset()
        // After reset the next detection is adopted as-is (no blend with 0.2).
        val out = s.process(frameWithNose(0.9f, 0.9f))
        assertEquals(0.9f, out.landmark(LandmarkType.NOSE)!!.position.x, eps)
    }

    @Test
    fun tinyMovementWithinDeadZone_isIgnored() {
        val s = SubjectSmoother(smoothingFactor = 0.5f, deadZone = 0.01f)
        s.process(frameWithNose(0.5f, 0.5f))
        // Move by 0.005 (< dead zone) -> position should stay put.
        val out = s.process(frameWithNose(0.505f, 0.5f))
        assertEquals(0.5f, out.landmark(LandmarkType.NOSE)!!.position.x, eps)
    }

    @Test
    fun movementBeyondDeadZone_isTracked() {
        val s = SubjectSmoother(smoothingFactor = 0.5f, deadZone = 0.01f)
        s.process(frameWithNose(0.5f, 0.5f))
        // Move by 0.2 (> dead zone) -> blends: 0.5 + 0.5*(0.7-0.5) = 0.6
        val out = s.process(frameWithNose(0.7f, 0.5f))
        assertEquals(0.6f, out.landmark(LandmarkType.NOSE)!!.position.x, eps)
    }

    @Test
    fun recoveryAfterHold_resumesBlending() {
        val s = SubjectSmoother(smoothingFactor = 0.5f, holdFrames = 3)
        s.process(frameWithNose(0.4f, 0.4f))
        s.process(SubjectFrame.empty(2L))             // miss, held
        val out = s.process(frameWithNose(0.8f, 0.8f)) // blends from held 0.4 -> 0.6
        assertEquals(0.6f, out.landmark(LandmarkType.NOSE)!!.position.x, eps)
    }
}
