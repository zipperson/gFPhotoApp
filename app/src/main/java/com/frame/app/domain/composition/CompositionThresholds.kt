package com.frame.app.domain.composition

/**
 * Tunable thresholds for composition evaluation, gathered in one place so the
 * on-device tuning pass (Phase 4 Task 5) can adjust feel without hunting through
 * logic. All values are in normalized frame units unless noted.
 *
 * Thresholds are style-dependent, so they come as named presets. FRAME's
 * primary use case is the "TikTok aesthetic full-body selfie" look, so
 * [AESTHETIC_FULL_BODY] is the default: subject smaller in frame, generous
 * headroom/negative space, placed off-center on a vertical third, shot from a
 * lowish angle. [CLASSIC_PORTRAIT] keeps conventional tight-portrait rules for
 * headshots / other styles.
 *
 * These encode guidance as *defaults*, not absolutes — composition is
 * subjective and the UI lets the user ignore the advice.
 */
data class CompositionThresholds(
    /** Ideal headroom range (fraction of frame above the head). */
    val headroomMin: Float,
    val headroomMax: Float,
    /** Headroom above this reads as "too much empty space at top". */
    val headroomTooMuch: Float,

    /** Max distance from a vertical third line to count placement as "good". */
    val thirdsTolerance: Float,
    /** Within this of dead-center, treat centered composition as intentional. */
    val centerTolerance: Float,

    /** Subject size (larger dimension) below this reads as "too far / too small". */
    val sizeTooSmall: Float,
    /** Subject size above this reads as "too close / too tight". */
    val sizeTooLarge: Float,
    /** Preferred subject-size band (for gentle "good framing" scoring). */
    val sizeIdealMin: Float,
    val sizeIdealMax: Float,

    /** Roll within this (degrees) counts as level. */
    val levelToleranceDegrees: Float,

    /**
     * Reward placing the subject low in the frame (feet toward the bottom), as
     * in full-body aesthetic shots. When true, a low vertical center is good and
     * a high one is nudged. When false (classic), vertical placement is neutral.
     */
    val preferLowSubject: Boolean,
    /** Target vertical center for the subject when [preferLowSubject]. */
    val idealVerticalCenter: Float,
) {
    companion object {
        /**
         * TikTok-style full-body aesthetic (FRAME's default). Tuned so the look
         * is rewarded, not flagged:
         *  - subject can be fairly small (lots of environment is intentional)
         *  - generous headroom/negative space is good (no "too much space" nag
         *    until it's extreme)
         *  - off-center-on-a-third placement encouraged
         *  - subject sitting low in the frame is preferred (long-legs low angle)
         */
        val AESTHETIC_FULL_BODY = CompositionThresholds(
            headroomMin = 0.04f,
            headroomMax = 0.25f,
            headroomTooMuch = 0.45f,       // airy space above is part of the look
            thirdsTolerance = 0.10f,
            centerTolerance = 0.05f,       // centered is allowed but thirds favored
            sizeTooSmall = 0.12f,          // small subject is fine / intentional
            sizeTooLarge = 0.85f,          // flag only genuinely cramped tight shots
            sizeIdealMin = 0.35f,
            sizeIdealMax = 0.75f,
            levelToleranceDegrees = 4.0f,  // slightly looser; horizon still matters
            preferLowSubject = true,
            idealVerticalCenter = 0.6f,    // subject centered a bit below middle
        )

        /** Conventional tight-portrait rules (headshots / non-aesthetic use). */
        val CLASSIC_PORTRAIT = CompositionThresholds(
            headroomMin = 0.03f,
            headroomMax = 0.18f,
            headroomTooMuch = 0.30f,
            thirdsTolerance = 0.08f,
            centerTolerance = 0.06f,
            sizeTooSmall = 0.25f,
            sizeTooLarge = 0.92f,
            sizeIdealMin = 0.4f,
            sizeIdealMax = 0.85f,
            levelToleranceDegrees = 3.0f,
            preferLowSubject = false,
            idealVerticalCenter = 0.5f,
        )

        /** Default used by the engine. */
        val DEFAULT = AESTHETIC_FULL_BODY
    }
}
