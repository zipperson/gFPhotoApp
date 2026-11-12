package com.frame.app.domain.composition

import com.frame.app.domain.model.AspectCheck
import com.frame.app.domain.model.AspectVerdict
import com.frame.app.domain.model.CompositionAspect
import com.frame.app.domain.model.CompositionRating
import com.frame.app.domain.model.CompositionResult
import com.frame.app.domain.model.ReferenceProfile
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState
import com.frame.app.domain.service.CompositionEngine
import kotlin.math.abs

/**
 * Real composition engine: evaluates measurable [CompositionMetrics] against
 * conventional photographic guidance ([CompositionThresholds]) to produce
 * per-aspect verdicts and a 0..100 score.
 *
 * Pure and Android-free, so every rule is unit-testable. The score is derived
 * from the aspect checks (count of GOOD vs NEEDS_ADJUSTMENT, weighted), never an
 * opaque judgment — matching the spec's requirement that scoring be measurable.
 *
 * Recommendation *text* is produced by [CompositionAdvisor] (Task 3); this
 * engine attaches short [AspectCheck.detail] notes and leaves [CompositionResult.suggestions]
 * to be filled by the advisor wiring.
 */
class DefaultCompositionEngine(
    private val thresholds: CompositionThresholds = CompositionThresholds.DEFAULT,
    private val advisor: CompositionAdvisor = CompositionAdvisor(),
) : CompositionEngine {

    override fun evaluate(
        subject: SubjectFrame,
        tilt: TiltState,
        frameAspectRatio: Float,
    ): CompositionResult {
        if (!subject.hasSubject) {
            // No subject: still report camera tilt (useful without a person),
            // but no overall subject-composition score.
            val tiltCheck = evaluateTilt(tilt)
            return CompositionResult(
                overall = CompositionRating.UNKNOWN,
                aspects = listOfNotNull(tiltCheck),
                suggestions = emptyList(),
                score = null,
            )
        }

        val checks = buildList {
            add(evaluateSubjectPosition(subject))
            add(evaluateHeadroom(subject))
            add(evaluateBodyFraming(subject))
            evaluateTilt(tilt)?.let { add(it) }
            add(evaluateNegativeSpace(subject))
            // Only an active criterion for styles that prefer a low subject
            // (e.g. full-body aesthetic). Classic portrait leaves it out.
            if (thresholds.preferLowSubject) add(evaluateVerticalPlacement(subject))
        }

        val score = scoreFrom(checks)
        val result = CompositionResult(
            overall = ratingFrom(score),
            aspects = checks,
            suggestions = emptyList(),
            score = score,
        )
        // Advisor turns failing checks into prioritized suggestion text (Task 3).
        return result.copy(suggestions = advisor.suggest(checks, subject, tilt))
    }

    override fun evaluateAgainstReference(
        subject: SubjectFrame,
        tilt: TiltState,
        frameAspectRatio: Float,
        reference: ReferenceProfile,
    ): CompositionResult {
        // Phase 5: compare against a reference profile. For now, free-form eval.
        return evaluate(subject, tilt, frameAspectRatio)
    }

    // --- Per-aspect evaluation ---

    private fun evaluateSubjectPosition(subject: SubjectFrame): AspectCheck {
        val distThird = CompositionMetrics.distanceToNearestVerticalThird(subject)
        val distCenter = CompositionMetrics.distanceFromCenterX(subject)
        val onThird = distThird != null && distThird <= thresholds.thirdsTolerance
        val centered = distCenter != null && distCenter <= thresholds.centerTolerance

        val verdict = if (onThird || centered) AspectVerdict.GOOD else AspectVerdict.NEEDS_ADJUSTMENT
        return AspectCheck(
            aspect = CompositionAspect.SUBJECT_POSITION,
            verdict = verdict,
            detail = if (verdict == AspectVerdict.GOOD) {
                if (centered) "Centered" else "On the thirds"
            } else "Off the thirds",
        )
    }

    private fun evaluateHeadroom(subject: SubjectFrame): AspectCheck {
        val headroom = CompositionMetrics.headroomRatio(subject)
            ?: return AspectCheck(CompositionAspect.HEADROOM, AspectVerdict.NOT_APPLICABLE)

        // Only meaningful when the head is actually in frame (framing includes head).
        val framing = CompositionMetrics.framing(subject)
        if (framing == FramingType.PARTIAL) {
            return AspectCheck(CompositionAspect.HEADROOM, AspectVerdict.NOT_APPLICABLE)
        }

        val verdict = when {
            headroom < thresholds.headroomMin -> AspectVerdict.NEEDS_ADJUSTMENT // cropped head
            headroom > thresholds.headroomTooMuch -> AspectVerdict.NEEDS_ADJUSTMENT // too much
            else -> AspectVerdict.GOOD
        }
        val detail = when {
            headroom < thresholds.headroomMin -> "Head too close to the top"
            headroom > thresholds.headroomTooMuch -> "Too much space above"
            else -> "Good headroom"
        }
        return AspectCheck(CompositionAspect.HEADROOM, verdict, detail)
    }

    private fun evaluateBodyFraming(subject: SubjectFrame): AspectCheck {
        val size = CompositionMetrics.sizeRatio(subject)
            ?: return AspectCheck(CompositionAspect.BODY_FRAMING, AspectVerdict.NOT_APPLICABLE)
        val clip = CompositionMetrics.edgeClipping(subject)

        val verdict = when {
            size > thresholds.sizeTooLarge -> AspectVerdict.NEEDS_ADJUSTMENT // too tight
            size < thresholds.sizeTooSmall -> AspectVerdict.NEEDS_ADJUSTMENT // too far
            // Clipping left/right of the subject often means a cut-off body.
            clip.left && clip.right -> AspectVerdict.NEEDS_ADJUSTMENT
            else -> AspectVerdict.GOOD
        }
        val detail = when {
            size > thresholds.sizeTooLarge -> "Framing too tight"
            size < thresholds.sizeTooSmall -> "Subject too small"
            else -> "Well framed"
        }
        return AspectCheck(CompositionAspect.BODY_FRAMING, verdict, detail)
    }

    private fun evaluateTilt(tilt: TiltState): AspectCheck? {
        if (!tilt.isAvailable) return null
        val level = abs(tilt.rollDegrees) <= thresholds.levelToleranceDegrees
        return AspectCheck(
            aspect = CompositionAspect.CAMERA_TILT,
            verdict = if (level) AspectVerdict.GOOD else AspectVerdict.NEEDS_ADJUSTMENT,
            detail = if (level) "Level" else "Camera tilted",
        )
    }

    /**
     * For low-subject aesthetic styles: reward the subject sitting a bit below
     * center (long-legs, environment-above look). Reported under PERSPECTIVE,
     * which best captures the low-angle/placement feel.
     */
    private fun evaluateVerticalPlacement(subject: SubjectFrame): AspectCheck {
        val vc = CompositionMetrics.verticalCenter(subject)
            ?: return AspectCheck(CompositionAspect.PERSPECTIVE, AspectVerdict.NOT_APPLICABLE)
        val off = abs(vc - thresholds.idealVerticalCenter)
        val verdict = if (off <= VERTICAL_TOLERANCE) AspectVerdict.GOOD else AspectVerdict.NEEDS_ADJUSTMENT
        val detail = when {
            verdict == AspectVerdict.GOOD -> "Nice low placement"
            vc < thresholds.idealVerticalCenter -> "Subject sits high"
            else -> "Subject sits low"
        }
        return AspectCheck(CompositionAspect.PERSPECTIVE, verdict, detail)
    }

    private fun evaluateNegativeSpace(subject: SubjectFrame): AspectCheck {
        val neg = CompositionMetrics.negativeSpaceRatio(subject)
            ?: return AspectCheck(CompositionAspect.NEGATIVE_SPACE, AspectVerdict.NOT_APPLICABLE)
        // Very little negative space = cramped; some is desirable. This is a soft
        // check that mostly nudges the score rather than driving hard advice.
        val verdict = if (neg < 0.15f) AspectVerdict.NEEDS_ADJUSTMENT else AspectVerdict.GOOD
        return AspectCheck(
            aspect = CompositionAspect.NEGATIVE_SPACE,
            verdict = verdict,
            detail = if (verdict == AspectVerdict.GOOD) "Balanced space" else "Cramped",
        )
    }

    // --- Scoring ---

    /**
     * Score = fraction of applicable checks that are GOOD, scaled to 0..100.
     * NOT_APPLICABLE checks are excluded so they neither help nor hurt. This is
     * fully determined by the measurable verdicts.
     */
    private fun scoreFrom(checks: List<AspectCheck>): Int {
        val applicable = checks.filter { it.verdict != AspectVerdict.NOT_APPLICABLE }
        if (applicable.isEmpty()) return 0
        val good = applicable.count { it.verdict == AspectVerdict.GOOD }
        return ((good.toFloat() / applicable.size) * 100f).toInt()
    }

    private fun ratingFrom(score: Int): CompositionRating = when {
        score >= 80 -> CompositionRating.GOOD
        score >= 50 -> CompositionRating.FAIR
        else -> CompositionRating.POOR
    }

    private companion object {
        const val VERTICAL_TOLERANCE = 0.15f
    }
}
