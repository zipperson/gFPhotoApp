package com.frame.app.domain.composition

import com.frame.app.domain.model.NormalizedPoint
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame

/**
 * Computes the *ideal* subject placement for a style — the target the on-screen
 * guidance draws and the user lines their subject up with.
 *
 * Pure and style-driven (from [CompositionThresholds]), so it's unit-testable
 * and automatically matches whatever style is selected. The target is a
 * normalized rectangle the subject box should occupy; the UI draws it and points
 * an arrow from the current subject toward it.
 */
object CompositionTarget {

    /**
     * The ideal subject rectangle for the given thresholds. Sized to the middle
     * of the style's preferred size band, placed horizontally on the favored
     * vertical third (or centered for center-styles) and vertically at the
     * style's ideal center.
     *
     * @param preferLeftThird when a thirds style, which third to target. Chosen
     *        from the current subject so the arrow points the shorter way.
     */
    fun idealRect(
        thresholds: CompositionThresholds,
        preferLeftThird: Boolean,
    ): NormalizedRect {
        // Target size: middle of the ideal band. Assume a portrait subject that's
        // taller than wide (typical standing person): height from the size band,
        // width a fraction of that.
        val targetHeight = ((thresholds.sizeIdealMin + thresholds.sizeIdealMax) / 2f)
            .coerceIn(0.2f, 0.95f)
        val targetWidth = (targetHeight * SUBJECT_ASPECT).coerceIn(0.1f, 0.9f)

        val centerX = when {
            thresholds.thirdsTolerance <= 0f -> 0.5f // centered styles
            preferLeftThird -> CompositionMetrics.THIRD_LOW
            else -> CompositionMetrics.THIRD_HIGH
        }
        val centerY = thresholds.idealVerticalCenter

        return rectFromCenter(centerX, centerY, targetWidth, targetHeight)
    }

    /**
     * Convenience: pick the nearer third based on where the subject currently is,
     * so guidance nudges the shorter direction.
     */
    fun idealRectFor(
        thresholds: CompositionThresholds,
        subject: SubjectFrame,
    ): NormalizedRect {
        val cx = subject.boundingBox?.centerX ?: 0.5f
        val preferLeft = cx <= 0.5f
        return idealRect(thresholds, preferLeftThird = preferLeft)
    }

    /** Center offset from the subject's current center to the target center. */
    fun offsetToTarget(subject: SubjectFrame, target: NormalizedRect): NormalizedPoint? {
        val box = subject.boundingBox ?: return null
        return NormalizedPoint(
            x = target.centerX - box.centerX,
            y = target.centerY - box.centerY,
        )
    }

    /**
     * True when the subject is close enough to the target to be considered
     * aligned (both center and size within tolerance).
     */
    fun isAligned(
        subject: SubjectFrame,
        target: NormalizedRect,
        centerTolerance: Float = ALIGN_CENTER_TOL,
        sizeTolerance: Float = ALIGN_SIZE_TOL,
    ): Boolean {
        val box = subject.boundingBox ?: return false
        val dx = kotlin.math.abs(box.centerX - target.centerX)
        val dy = kotlin.math.abs(box.centerY - target.centerY)
        val dSize = kotlin.math.abs(maxOf(box.width, box.height) - maxOf(target.width, target.height))
        return dx <= centerTolerance && dy <= centerTolerance && dSize <= sizeTolerance
    }

    private fun rectFromCenter(cx: Float, cy: Float, w: Float, h: Float): NormalizedRect {
        var left = cx - w / 2f
        var right = cx + w / 2f
        var top = cy - h / 2f
        var bottom = cy + h / 2f
        // Shift fully into [0,1] if it spills past an edge, preserving size.
        if (left < 0f) { right -= left; left = 0f }
        if (right > 1f) { left -= (right - 1f); right = 1f }
        if (top < 0f) { bottom -= top; top = 0f }
        if (bottom > 1f) { top -= (bottom - 1f); bottom = 1f }
        return NormalizedRect(
            left = left.coerceIn(0f, 1f),
            top = top.coerceIn(0f, 1f),
            right = right.coerceIn(0f, 1f),
            bottom = bottom.coerceIn(0f, 1f),
        )
    }

    /** Standing-subject box is taller than wide. */
    private const val SUBJECT_ASPECT = 0.42f
    // Loosened so real handheld framing actually reaches "aligned". Center
    // matters more than exact size (the skeleton box rarely matches exactly).
    private const val ALIGN_CENTER_TOL = 0.12f
    private const val ALIGN_SIZE_TOL = 0.22f
}
