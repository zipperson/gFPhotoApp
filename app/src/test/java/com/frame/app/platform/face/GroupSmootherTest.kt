package com.frame.app.platform.face

import com.frame.app.domain.composition.GroupFraming
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupSmootherTest {

    private fun frame(vararg faces: NormalizedRect, t: Long = 0L) =
        GroupFraming.toSubjectFrame(faces.toList(), t)

    private fun face(left: Float, top: Float = 0.2f) = NormalizedRect(left, top, left + 0.1f, top + 0.1f)

    @Test
    fun firstDetection_isAdoptedAsIs() {
        val smoother = GroupSmoother()
        val out = smoother.process(frame(face(0.3f), face(0.6f)))
        assertEquals(2, out.group!!.count)
        assertEquals(0.3f, out.group!!.faces[0].left, 1e-4f)
    }

    @Test
    fun smallMovement_isIgnoredAsNoise() {
        val smoother = GroupSmoother()
        smoother.process(frame(face(0.3f), face(0.6f)))
        val out = smoother.process(frame(face(0.304f), face(0.604f)))
        assertEquals(0.3f, out.group!!.faces[0].left, 1e-4f)
    }

    @Test
    fun largeMovement_isEasedNotJumped() {
        val smoother = GroupSmoother(smoothingFactor = 0.5f)
        smoother.process(frame(face(0.3f), face(0.6f)))
        val out = smoother.process(frame(face(0.5f), face(0.6f)))
        assertEquals(0.4f, out.group!!.faces[0].left, 1e-4f)
    }

    @Test
    fun faceCountChange_adoptsNewDetectionImmediately() {
        val smoother = GroupSmoother()
        smoother.process(frame(face(0.3f), face(0.6f)))
        val out = smoother.process(frame(face(0.3f), face(0.6f), face(0.8f)))
        assertEquals(3, out.group!!.count)
        assertEquals(0.8f, out.group!!.faces[2].left, 1e-4f)
    }

    @Test
    fun briefDropout_holdsLastGroup_thenClears() {
        val smoother = GroupSmoother(holdFrames = 2)
        smoother.process(frame(face(0.3f), face(0.6f)))
        assertTrue(smoother.process(SubjectFrame.empty(1L)).hasSubject)
        assertTrue(smoother.process(SubjectFrame.empty(2L)).hasSubject)
        assertFalse(smoother.process(SubjectFrame.empty(3L)).hasSubject)
    }

    @Test
    fun reset_forgetsTheGroup() {
        val smoother = GroupSmoother()
        smoother.process(frame(face(0.3f)))
        smoother.reset()
        assertFalse(smoother.process(SubjectFrame.empty(1L)).hasSubject)
    }
}
