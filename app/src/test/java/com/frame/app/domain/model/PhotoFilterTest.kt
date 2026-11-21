package com.frame.app.domain.model

import com.frame.app.platform.filter.PhotoFilterProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoFilterTest {

    @Test
    fun original_hasNoMatrixAndIsInactive() {
        assertNull(PhotoFilter.NONE.matrix)
        assertFalse(PhotoFilter.NONE.isActive)
        assertEquals(Triple(10, 20, 30), PhotoFilter.NONE.apply(10, 20, 30))
    }

    @Test
    fun everyLook_isA4x5Matrix_thatLeavesAlphaAlone() {
        PhotoFilter.entries.filter { it.isActive }.forEach { filter ->
            val m = filter.matrix!!
            assertEquals(filter.name, 20, m.size)
            // Alpha row: 0, 0, 0, 1, 0.
            assertEquals(listOf(0f, 0f, 0f, 1f, 0f), m.slice(15..19))
        }
    }

    @Test
    fun everyLook_keepsWhiteBright_andNeverClipsBlackToPureBlack() {
        PhotoFilter.entries.filter { it.isActive }.forEach { filter ->
            val (wr, wg, wb) = filter.apply(255, 255, 255)
            assertTrue("${filter.name} white too dark", minOf(wr, wg, wb) >= 215)
            val (br, bg, bb) = filter.apply(0, 0, 0)
            assertTrue("${filter.name} blacks not lifted", maxOf(br, bg, bb) > 0)
        }
    }

    @Test
    fun mono_producesEqualChannels() {
        val (r, g, b) = PhotoFilter.MONO.apply(200, 80, 40)
        assertEquals(r, g)
        assertEquals(g, b)
    }

    @Test
    fun japanVintage_coolsShadows() {
        val (r, _, b) = PhotoFilter.JAPANESE_VINTAGE.apply(0, 0, 0)
        assertTrue(b > r)
    }

    @Test
    fun warmFade_warmsHighlights() {
        val (r, _, b) = PhotoFilter.WARM_FADE.apply(255, 255, 255)
        assertTrue(r > b)
    }

    @Test
    fun sampleSize_keepsTwelveMegapixelsFullResolution() {
        assertEquals(1, PhotoFilterProcessor.sampleSizeFor(4000, 3000))
    }

    @Test
    fun sampleSize_reducesHugeImagesToFitTheCap() {
        // 50 MP -> must halve once (12.5 MP).
        val sample = PhotoFilterProcessor.sampleSizeFor(8160, 6120)
        assertEquals(2, sample)
        val decoded = (8160L / sample) * (6120L / sample)
        assertTrue(decoded <= PhotoFilterProcessor.MAX_PIXELS)
    }
}
