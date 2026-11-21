package com.frame.app.domain.model

/**
 * A colour look applied to the live preview and to the saved photo.
 *
 * Each look is a single 4x5 colour matrix (Android `ColorMatrix` layout,
 * row-major: R, G, B, A rows, each `[r, g, b, a, offset]`, offsets on a 0..255
 * scale). Using one matrix for both preview and saved file means what you see
 * is what you get. It also keeps the domain free of Android types.
 *
 * Grain and vignette are deliberately not part of a look: they can't be
 * previewed cheaply, and the preview and result must match.
 */
enum class PhotoFilter(
    val displayName: String,
    /** 20 floats, or null for [NONE] (identity, no processing at all). */
    val matrix: FloatArray?,
) {
    NONE("Original", null),

    /** Soft, low-contrast, lifted blacks with cool shadows: a Japanese film look. */
    JAPANESE_VINTAGE(
        "Japan Vintage",
        floatArrayOf(
            0.93f, 0.07f, 0.00f, 0f, 14f,
            0.02f, 0.92f, 0.06f, 0f, 16f,
            0.04f, 0.08f, 0.80f, 0f, 22f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),

    /** Warm highlights and faded blacks. */
    WARM_FADE(
        "Warm Fade",
        floatArrayOf(
            0.98f, 0.06f, 0.00f, 0f, 16f,
            0.02f, 0.94f, 0.04f, 0f, 10f,
            0.00f, 0.06f, 0.80f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),

    /** Black and white with slightly lifted blacks. */
    MONO(
        "Mono",
        floatArrayOf(
            0.28f, 0.55f, 0.11f, 0f, 10f,
            0.28f, 0.55f, 0.11f, 0f, 10f,
            0.28f, 0.55f, 0.11f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),

    /** Cool, matte blues with raised blacks. */
    COOL_MATTE(
        "Cool Matte",
        floatArrayOf(
            0.90f, 0.05f, 0.05f, 0f, 12f,
            0.00f, 0.95f, 0.05f, 0f, 14f,
            0.00f, 0.05f, 1.00f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f,
        ),
    );

    val isActive: Boolean get() = this != NONE

    /** Applies the look to one 0..255 RGB pixel (clamped). Used for tests. */
    fun apply(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        val m = matrix ?: return Triple(r, g, b)
        fun row(i: Int) = (m[i] * r + m[i + 1] * g + m[i + 2] * b + m[i + 4])
            .toInt().coerceIn(0, 255)
        return Triple(row(0), row(5), row(10))
    }
}
