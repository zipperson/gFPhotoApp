package com.frame.app.domain.model

/**
 * The result of analyzing one camera frame for a human subject.
 *
 * Produced by the [com.frame.app.domain.service.PoseDetector] (Phase 3) and
 * consumed by the [com.frame.app.domain.service.CompositionEngine]. Carrying
 * only normalized, lightweight data keeps per-frame work cheap and the UI layer
 * free of any pixel buffers.
 *
 * @property boundingBox tight box around the detected subject, or null if none
 * @property landmarks   detected body landmarks (may be empty)
 * @property timestampMs frame timestamp (device uptime millis) for ordering/throttle
 */
data class SubjectFrame(
    val boundingBox: NormalizedRect?,
    val landmarks: List<NormalizedLandmark>,
    val timestampMs: Long,
) {
    val hasSubject: Boolean get() = boundingBox != null

    fun landmark(type: LandmarkType): NormalizedLandmark? =
        landmarks.firstOrNull { it.type == type }

    companion object {
        /** A frame in which no subject was detected. */
        fun empty(timestampMs: Long): SubjectFrame =
            SubjectFrame(boundingBox = null, landmarks = emptyList(), timestampMs = timestampMs)
    }
}
