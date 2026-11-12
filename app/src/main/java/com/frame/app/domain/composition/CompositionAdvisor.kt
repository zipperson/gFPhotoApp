package com.frame.app.domain.composition

import com.frame.app.domain.model.AspectCheck
import com.frame.app.domain.model.AspectVerdict
import com.frame.app.domain.model.CompositionAspect
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState

/**
 * Turns failing [AspectCheck]s into concrete, friendly, actionable suggestions
 * (e.g. "Move slightly left", "Leave more space above", "Lower your phone").
 *
 * Pure and unit-testable: direction is derived from the subject's measured
 * metrics, not guessed. Suggestions are prioritized and capped so the UI shows
 * only the most impactful guidance rather than a wall of text.
 *
 * @param thresholds the active style's thresholds, so advice matches the chosen
 *        look (e.g. where "ideal" placement is).
 * @param maxSuggestions cap on how many suggestions to return.
 */
open class CompositionAdvisor(
    private val thresholds: CompositionThresholds = CompositionThresholds.DEFAULT,
    private val maxSuggestions: Int = 3,
) {

    open fun suggest(
        checks: List<AspectCheck>,
        subject: SubjectFrame,
        tilt: TiltState,
    ): List<String> {
        val failing = checks.filter { it.verdict == AspectVerdict.NEEDS_ADJUSTMENT }
        if (failing.isEmpty()) return emptyList()

        // Emit in priority order (most impactful first), then cap.
        return PRIORITY
            .mapNotNull { aspect ->
                failing.firstOrNull { it.aspect == aspect }
                    ?.let { suggestionFor(aspect, subject, tilt) }
            }
            .take(maxSuggestions)
    }

    private fun suggestionFor(
        aspect: CompositionAspect,
        subject: SubjectFrame,
        tilt: TiltState,
    ): String? = when (aspect) {
        CompositionAspect.BODY_FRAMING -> framingAdvice(subject)
        CompositionAspect.SUBJECT_POSITION -> positionAdvice(subject)
        CompositionAspect.HEADROOM -> headroomAdvice(subject)
        CompositionAspect.PERSPECTIVE -> perspectiveAdvice(subject)
        CompositionAspect.CAMERA_TILT -> tiltAdvice(tilt)
        CompositionAspect.NEGATIVE_SPACE -> "Give the subject a little more room"
        else -> null
    }

    private fun framingAdvice(subject: SubjectFrame): String? {
        val size = CompositionMetrics.sizeRatio(subject) ?: return null
        return when {
            size > thresholds.sizeTooLarge -> "Step back to fit the whole shot"
            size < thresholds.sizeTooSmall -> "Move a bit closer"
            else -> "Adjust your framing"
        }
    }

    private fun positionAdvice(subject: SubjectFrame): String? {
        val cx = CompositionMetrics.horizontalCenter(subject) ?: return null
        // Move the subject toward the nearest third line. "Left/right" is from
        // the photographer's view of the frame.
        val target = if (cx < 0.5f) CompositionMetrics.THIRD_LOW else CompositionMetrics.THIRD_HIGH
        return if (cx > target) "Move slightly left" else "Move slightly right"
    }

    private fun headroomAdvice(subject: SubjectFrame): String? {
        val headroom = CompositionMetrics.headroomRatio(subject) ?: return null
        return when {
            headroom < thresholds.headroomMin -> "Leave more space above the head"
            headroom > thresholds.headroomTooMuch -> "Too much space above — tilt down a touch"
            else -> null
        }
    }

    private fun perspectiveAdvice(subject: SubjectFrame): String? {
        val vc = CompositionMetrics.verticalCenter(subject) ?: return null
        return if (vc < thresholds.idealVerticalCenter) {
            "Lower your phone for a taller look"
        } else {
            "Raise your phone slightly"
        }
    }

    private fun tiltAdvice(tilt: TiltState): String? {
        if (!tilt.isAvailable) return null
        // Positive roll = rolled clockwise (right edge down) in our convention.
        return if (tilt.rollDegrees > 0f) {
            "Straighten the camera (tilting right)"
        } else {
            "Straighten the camera (tilting left)"
        }
    }

    private companion object {
        /** Most impactful first: fix how much of the subject is in frame and
         *  where they sit before fine details like negative space. */
        val PRIORITY = listOf(
            CompositionAspect.BODY_FRAMING,
            CompositionAspect.SUBJECT_POSITION,
            CompositionAspect.HEADROOM,
            CompositionAspect.PERSPECTIVE,
            CompositionAspect.CAMERA_TILT,
            CompositionAspect.NEGATIVE_SPACE,
        )
    }
}
