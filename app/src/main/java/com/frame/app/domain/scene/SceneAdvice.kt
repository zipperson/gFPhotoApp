package com.frame.app.domain.scene

/**
 * AI scene-analysis result: higher-level, natural-language guidance that
 * complements the real-time rule engine. Produced by a [SceneAnalyzer] from a
 * single captured frame on demand (never in the live loop).
 *
 * Fields are optional because a model may not always speak to every dimension;
 * [rawText] always holds the full response so nothing is lost.
 */
data class SceneAdvice(
    /** Primary "move the camera / reposition" guidance. */
    val cameraMovement: String?,
    /** Lighting observations (direction, harshness, golden-hour, backlight...). */
    val lighting: String?,
    /** Background / environment notes (clutter, leading lines, framing elements). */
    val background: String?,
    /** One-line overall suggestion / mood read. */
    val overall: String?,
    /**
     * Where the AI suggests the subject should sit, in normalized frame coords
     * [0,1] (left, top, right, bottom). Null when the model didn't provide one.
     * Rendered on the overlay as a target zone so the advice is visual, not just
     * text.
     */
    val suggestedTarget: SuggestedTarget?,
    /** Full model response text, for display fallback / debugging. */
    val rawText: String,
)

/** Normalized target rectangle (0..1) the AI suggests for the subject. */
data class SuggestedTarget(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/**
 * Outcome of a scene-analysis request. Models the states the UI must handle:
 * success, a recoverable error (with a user-facing message), and offline (the
 * hybrid contract — the app works without this, and says so clearly).
 */
sealed interface SceneAnalysisResult {
    data class Success(val advice: SceneAdvice) : SceneAnalysisResult
    data class Error(val message: String) : SceneAnalysisResult
    data object Offline : SceneAnalysisResult
}

/**
 * Context passed alongside the frame so the model's advice complements — rather
 * than repeats — the rule engine. Kept as plain, already-computed summary text.
 *
 * @property styleName    the user's selected photography style
 * @property styleTagline short description of that style's look
 * @property ruleSummary  brief summary of the current rule-engine read
 *                        (e.g. "rating FAIR, score 60; subject centered, tilted"),
 *                        so the LLM can build on it instead of restating it.
 */
data class SceneContext(
    val styleName: String,
    val styleTagline: String,
    val ruleSummary: String,
    /**
     * Concrete, style-specific framing guidance (derived from the style's own
     * composition rules) so the model's advice + suggested target genuinely
     * follow the chosen look rather than generic portraiture.
     */
    val styleBrief: String = "",
)
