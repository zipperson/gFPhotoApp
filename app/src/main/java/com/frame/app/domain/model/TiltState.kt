package com.frame.app.domain.model

import kotlin.math.abs

/**
 * Device orientation relevant to holding the camera level, derived from motion
 * sensors by the platform [com.frame.app.domain.service.LevelSensor].
 *
 * @property rollDegrees  rotation around the viewing axis; 0 = horizon level.
 *                        Positive = rolled clockwise from the user's view.
 * @property pitchDegrees forward/back tilt; 0 = phone vertical (portrait),
 *                        negative = tilted back (pointing up), positive = forward.
 * @property isAvailable  false when sensors haven't reported yet / are absent.
 */
data class TiltState(
    val rollDegrees: Float,
    val pitchDegrees: Float,
    val isAvailable: Boolean,
) {
    /** True when roll is within [toleranceDegrees] of level. */
    fun isLevel(toleranceDegrees: Float = DEFAULT_LEVEL_TOLERANCE): Boolean =
        isAvailable && abs(rollDegrees) <= toleranceDegrees

    companion object {
        const val DEFAULT_LEVEL_TOLERANCE = 1.5f

        val UNAVAILABLE = TiltState(
            rollDegrees = 0f,
            pitchDegrees = 0f,
            isAvailable = false,
        )
    }
}
