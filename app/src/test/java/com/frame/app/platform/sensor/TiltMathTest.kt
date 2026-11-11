package com.frame.app.platform.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for tilt math, using hand-computed gravity vectors.
 * g ~ 9.81 m/s^2. Android sensor frame: x=right, y=top, z=out of screen.
 */
class TiltMathTest {

    private val g = 9.81f
    private val eps = 0.5f // degrees; generous for float trig

    @Test
    fun upright_portrait_isLevelZeroRoll() {
        // Held upright in portrait, the gravity sensor reads (0, +g, 0).
        val roll = TiltMath.rollDegrees(0f, g)
        assertEquals(0f, roll, eps)
    }

    @Test
    fun rolledRight_givesPositiveRoll() {
        // Tilt so the right edge dips: gravity gains +x. (+g/√2 x, +g/√2 y)
        val roll = TiltMath.rollDegrees(g * 0.7071f, g * 0.7071f)
        assertEquals(45f, roll, eps)
    }

    @Test
    fun rolledLeft_givesNegativeRoll() {
        val roll = TiltMath.rollDegrees(-g * 0.7071f, g * 0.7071f)
        assertEquals(-45f, roll, eps)
    }

    @Test
    fun ninetyDegreeRoll_landscape() {
        // Rotated to landscape: gravity along +x -> (g, 0, 0).
        val roll = TiltMath.rollDegrees(g, 0f)
        assertEquals(90f, roll, eps)
    }

    @Test
    fun faceUp_flatOnTable_pitchIsNinety() {
        // Lying flat face-up: gravity out of screen -> (0, 0, g).
        val pitch = TiltMath.pitchDegrees(0f, 0f, g)
        assertEquals(90f, pitch, eps)
    }

    @Test
    fun upright_portrait_pitchIsZero() {
        // Upright: no z component -> pitch 0.
        val pitch = TiltMath.pitchDegrees(0f, g, 0f)
        assertEquals(0f, pitch, eps)
    }

    @Test
    fun toTiltState_upright_isAvailableAndLevel() {
        val state = TiltMath.toTiltState(0f, g, 0f)
        assertTrue(state.isAvailable)
        assertTrue(state.isLevel())
    }

    @Test
    fun toTiltState_rolled_isNotLevel() {
        val state = TiltMath.toTiltState(g * 0.5f, g * 0.87f, 0f)
        assertTrue(state.isAvailable)
        assertFalse(state.isLevel())
    }

    @Test
    fun toTiltState_nearZeroVector_isUnavailable() {
        // Freefall / degenerate sample.
        val state = TiltMath.toTiltState(0.1f, 0.1f, 0.1f)
        assertFalse(state.isAvailable)
    }
}
