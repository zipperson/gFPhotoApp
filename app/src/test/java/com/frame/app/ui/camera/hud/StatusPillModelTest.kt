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
    fun groupMode_goodLineNamesThePeople() {
        assertEquals(
            "Great group shot · 3 people",
            result(CompositionRating.GOOD, 100).toStatusPillModel(peopleCount = 3).message,
        )
        assertEquals(
            "Great group shot · 1 person",
            result(CompositionRating.GOOD, 100).toStatusPillModel(peopleCount = 1).message,
        )
    }

    @Test
    fun groupMode_suggestionStillWinsOverTheCountLine() {
        val m = result(CompositionRating.FAIR, 60, listOf("Step back to fit everyone"))
            .toStatusPillModel(peopleCount = 4)
        assertEquals("Step back to fit everyone", m.message)
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
