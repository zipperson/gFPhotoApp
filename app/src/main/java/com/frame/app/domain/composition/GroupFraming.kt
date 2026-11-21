package com.frame.app.domain.composition

import com.frame.app.domain.model.GroupInfo
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame

/**
 * Builds a group [SubjectFrame] from detected face rectangles. Pure, so it is
 * unit-testable without ML Kit.
 */
object GroupFraming {

    /** A face this close to the frame edge counts as cut off. */
    const val EDGE_MARGIN = 0.015f

    /**
     * @param faces normalized face boxes in any order
     * @return a subject whose box is the union of the faces, or an empty subject
     */
    fun toSubjectFrame(faces: List<NormalizedRect>, timestampMs: Long): SubjectFrame {
        if (faces.isEmpty()) return SubjectFrame.empty(timestampMs)
        val sorted = faces.sortedBy { it.left }
        return SubjectFrame(
            boundingBox = union(sorted),
            landmarks = emptyList(),
            timestampMs = timestampMs,
            group = GroupInfo(faces = sorted, clippedCount = sorted.count { isClipped(it) }),
        )
    }

    fun union(faces: List<NormalizedRect>): NormalizedRect = NormalizedRect(
        left = faces.minOf { it.left },
        top = faces.minOf { it.top },
        right = faces.maxOf { it.right },
        bottom = faces.maxOf { it.bottom },
    )

    fun isClipped(face: NormalizedRect): Boolean =
        face.left <= EDGE_MARGIN ||
            face.top <= EDGE_MARGIN ||
            face.right >= 1f - EDGE_MARGIN ||
            face.bottom >= 1f - EDGE_MARGIN
}
