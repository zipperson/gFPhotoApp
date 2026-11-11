package com.frame.app.ui.camera.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure tests for normalized -> canvas-pixel mapping, including the FILL_CENTER
 * crop transform that future subject boxes / reference outlines rely on.
 */
class OverlayMappingTest {

    private val eps = 1e-3f

    @Test
    fun forCanvas_mapsNormalizedToPixels() {
        val m = OverlayMapping.forCanvas(1000f, 2000f)
        assertEquals(0f, m.x(0f), eps)
        assertEquals(500f, m.x(0.5f), eps)
        assertEquals(1000f, m.x(1f), eps)
        assertEquals(0f, m.y(0f), eps)
        assertEquals(1000f, m.y(0.5f), eps)
        assertEquals(2000f, m.y(1f), eps)
    }

    @Test
    fun forImageInView_sameAspect_isPureScaleNoOffset() {
        // Source 1000x2000 into canvas 500x1000 (same 1:2 aspect): scale 0.5, no crop.
        val m = OverlayMapping.forImageInView(
            canvasWidth = 500f, canvasHeight = 1000f,
            sourceWidth = 1000f, sourceHeight = 2000f,
        )
        assertEquals(0.5f, m.scaleX, eps)
        assertEquals(0f, m.offsetX, eps)
        assertEquals(0f, m.offsetY, eps)
        // A source-space center maps to the canvas center.
        assertEquals(250f, m.x(0.5f), eps)
        assertEquals(500f, m.y(0.5f), eps)
    }

    @Test
    fun forImageInView_widerSource_cropsHorizontally() {
        // Source 2000x1000 (2:1) into canvas 1000x1000 (1:1).
        // Cover scale = max(1000/2000, 1000/1000) = 1.0 -> scaledW=2000, offX=-500.
        val m = OverlayMapping.forImageInView(
            canvasWidth = 1000f, canvasHeight = 1000f,
            sourceWidth = 2000f, sourceHeight = 1000f,
        )
        assertEquals(1.0f, m.scaleX, eps)
        assertEquals(-500f, m.offsetX, eps)
        assertEquals(0f, m.offsetY, eps)
        // Source center still lands at canvas center.
        assertEquals(500f, m.x(0.5f), eps)
        assertEquals(500f, m.y(0.5f), eps)
    }

    @Test
    fun forImageInView_degenerateSource_fallsBackToCanvas() {
        val m = OverlayMapping.forImageInView(1000f, 1000f, 0f, 0f)
        assertEquals(1000f, m.canvasWidth, eps)
        assertEquals(500f, m.x(0.5f), eps)
    }
}
