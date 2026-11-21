package com.frame.app.domain.model

/**
 * A "digicam" look applied to the live preview and to the saved photo.
 *
 * A look has three parts, all cheap enough to preview live:
 *  1. a 4x5 colour matrix (Android `ColorMatrix` layout, row-major: R, G, B, A
 *     rows, each `[r, g, b, a, offset]`, offsets on a 0..255 scale). Gains above 1
 *     with negative offsets give the punchy contrast and crushed blacks that
 *     compact cameras have.
 *  2. a vignette: how much the corners darken (0 = none, 1 = black corners).
 *  3. grain: how visible the film/sensor noise is (0 = none).
 *
 * The preview and the saved photo use the same three parts, so what you see is
 * what you get (the grain pattern itself differs, its strength does not). This
 * type stays free of Android types.
 */
enum class PhotoFilter(
    val displayName: String,
    /** 20 floats, or null for [NONE] (identity, no processing at all). */
    val matrix: FloatArray?,
    /** Corner darkening, 0..1. */
    val vignette: Float = 0f,
    /** Noise strength, 0..1. */
    val grain: Float = 0f,
) {
    NONE("Original", null),

    /**
     * Sony Cybershot-style compact: contrast with crushed blacks, warm
     * highlights, slightly teal shadows and muted blue skies.
     */
    DIGICAM(
        "Digicam",
        floatArrayOf(
            1.10f, 0.04f, -0.02f, 0f, -16f,
            0.00f, 1.04f, 0.00f, 0f, -6f,
            -0.02f, 0.04f, 0.96f, 0f, -4f,
            0f, 0f, 0f, 1f, 0f,
        ),
        vignette = 0.35f,
        grain = 0.25f,
    ),

    /** Warm golden wash: yellowed highlights, olive greens, strong vignette. */
    GOLDEN_HOUR(
        "Golden",
        floatArrayOf(
            1.12f, 0.18f, 0.00f, 0f, -6f,
            0.00f, 1.00f, 0.00f, 0f, -8f,
            0.00f, 0.00f, 0.80f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
        vignette = 0.50f,
        grain = 0.30f,
    ),

    /** Deep burnt orange with near-black shadows, like lamplit film. */
    ORANGE(
        "Orange",
        floatArrayOf(
            1.25f, 0.12f, 0.00f, 0f, -22f,
            0.00f, 0.92f, 0.00f, 0f, -18f,
            0.00f, 0.00f, 0.50f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f,
        ),
        vignette = 0.60f,
        grain = 0.30f,
    ),

    /** Cool compact: cyan-blue shadows and a clean cool white. */
    COOL_CAM(
        "Cool Cam",
        floatArrayOf(
            0.94f, 0.04f, 0.02f, 0f, -10f,
            0.00f, 1.02f, 0.04f, 0f, -6f,
            0.00f, 0.06f, 1.04f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f,
        ),
        vignette = 0.35f,
        grain = 0.25f,
    ),

    /** Contrasty black and white with grain. */
    MONO(
        "Mono",
        floatArrayOf(
            0.34f, 0.67f, 0.13f, 0f, -18f,
            0.34f, 0.67f, 0.13f, 0f, -18f,
            0.34f, 0.67f, 0.13f, 0f, -18f,
            0f, 0f, 0f, 1f, 0f,
        ),
        vignette = 0.40f,
        grain = 0.35f,
    );

    val isActive: Boolean get() = this != NONE

    /** Applies the colour matrix to one 0..255 RGB pixel (clamped). Used for tests. */
    fun apply(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        val m = matrix ?: return Triple(r, g, b)
        fun row(i: Int) = (m[i] * r + m[i + 1] * g + m[i + 2] * b + m[i + 4])
            .toInt().coerceIn(0, 255)
        return Triple(row(0), row(5), row(10))
    }
}
