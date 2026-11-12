package com.frame.app.platform.pose

import com.frame.app.domain.model.NormalizedLandmark
import com.frame.app.domain.model.NormalizedPoint
import com.frame.app.domain.model.SubjectFrame

/**
 * Smooths raw per-frame detections to reduce jitter and flicker.
 *
 * Two behaviors, both pure and unit-testable:
 *  1. **Positional smoothing** - an exponential moving average over each
 *     landmark's position, so a still subject's box/dots stop shaking.
 *  2. **Dropout hold** - if a frame briefly loses the subject, keep showing the
 *     last known one for up to [holdFrames] frames instead of flickering off.
 *
 * Not thread-safe; call from a single thread (the analysis executor).
 *
 * @param smoothingFactor EMA weight for the NEW sample, in (0,1]. Lower = more
 *        smoothing/lag; higher = more responsive/jittery. ~0.5 is a good balance.
 * @param holdFrames how many consecutive subject-less frames to keep the last
 *        subject before emitting empty.
 */
class SubjectSmoother(
    private val smoothingFactor: Float = DEFAULT_SMOOTHING,
    private val holdFrames: Int = DEFAULT_HOLD_FRAMES,
    /** Movements below this (normalized) are treated as noise and ignored,
     *  eliminating micro-jitter when the subject is essentially still. */
    private val deadZone: Float = DEFAULT_DEAD_ZONE,
) {
    private var last: SubjectFrame? = null
    private var missCount: Int = 0

    /** Feed a raw detection; returns the smoothed/held frame to display. */
    fun process(frame: SubjectFrame): SubjectFrame {
        if (!frame.hasSubject) {
            val held = last
            return if (held != null && missCount < holdFrames) {
                missCount++
                // Re-stamp with the new timestamp but keep the last geometry.
                held.copy(timestampMs = frame.timestampMs)
            } else {
                last = null
                SubjectFrame.empty(frame.timestampMs)
            }
        }

        missCount = 0
        val prev = last
        val smoothed = if (prev == null || !prev.hasSubject) {
            frame // first detection: adopt as-is
        } else {
            blend(prev, frame, smoothingFactor)
        }
        last = smoothed
        return smoothed
    }

    /** Reset state, e.g. on a lens switch so the new camera starts clean. */
    fun reset() {
        last = null
        missCount = 0
    }

    private fun blend(prev: SubjectFrame, next: SubjectFrame, a: Float): SubjectFrame {
        val blendedLandmarks = next.landmarks.map { n ->
            val p = prev.landmark(n.type)
            if (p == null) n else NormalizedLandmark(
                type = n.type,
                position = NormalizedPoint(
                    x = smoothAxis(p.position.x, n.position.x, a),
                    y = smoothAxis(p.position.y, n.position.y, a),
                ),
                confidence = n.confidence,
            )
        }
        val box = PoseMapping.boundingBox(blendedLandmarks.map { it.position })
        return SubjectFrame(
            boundingBox = box,
            landmarks = blendedLandmarks,
            timestampMs = next.timestampMs,
        )
    }

    /** Dead-zone + EMA: sub-threshold moves are held (kills micro-jitter);
     *  larger moves are eased toward the new value. */
    private fun smoothAxis(old: Float, new: Float, a: Float): Float {
        if (kotlin.math.abs(new - old) < deadZone) return old
        return ema(old, new, a)
    }

    private fun ema(old: Float, new: Float, a: Float): Float = old + a * (new - old)

    companion object {
        // Lower factor = heavier smoothing. 0.25 keeps most weight on history.
        const val DEFAULT_SMOOTHING = 0.25f
        const val DEFAULT_HOLD_FRAMES = 4
        // ~0.8% of frame width/height; below this, treat as noise.
        const val DEFAULT_DEAD_ZONE = 0.008f
    }
}
