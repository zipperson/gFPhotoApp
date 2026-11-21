package com.frame.app.domain.composition

import com.frame.app.domain.model.AspectVerdict
import com.frame.app.domain.model.CompositionAspect
import com.frame.app.domain.model.CompositionRating
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupCompositionTest {

    private val engine = GroupCompositionEngine()
    private val level = TiltState(rollDegrees = 0f, pitchDegrees = 0f, isAvailable = true)

    private fun face(left: Float, top: Float, size: Float = 0.1f) =
        NormalizedRect(left, top, left + size, top + size)

    private fun group(vararg faces: NormalizedRect) =
        GroupFraming.toSubjectFrame(faces.toList(), 0L)

    // --- GroupFraming ---

    @Test
    fun framing_noFaces_isEmptySubject() {
        val subject = GroupFraming.toSubjectFrame(emptyList(), 5L)
        assertFalse(subject.hasSubject)
        assertNull(subject.group)
    }

    @Test
    fun framing_unionsFaces_sortsLeftToRight_andCounts() {
        val subject = group(face(0.6f, 0.3f), face(0.2f, 0.2f))
        val box = subject.boundingBox!!
        assertEquals(0.2f, box.left, 1e-4f)
        assertEquals(0.7f, box.right, 1e-4f)
        assertEquals(0.2f, box.top, 1e-4f)
        assertEquals(0.4f, box.bottom, 1e-4f)
        assertEquals(2, subject.group!!.count)
        assertEquals(0.2f, subject.group!!.faces.first().left, 1e-4f)
    }

    @Test
    fun framing_countsFacesTouchingTheEdge() {
        val subject = group(face(0.0f, 0.3f), face(0.5f, 0.3f))
        assertEquals(1, subject.group!!.clippedCount)
    }

    // --- GroupCompositionEngine ---

    @Test
    fun engine_noGroup_isUnknown() {
        val result = engine.evaluate(SubjectFrame.empty(0L), level, 9f / 16f)
        assertEquals(CompositionRating.UNKNOWN, result.overall)
        assertNull(result.score)
    }

    @Test
    fun engine_wellFramedCenteredGroup_isGoodWithNoSuggestions() {
        val result = engine.evaluate(group(face(0.3f, 0.2f), face(0.6f, 0.22f)), level, 9f / 16f)
        assertEquals(CompositionRating.GOOD, result.overall)
        assertEquals(100, result.score)
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun engine_cutOffFace_asksToStepBack_first() {
        val result = engine.evaluate(group(face(0.0f, 0.2f), face(0.5f, 0.2f)), level, 9f / 16f)
        assertEquals("Step back to fit everyone", result.suggestions.first())
    }

    @Test
    fun engine_groupOnTheRight_asksToMoveLeft() {
        val result = engine.evaluate(group(face(0.65f, 0.2f), face(0.8f, 0.2f)), level, 9f / 16f)
        assertTrue(result.suggestions.contains("Move slightly left"))
    }

    @Test
    fun engine_tinyFaces_askToMoveCloser() {
        val result = engine.evaluate(
            group(face(0.4f, 0.3f, 0.03f), face(0.55f, 0.3f, 0.03f)), level, 9f / 16f,
        )
        assertTrue(result.suggestions.contains("Move closer"))
    }

    @Test
    fun engine_facesCrowdingTop_asksForHeadroom() {
        val result = engine.evaluate(group(face(0.3f, 0.01f), face(0.6f, 0.01f)), level, 9f / 16f)
        assertTrue(result.suggestions.contains("Leave more space above"))
    }

    @Test
    fun engine_facesTooLow_asksToTiltDown() {
        val result = engine.evaluate(group(face(0.3f, 0.7f), face(0.6f, 0.7f)), level, 9f / 16f)
        assertTrue(result.suggestions.contains("Tilt down to fit bodies"))
    }

    @Test
    fun engine_tiltedCamera_isFlagged() {
        val tilted = TiltState(rollDegrees = 10f, pitchDegrees = 0f, isAvailable = true)
        val result = engine.evaluate(group(face(0.3f, 0.2f), face(0.6f, 0.2f)), tilted, 9f / 16f)
        val tiltCheck = result.aspects.first { it.aspect == CompositionAspect.CAMERA_TILT }
        assertEquals(AspectVerdict.NEEDS_ADJUSTMENT, tiltCheck.verdict)
        assertNotNull(result.suggestions.firstOrNull { it.startsWith("Straighten") })
    }

    @Test
    fun engine_capsSuggestionsAtThree() {
        val tilted = TiltState(rollDegrees = 10f, pitchDegrees = 0f, isAvailable = true)
        // Cut off, off-centre, no headroom, low: many failures at once.
        val result = engine.evaluate(group(face(0.85f, 0.0f, 0.15f), face(0.7f, 0.75f)), tilted, 9f / 16f)
        assertTrue(result.suggestions.size <= 3)
    }
}
