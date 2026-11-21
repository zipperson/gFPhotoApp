package com.frame.app.domain.model

import com.frame.app.platform.filter.NoiseTile
import com.frame.app.platform.filter.PhotoFilterProcessor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoFilterTest {

    private val looks = PhotoFilter.entries.filter { it.isActive }

    @Test
    fun original_hasNoMatrixOrEffects() {
        assertNull(PhotoFilter.NONE.matrix)
        assertFalse(PhotoFilter.NONE.isActive)
        assertEquals(0f, PhotoFilter.NONE.vignette, 0f)
        assertEquals(0f, PhotoFilter.NONE.grain, 0f)
        assertEquals(Triple(10, 20, 30), PhotoFilter.NONE.apply(10, 20, 30))
    }

    @Test
    fun everyLook_isA4x5Matrix_thatLeavesAlphaAlone() {
        looks.forEach { look ->
            val m = look.matrix!!
            assertEquals(look.name, 20, m.size)
            assertEquals(listOf(0f, 0f, 0f, 1f, 0f), m.slice(15..19))
        }
    }

    @Test
    fun everyLook_hasVignetteAndGrainInRange() {
        looks.forEach { look ->
            assertTrue(look.name, look.vignette in 0.1f..1f)
            assertTrue(look.name, look.grain in 0.1f..1f)
        }
    }

    @Test
    fun digicamLooks_crushBlacks_notLiftThem() {
        // The reference look has deep blacks. A lifted/faded look would fail this.
        looks.forEach { look ->
            val (r, g, b) = look.apply(0, 0, 0)
            assertTrue("${look.name} blacks lifted: $r,$g,$b", maxOf(r, g, b) <= 8)
        }
    }

    @Test
    fun digicamLooks_addContrast_midGreyStaysMid_brightsStayBright() {
        // Neutral mid grey should not drift far from the middle...
        looks.filter { it != PhotoFilter.ORANGE && it != PhotoFilter.GOLDEN_HOUR }.forEach { look ->
            val (r, g, b) = look.apply(128, 128, 128)
            assertTrue("${look.name} mid $r,$g,$b", listOf(r, g, b).all { it in 100..160 })
        }
        // ...and a bright pixel stays bright (no washed-out greys).
        looks.forEach { look ->
            val (r, g, b) = look.apply(240, 240, 240)
            assertTrue("${look.name} bright $r,$g,$b", maxOf(r, g, b) >= 200)
        }
    }

    @Test
    fun digicam_warmHighlights_tealShadows() {
        val (hr, _, hb) = PhotoFilter.DIGICAM.apply(255, 255, 255)
        assertTrue(hr > hb)
        val (sr, sg, sb) = PhotoFilter.DIGICAM.apply(32, 32, 32)
        assertTrue("shadow $sr,$sg,$sb", sg > sr && sb > sr)
    }

    @Test
    fun golden_warmsEverything_andTurnsGreensOlive() {
        val (r, _, b) = PhotoFilter.GOLDEN_HOUR.apply(128, 128, 128)
        assertTrue(r - b > 40)
        // A leaf green gains red (olive) and loses blue.
        val (lr, _, lb) = PhotoFilter.GOLDEN_HOUR.apply(70, 120, 50)
        assertTrue(lr > 70 && lb < 50)
    }

    @Test
    fun orange_isStronglyOrange() {
        val (r, g, b) = PhotoFilter.ORANGE.apply(128, 128, 128)
        assertTrue("$r,$g,$b", r > g && g > b)
    }

    @Test
    fun coolCam_coolsTheWhitePoint() {
        val (r, _, b) = PhotoFilter.COOL_CAM.apply(255, 255, 255)
        assertTrue(b > r)
    }

    @Test
    fun mono_producesEqualChannels() {
        val (r, g, b) = PhotoFilter.MONO.apply(200, 80, 40)
        assertEquals(r, g)
        assertEquals(g, b)
    }

    // --- Grain tile ---

    @Test
    fun noiseTile_isDeterministic_opaqueGrey_andCentredOnMid() {
        val a = NoiseTile.pixels(size = 32, seed = 1L)
        assertArrayEquals(a, NoiseTile.pixels(size = 32, seed = 1L))
        assertTrue(a.all { (it ushr 24) == 0xFF && ((it shr 16) and 0xFF) == (it and 0xFF) })
        val mean = a.map { it and 0xFF }.average()
        assertTrue("mean $mean", mean in 118.0..138.0)
    }

    // --- Memory cap ---

    @Test
    fun sampleSize_keepsTwelveMegapixelsFullResolution() {
        assertEquals(1, PhotoFilterProcessor.sampleSizeFor(4000, 3000))
    }

    @Test
    fun sampleSize_reducesHugeImagesToFitTheCap() {
        val sample = PhotoFilterProcessor.sampleSizeFor(8160, 6120)
        assertEquals(2, sample)
        assertTrue((8160L / sample) * (6120L / sample) <= PhotoFilterProcessor.MAX_PIXELS)
    }
}
