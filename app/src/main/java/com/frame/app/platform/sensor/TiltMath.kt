package com.frame.app.platform.sensor

import com.frame.app.domain.model.TiltState
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Pure math converting a device gravity vector into roll/pitch angles. Kept
 * free of Android so it's unit-testable with hand-computed vectors.
 *
 * Coordinate convention (Android sensor frame, device held in portrait):
 *  - x: points to the right of the screen
 *  - y: points to the top of the screen
 *  - z: points out of the screen toward the user
 * The gravity sensor reports the reaction to gravity, so held upright in
 * portrait it reads ~ (0, +9.81, 0), and lying flat face-up ~ (0, 0, +9.81).
 */
object TiltMath {

    /**
     * Roll: rotation about the viewing axis (z). 0 = horizon level. Positive =
     * the device is rolled so its right edge dips (clockwise from the user's
     * view). Computed from the x/y gravity components.
     */
    fun rollDegrees(gx: Float, gy: Float): Float {
        // On a real device held upright in portrait, gravity reads (0, +g, 0),
        // so atan2(x, y) gives 0 when upright. (A rolled-right device gains +x.)
        return Math.toDegrees(atan2(gx.toDouble(), gy.toDouble())).toFloat()
    }

    /**
     * Pitch: forward/back tilt. 0 = device vertical (portrait, pointing at the
     * horizon). Positive = top tilts toward the user (pointing down); negative =
     * top tilts away (pointing up). Computed from the z component vs the
     * in-plane magnitude.
     */
    fun pitchDegrees(gx: Float, gy: Float, gz: Float): Float {
        val inPlane = sqrt((gx * gx + gy * gy).toDouble())
        return Math.toDegrees(atan2(gz.toDouble(), inPlane)).toFloat()
    }

    /**
     * Build a [TiltState] from a gravity vector. Returns
     * [TiltState.UNAVAILABLE] for a degenerate (near-zero) vector, which can
     * happen during freefall or bad samples.
     */
    fun toTiltState(gx: Float, gy: Float, gz: Float): TiltState {
        val magnitude = sqrt((gx * gx + gy * gy + gz * gz).toDouble())
        if (magnitude < MIN_GRAVITY_MAGNITUDE) return TiltState.UNAVAILABLE
        return TiltState(
            rollDegrees = rollDegrees(gx, gy),
            pitchDegrees = pitchDegrees(gx, gy, gz),
            isAvailable = true,
        )
    }

    /** Below this magnitude (m/s^2) the vector is treated as unreliable. */
    private const val MIN_GRAVITY_MAGNITUDE = 1.0
}
