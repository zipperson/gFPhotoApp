package com.frame.app.platform.face

import com.frame.app.domain.composition.GroupFraming
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame
import kotlin.math.abs

/**
 * Steadies group detections frame to frame, like [com.frame.app.platform.pose.SubjectSmoother]
 * does for a single person.
 *
 *  - When the number of faces is unchanged, each face (matched left to right) is
 *    eased toward its new position, with a small dead zone to kill jitter.
 *  - When the number changes, the new detection is adopted as is, because we
 *    can't tell which old face is which.
 *  - A short run of empty frames keeps showing the last group instead of flickering.
 *
 * Not thread-safe; call from the analysis thread only.
 */
class GroupSmoother(
    private val smoothingFactor: Float = 0.3f,
    private val holdFrames: Int = 4,
    private val deadZone: Float = 0.008f,
) {
    private var last: SubjectFrame? = null
    private var missCount = 0

    fun process(raw: SubjectFrame): SubjectFrame {
        val rawFaces = raw.group?.faces.orEmpty()
        if (rawFaces.isEmpty()) {
            val held = last
            return if (held != null && missCount < holdFrames) {
                missCount++
                held.copy(timestampMs = raw.timestampMs)
            } else {
                last = null
                SubjectFrame.empty(raw.timestampMs)
            }
        }

        missCount = 0
        val prevFaces = last?.group?.faces
        val faces = if (prevFaces == null || prevFaces.size != rawFaces.size) {
            rawFaces
        } else {
            rawFaces.mapIndexed { i, face -> blend(prevFaces[i], face) }
        }
        val result = GroupFraming.toSubjectFrame(faces, raw.timestampMs)
        last = result
        return result
    }

    fun reset() {
        last = null
        missCount = 0
    }

    private fun blend(old: NormalizedRect, new: NormalizedRect) = NormalizedRect(
        left = axis(old.left, new.left),
        top = axis(old.top, new.top),
        right = axis(old.right, new.right),
        bottom = axis(old.bottom, new.bottom),
    )

    private fun axis(old: Float, new: Float): Float =
        if (abs(new - old) < deadZone) old else old + smoothingFactor * (new - old)
}
