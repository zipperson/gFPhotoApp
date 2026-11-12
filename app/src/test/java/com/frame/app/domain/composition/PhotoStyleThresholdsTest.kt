package com.frame.app.domain.composition

import com.frame.app.domain.model.PhotoStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoStyleThresholdsTest {

    @Test
    fun everyStyleMapsToAThresholdPreset() {
        // Exhaustive: each style resolves without throwing.
        PhotoStyle.values().forEach { style ->
            val t = style.toThresholds()
            assertTrue(t.headroomMin >= 0f)
        }
    }

    @Test
    fun aestheticAndLowAngle_preferLowSubject() {
        assertTrue(PhotoStyle.AESTHETIC_FULL_BODY.toThresholds().preferLowSubject)
        assertTrue(PhotoStyle.LOW_ANGLE.toThresholds().preferLowSubject)
    }

    @Test
    fun classicAndCentered_doNotPreferLowSubject() {
        assertEquals(false, PhotoStyle.CLASSIC_PORTRAIT.toThresholds().preferLowSubject)
        assertEquals(false, PhotoStyle.CENTERED_SYMMETRICAL.toThresholds().preferLowSubject)
    }

    @Test
    fun lowAngle_placesSubjectLowerThanAesthetic() {
        assertTrue(
            PhotoStyle.LOW_ANGLE.toThresholds().idealVerticalCenter >
                PhotoStyle.AESTHETIC_FULL_BODY.toThresholds().idealVerticalCenter
        )
    }

    @Test
    fun centered_hasGenerousCenterToleranceAndNoThirds() {
        val t = PhotoStyle.CENTERED_SYMMETRICAL.toThresholds()
        assertEquals(0f, t.thirdsTolerance, 1e-6f)
        assertTrue(t.centerTolerance >= 0.1f)
    }

    @Test
    fun defaultStyle_isAestheticFullBody() {
        assertEquals(PhotoStyle.AESTHETIC_FULL_BODY, PhotoStyle.DEFAULT)
    }
}
