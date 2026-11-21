package com.frame.app.domain.composition

import com.frame.app.domain.model.AspectCheck
import com.frame.app.domain.model.AspectVerdict
import com.frame.app.domain.model.CompositionRating

/**
 * Turns a list of [AspectCheck]s into a 0..100 score and an overall rating.
 * Shared by the single-person and group engines so both are scored the same way.
 */
internal object CompositionScoring {

    /**
     * Fraction of applicable checks that are GOOD, scaled to 0..100.
     * NOT_APPLICABLE checks are excluded so they neither help nor hurt.
     */
    fun scoreFrom(checks: List<AspectCheck>): Int {
        val applicable = checks.filter { it.verdict != AspectVerdict.NOT_APPLICABLE }
        if (applicable.isEmpty()) return 0
        val good = applicable.count { it.verdict == AspectVerdict.GOOD }
        return ((good.toFloat() / applicable.size) * 100f).toInt()
    }

    fun ratingFrom(score: Int): CompositionRating = when {
        score >= 80 -> CompositionRating.GOOD
        score >= 50 -> CompositionRating.FAIR
        else -> CompositionRating.POOR
    }
}
