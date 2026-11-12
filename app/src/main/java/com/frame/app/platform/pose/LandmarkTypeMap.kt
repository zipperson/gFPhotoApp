package com.frame.app.platform.pose

import com.google.mlkit.vision.pose.PoseLandmark
import com.frame.app.domain.model.LandmarkType

/**
 * Maps the subset of ML Kit's 33 pose landmarks that FRAME's composition logic
 * uses onto the domain [LandmarkType]. Landmarks outside this subset are ignored
 * — we only need the ones that define subject position, orientation, and framing.
 */
internal val MlKitToDomainLandmark: Map<Int, LandmarkType> = mapOf(
    PoseLandmark.NOSE to LandmarkType.NOSE,
    PoseLandmark.LEFT_EYE to LandmarkType.LEFT_EYE,
    PoseLandmark.RIGHT_EYE to LandmarkType.RIGHT_EYE,
    PoseLandmark.LEFT_SHOULDER to LandmarkType.LEFT_SHOULDER,
    PoseLandmark.RIGHT_SHOULDER to LandmarkType.RIGHT_SHOULDER,
    PoseLandmark.LEFT_HIP to LandmarkType.LEFT_HIP,
    PoseLandmark.RIGHT_HIP to LandmarkType.RIGHT_HIP,
    PoseLandmark.LEFT_KNEE to LandmarkType.LEFT_KNEE,
    PoseLandmark.RIGHT_KNEE to LandmarkType.RIGHT_KNEE,
    PoseLandmark.LEFT_ANKLE to LandmarkType.LEFT_ANKLE,
    PoseLandmark.RIGHT_ANKLE to LandmarkType.RIGHT_ANKLE,
)
