package com.frame.app.platform.pose

import com.frame.app.domain.model.LandmarkType
import com.frame.app.domain.model.NormalizedLandmark
import com.frame.app.domain.model.NormalizedPoint
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame

/**
 * Pure mapping from raw ML Kit landmark data to the domain [SubjectFrame].
 * Android-free (operates on plain numbers), so the normalization + mirroring +
 * bounding-box math is fully unit-testable without the ML Kit SDK or a device.
 *
 * Coordinate contract: ML Kit returns landmark positions in the pixel space of
 * the *rotated* InputImage (i.e. already upright), where x in [0, imageWidth]
 * and y in [0, imageHeight]. We normalize by those dimensions. For the front
 * camera the preview is horizontally mirrored, so we flip x to match what the
 * user sees.
 */
object PoseMapping {

    /** A single raw landmark from ML Kit, reduced to the numbers we need. */
    data class RawLandmark(
        val type: LandmarkType,
        val x: Float,
        val y: Float,
        val inFrameLikelihood: Float,
    )

    /**
     * Build a [SubjectFrame] from raw landmarks.
     *
     * @param landmarks        raw landmarks (already filtered to the types we map)
     * @param imageWidth       rotated image width (px)
     * @param imageHeight      rotated image height (px)
     * @param mirrorX          true for the front camera (preview is mirrored)
     * @param timestampMs      frame timestamp
     * @param minLikelihood    drop landmarks below this in-frame likelihood
     */
    fun toSubjectFrame(
        landmarks: List<RawLandmark>,
        imageWidth: Int,
        imageHeight: Int,
        mirrorX: Boolean,
        timestampMs: Long,
        minLikelihood: Float = DEFAULT_MIN_LIKELIHOOD,
    ): SubjectFrame {
        if (imageWidth <= 0 || imageHeight <= 0) return SubjectFrame.empty(timestampMs)

        val mapped = landmarks
            .filter { it.inFrameLikelihood >= minLikelihood }
            .map { lm ->
                val nx = (lm.x / imageWidth).coerceIn(0f, 1f)
                val ny = (lm.y / imageHeight).coerceIn(0f, 1f)
                val finalX = if (mirrorX) 1f - nx else nx
                NormalizedLandmark(
                    type = lm.type,
                    position = NormalizedPoint(finalX, ny),
                    confidence = lm.inFrameLikelihood,
                )
            }

        if (mapped.isEmpty()) return SubjectFrame.empty(timestampMs)

        val box = boundingBox(mapped.map { it.position })
        return SubjectFrame(
            boundingBox = box,
            landmarks = mapped,
            timestampMs = timestampMs,
        )
    }

    /** Tight axis-aligned box around a set of normalized points. */
    fun boundingBox(points: List<NormalizedPoint>): NormalizedRect? {
        if (points.isEmpty()) return null
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = Float.MIN_VALUE
        var bottom = Float.MIN_VALUE
        for (p in points) {
            if (p.x < left) left = p.x
            if (p.y < top) top = p.y
            if (p.x > right) right = p.x
            if (p.y > bottom) bottom = p.y
        }
        return NormalizedRect(left, top, right, bottom)
    }

    const val DEFAULT_MIN_LIKELIHOOD = 0.5f
}
