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

/** Tunable numbers for group framing. All values are fractions of the frame. */
data class GroupThresholds(
    /** Tallest face, as a fraction of frame height: below = too far, above = too tight. */
    val faceHeightMin: Float = 0.05f,
    val faceHeightMax: Float = 0.30f,
    /** How far the group's horizontal centre may sit from the middle. */
    val centerTolerance: Float = 0.12f,
    /** Space above the highest face. */
    val headroomMin: Float = 0.04f,
    val headroomMax: Float = 0.38f,
    /** Lowest face edge; lower than this leaves no room for bodies. */
    val faceBottomMax: Float = 0.72f,
    val levelToleranceDegrees: Float = 3f,
)

/**
 * Composition checks for group photos, based on face positions only (no body
 * pose). It answers practical group-shot questions: is everyone in the frame,
 * is the group centred, is there room above and below the faces, and are we at
 * a sensible distance.
 *
 * Pure and Android-free. Scoring is shared with the single-person engine.
 */
class GroupCompositionEngine(
    private val thresholds: GroupThresholds = GroupThresholds(),
) : CompositionEngine {

    override fun evaluate(
        subject: SubjectFrame,
        tilt: TiltState,
        frameAspectRatio: Float,
    ): CompositionResult {
        val group = subject.group
        val box = subject.boundingBox
        if (group == null || group.faces.isEmpty() || box == null) {
            return CompositionResult(
                overall = CompositionRating.UNKNOWN,
                aspects = listOfNotNull(tiltCheck(tilt)),
                suggestions = emptyList(),
                score = null,
            )
        }

        val tallestFace = group.faces.maxOf { it.height }
        val checks = listOf(
            framingCheck(group.clippedCount, tallestFace),
            positionCheck(box.centerX),
            headroomCheck(box.top),
            bodyRoomCheck(box.bottom),
        ) + listOfNotNull(tiltCheck(tilt))

        val score = CompositionScoring.scoreFrom(checks)
        return CompositionResult(
            overall = CompositionScoring.ratingFrom(score),
            aspects = checks,
            suggestions = suggestionsFor(checks, tilt),
            score = score,
        )
    }

    override fun evaluateAgainstReference(
        subject: SubjectFrame,
        tilt: TiltState,
        frameAspectRatio: Float,
        reference: ReferenceProfile,
    ): CompositionResult = evaluate(subject, tilt, frameAspectRatio)

    // --- Checks. `detail` carries the suggestion text when a check fails. ---

    private fun framingCheck(clipped: Int, tallestFace: Float): AspectCheck {
        val (verdict, detail) = when {
            clipped > 0 -> AspectVerdict.NEEDS_ADJUSTMENT to "Step back to fit everyone"
            tallestFace > thresholds.faceHeightMax -> AspectVerdict.NEEDS_ADJUSTMENT to "Step back a little"
            tallestFace < thresholds.faceHeightMin -> AspectVerdict.NEEDS_ADJUSTMENT to "Move closer"
            else -> AspectVerdict.GOOD to "Everyone is in frame"
        }
        return AspectCheck(CompositionAspect.BODY_FRAMING, verdict, detail)
    }

    private fun positionCheck(centerX: Float): AspectCheck {
        val off = centerX - 0.5f
        return if (abs(off) <= thresholds.centerTolerance) {
            AspectCheck(CompositionAspect.SUBJECT_POSITION, AspectVerdict.GOOD, "Group is centered")
        } else {
            AspectCheck(
                CompositionAspect.SUBJECT_POSITION,
                AspectVerdict.NEEDS_ADJUSTMENT,
                // Same wording convention as the single-person advice.
                if (off > 0f) "Move slightly left" else "Move slightly right",
            )
        }
    }

    private fun headroomCheck(top: Float): AspectCheck = when {
        top < thresholds.headroomMin ->
            AspectCheck(CompositionAspect.HEADROOM, AspectVerdict.NEEDS_ADJUSTMENT, "Leave more space above")
        top > thresholds.headroomMax ->
            AspectCheck(CompositionAspect.HEADROOM, AspectVerdict.NEEDS_ADJUSTMENT, "Tilt down a touch")
        else -> AspectCheck(CompositionAspect.HEADROOM, AspectVerdict.GOOD, "Good headroom")
    }

    private fun bodyRoomCheck(faceBottom: Float): AspectCheck =
        if (faceBottom > thresholds.faceBottomMax) {
            AspectCheck(CompositionAspect.PERSPECTIVE, AspectVerdict.NEEDS_ADJUSTMENT, "Tilt down to fit bodies")
        } else {
            AspectCheck(CompositionAspect.PERSPECTIVE, AspectVerdict.GOOD, "Room for bodies")
        }

    private fun tiltCheck(tilt: TiltState): AspectCheck? {
        if (!tilt.isAvailable) return null
        val level = abs(tilt.rollDegrees) <= thresholds.levelToleranceDegrees
        return AspectCheck(
            aspect = CompositionAspect.CAMERA_TILT,
            verdict = if (level) AspectVerdict.GOOD else AspectVerdict.NEEDS_ADJUSTMENT,
            detail = if (level) "Level" else "Camera tilted",
        )
    }

    private fun suggestionsFor(checks: List<AspectCheck>, tilt: TiltState): List<String> {
        val failing = checks.filter { it.verdict == AspectVerdict.NEEDS_ADJUSTMENT }
        return PRIORITY.mapNotNull { aspect ->
            val check = failing.firstOrNull { it.aspect == aspect } ?: return@mapNotNull null
            if (aspect == CompositionAspect.CAMERA_TILT) {
                if (tilt.rollDegrees > 0f) "Straighten the camera (tilting right)"
                else "Straighten the camera (tilting left)"
            } else check.detail
        }.take(MAX_SUGGESTIONS)
    }

    private companion object {
        const val MAX_SUGGESTIONS = 3

        /** Fix who is in frame first, then placement, then fine details. */
        val PRIORITY = listOf(
            CompositionAspect.BODY_FRAMING,
            CompositionAspect.SUBJECT_POSITION,
            CompositionAspect.HEADROOM,
            CompositionAspect.PERSPECTIVE,
            CompositionAspect.CAMERA_TILT,
        )
    }
}
