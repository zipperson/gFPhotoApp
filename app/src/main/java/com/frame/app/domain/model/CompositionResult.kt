package com.frame.app.domain.model

/**
 * The output of the [com.frame.app.domain.service.CompositionEngine] for a
 * single analyzed frame: an overall read plus per-aspect checks and concrete,
 * human-readable suggestions.
 *
 * Composition is subjective. These results are advisory: the UI must let the
 * user ignore them, and the [score] is derived from measurable criteria
 * (positions, ratios, angles), never an opaque judgment.
 */
data class CompositionResult(
    val overall: CompositionRating,
    val aspects: List<AspectCheck>,
    val suggestions: List<String>,
    /** 0..100, derived from the aspect checks. Null when no subject is present. */
    val score: Int?,
) {
    companion object {
        /** Neutral result used before any analysis has run (e.g. Phase 1). */
        val NONE = CompositionResult(
            overall = CompositionRating.UNKNOWN,
            aspects = emptyList(),
            suggestions = emptyList(),
            score = null,
        )
    }
}

/** A coarse overall read of the current composition. */
enum class CompositionRating {
    UNKNOWN,
    POOR,
    FAIR,
    GOOD,
}

/** The individual composition dimensions FRAME evaluates. */
enum class CompositionAspect {
    SUBJECT_POSITION,
    HEADROOM,
    BODY_FRAMING,
    CAMERA_ANGLE,
    CAMERA_TILT,
    SUBJECT_BACKGROUND,
    NEGATIVE_SPACE,
    SYMMETRY,
    PERSPECTIVE,
    LEADING_LINES,
}

/** Verdict for a single [CompositionAspect]. */
enum class AspectVerdict {
    GOOD,
    NEEDS_ADJUSTMENT,
    NOT_APPLICABLE,
}

/**
 * One evaluated composition dimension.
 *
 * @property aspect  which dimension this is
 * @property verdict the result of the check
 * @property detail  optional short explanation shown to the user
 */
data class AspectCheck(
    val aspect: CompositionAspect,
    val verdict: AspectVerdict,
    val detail: String? = null,
)
