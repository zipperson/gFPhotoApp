package com.frame.app.domain.model

/**
 * The body landmarks FRAME cares about for composition analysis.
 *
 * This is a deliberately small, stable subset chosen for composition needs
 * (headroom, framing, orientation) rather than a 1:1 mirror of any specific
 * pose-detection SDK. The Phase 3 [com.frame.app.domain.service.PoseDetector]
 * implementation maps ML Kit's landmark set onto this enum, so the rest of the
 * app never depends on a vendor type.
 */
enum class LandmarkType {
    NOSE,
    LEFT_EYE,
    RIGHT_EYE,
    LEFT_SHOULDER,
    RIGHT_SHOULDER,
    LEFT_HIP,
    RIGHT_HIP,
    LEFT_KNEE,
    RIGHT_KNEE,
    LEFT_ANKLE,
    RIGHT_ANKLE,
}

/**
 * A single detected landmark in normalized frame space.
 *
 * @property type       which body point this is
 * @property position   location in normalized frame coordinates
 * @property confidence detector confidence in [0f, 1f]; 0f means "unknown"
 */
data class NormalizedLandmark(
    val type: LandmarkType,
    val position: NormalizedPoint,
    val confidence: Float,
)
