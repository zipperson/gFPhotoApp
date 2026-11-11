package com.frame.app.domain.service

import com.frame.app.domain.model.CompositionResult
import com.frame.app.domain.model.ReferenceProfile
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState

/**
 * Pure composition logic: given what the camera currently sees (a detected
 * subject + device tilt), produce an advisory [CompositionResult].
 *
 * Intentionally free of Android types so it is unit-testable in isolation and
 * portable to a future non-Android target. The real implementation arrives in
 * Phase 4; Phase 1 ships a no-op so the pipeline is wired end to end.
 */
interface CompositionEngine {

    /**
     * Evaluate free-form composition (no reference), e.g. rule-of-thirds,
     * headroom, framing, tilt.
     *
     * @param subject   latest subject detection (may have no subject)
     * @param tilt      current device tilt
     * @param frameAspectRatio preview aspect ratio (width / height)
     */
    fun evaluate(
        subject: SubjectFrame,
        tilt: TiltState,
        frameAspectRatio: Float,
    ): CompositionResult

    /**
     * Evaluate the live frame against a target [ReferenceProfile] (Phase 5),
     * producing suggestions that move the user toward the reference composition.
     */
    fun evaluateAgainstReference(
        subject: SubjectFrame,
        tilt: TiltState,
        frameAspectRatio: Float,
        reference: ReferenceProfile,
    ): CompositionResult
}

/**
 * Phase 1 placeholder: performs no analysis and returns [CompositionResult.NONE].
 * Lets the camera/overlay pipeline run end to end before the real engine lands.
 */
class NoOpCompositionEngine : CompositionEngine {
    override fun evaluate(
        subject: SubjectFrame,
        tilt: TiltState,
        frameAspectRatio: Float,
    ): CompositionResult = CompositionResult.NONE

    override fun evaluateAgainstReference(
        subject: SubjectFrame,
        tilt: TiltState,
        frameAspectRatio: Float,
        reference: ReferenceProfile,
    ): CompositionResult = CompositionResult.NONE
}
