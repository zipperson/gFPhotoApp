package com.frame.app.ui.camera.hud

import com.frame.app.domain.model.CompositionRating
import com.frame.app.domain.model.CompositionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusPillModelTest {

    private fun result(
        overall: CompositionRating,
        score: Int?,
        suggestions: List<String> = emptyList(),
    ) = CompositionResult(
        overall = overall,
        aspects = emptyList(),
        suggestions = suggestions,
        score = score,
    )

    @Test
    fun noSubject_isHidden() {
        val m = CompositionResult.NONE.toStatusPillModel()
        assertFalse(m.visible)
        assertNull(m.score)
    }

    @Test
    fun unknownRating_isHiddenEvenWithScore() {
        assertFalse(result(CompositionRating.UNKNOWN, 50).toStatusPillModel().visible)
    }

    @Test
    fun missingScore_isHidden() {
        assertFalse(result(CompositionRating.FAIR, null).toStatusPillModel().visible)
    }

    @Test
    fun topSuggestionBecomesTheMessage() {
        val m = result(CompositionRating.FAIR, 67, listOf("Move slightly left", "Step back"))
            .toStatusPillModel()
        assertTrue(m.visible)
        assertEquals("Move slightly left", m.message)
        assertEquals(67, m.score)
        assertFalse(m.isGood)
    }

    @Test
    fun goodWithoutSuggestion_praises() {
        val m = result(CompositionRating.GOOD, 92).toStatusPillModel()
        assertEquals("Great composition", m.message)
        assertTrue(m.isGood)
    }

    @Test
    fun poorWithoutSuggestion_encouragesAdjusting() {
        val m = result(CompositionRating.POOR, 30).toStatusPillModel()
        assertEquals("Keep adjusting", m.message)
        assertFalse(m.isGood)
    }

    @Test
    fun goodStillShowsSuggestionIfThereIsOne() {
        val m = result(CompositionRating.GOOD, 85, listOf("Raise your phone slightly"))
            .toStatusPillModel()
        assertEquals("Raise your phone slightly", m.message)
        assertTrue(m.isGood)
    }
}
