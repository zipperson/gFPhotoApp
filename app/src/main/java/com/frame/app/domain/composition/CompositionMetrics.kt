package com.frame.app.domain.composition

import com.frame.app.domain.model.LandmarkType
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame
import kotlin.math.abs
import kotlin.math.min

/**
 * Pure, measurable composition metrics extracted from a detected [SubjectFrame].
 *
 * These are facts, not judgments — the [com.frame.app.domain.service.CompositionEngine]
 * turns them into verdicts/scores. Keeping them here (domain, no Android) makes
 * every rule unit-testable with hand-built frames and keeps photography opinions
 * out of the measurement layer.
 */
object CompositionMetrics {

    /**
     * Headroom: normalized vertical space between the top of frame and the
     * highest subject point (head). 0 = head touches the top edge; larger =
     * more empty space above. Null when no subject.
     */
    fun headroomRatio(subject: SubjectFrame): Float? {
        val box = subject.boundingBox ?: return null
        return box.top.coerceIn(0f, 1f)
    }

    /**
     * Horizontal placement of the subject's center, normalized 0..1.
     * Null when no subject.
     */
    fun horizontalCenter(subject: SubjectFrame): Float? =
        subject.boundingBox?.centerX

    /** Vertical placement of the subject's center, normalized 0..1. */
    fun verticalCenter(subject: SubjectFrame): Float? =
        subject.boundingBox?.centerY

    /**
     * Subject size as the larger of its normalized width/height — a proxy for
     * "how much of the frame the subject fills". Null when no subject.
     */
    fun sizeRatio(subject: SubjectFrame): Float? {
        val box = subject.boundingBox ?: return null
        return maxOf(box.width, box.height).coerceIn(0f, 1f)
    }

    /**
     * Distance of the subject center from the nearest vertical rule-of-thirds
     * line (x = 1/3 or 2/3), normalized. 0 = exactly on a third line. Lower is
     * better for thirds-based placement. Null when no subject.
     */
    fun distanceToNearestVerticalThird(subject: SubjectFrame): Float? {
        val cx = horizontalCenter(subject) ?: return null
        return min(abs(cx - THIRD_LOW), abs(cx - THIRD_HIGH))
    }

    /** Distance of subject center from exact horizontal center (x=0.5). */
    fun distanceFromCenterX(subject: SubjectFrame): Float? {
        val cx = horizontalCenter(subject) ?: return null
        return abs(cx - 0.5f)
    }

    /**
     * Classify the framing based on which body landmarks are present and the
     * subject box height. Used to detect "too tight" vs full-body framing.
     */
    fun framing(subject: SubjectFrame): FramingType {
        if (!subject.hasSubject) return FramingType.NONE
        val hasHead = subject.landmark(LandmarkType.NOSE) != null ||
            subject.landmark(LandmarkType.LEFT_EYE) != null ||
            subject.landmark(LandmarkType.RIGHT_EYE) != null
        val hasShoulders = subject.landmark(LandmarkType.LEFT_SHOULDER) != null ||
            subject.landmark(LandmarkType.RIGHT_SHOULDER) != null
        val hasHips = subject.landmark(LandmarkType.LEFT_HIP) != null ||
            subject.landmark(LandmarkType.RIGHT_HIP) != null
        val hasKnees = subject.landmark(LandmarkType.LEFT_KNEE) != null ||
            subject.landmark(LandmarkType.RIGHT_KNEE) != null
        val hasAnkles = subject.landmark(LandmarkType.LEFT_ANKLE) != null ||
            subject.landmark(LandmarkType.RIGHT_ANKLE) != null

        return when {
            hasAnkles && hasKnees && hasHips -> FramingType.FULL_BODY
            hasHips -> FramingType.THREE_QUARTER
            hasShoulders && hasHead -> FramingType.UPPER_BODY
            hasHead -> FramingType.HEADSHOT
            else -> FramingType.PARTIAL
        }
    }

    /**
     * True when the subject box is clipped against a frame edge (any side within
     * [edgeThreshold] of the boundary), indicating the subject may be cut off.
     */
    fun edgeClipping(subject: SubjectFrame, edgeThreshold: Float = EDGE_THRESHOLD): EdgeClipping {
        val box = subject.boundingBox ?: return EdgeClipping.NONE_DETECTED
        return EdgeClipping(
            left = box.left <= edgeThreshold,
            right = box.right >= 1f - edgeThreshold,
            top = box.top <= edgeThreshold,
            bottom = box.bottom >= 1f - edgeThreshold,
            hasSubject = true,
        )
    }

    /** Negative space: fraction of the frame NOT occupied by the subject box. */
    fun negativeSpaceRatio(subject: SubjectFrame): Float? {
        val box = subject.boundingBox ?: return null
        return (1f - box.area).coerceIn(0f, 1f)
    }

    const val THIRD_LOW = 1f / 3f
    const val THIRD_HIGH = 2f / 3f
    private const val EDGE_THRESHOLD = 0.02f
}

/** Coarse framing classification from visible landmarks. */
enum class FramingType {
    NONE,
    HEADSHOT,       // head only
    UPPER_BODY,     // head + shoulders
    THREE_QUARTER,  // down to hips
    FULL_BODY,      // down to ankles
    PARTIAL,        // some landmarks but no clear head
}

/** Which frame edges the subject box is touching. */
data class EdgeClipping(
    val left: Boolean = false,
    val right: Boolean = false,
    val top: Boolean = false,
    val bottom: Boolean = false,
    val hasSubject: Boolean = false,
) {
    val any: Boolean get() = left || right || top || bottom

    companion object {
        val NONE_DETECTED = EdgeClipping()
    }
}
