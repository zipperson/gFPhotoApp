package com.frame.app.platform.filter

import java.util.Random

/**
 * A small tile of mid-grey noise used for film grain. Drawn with an Overlay
 * blend, mid-grey leaves the picture unchanged, so only the noise's deviation
 * from 128 shows up. The same tile feeds the live preview and the saved photo.
 *
 * Pure (no Android types), so it is unit-testable.
 */
object NoiseTile {

    const val SIZE = 256

    /** Spread of the noise around mid-grey, in 0..255 units. */
    private const val SPREAD = 45f

    /** ARGB pixels, `size * size`, all opaque grey. Deterministic for a seed. */
    fun pixels(size: Int = SIZE, seed: Long = 7L): IntArray {
        val random = Random(seed)
        return IntArray(size * size) {
            val v = (128f + random.nextGaussian().toFloat() * SPREAD).toInt().coerceIn(0, 255)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
    }
}
