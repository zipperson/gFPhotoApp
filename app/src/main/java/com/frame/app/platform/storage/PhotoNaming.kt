package com.frame.app.platform.storage

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds capture filenames. Pure and deterministic given a timestamp, so it can
 * be unit-tested without touching the filesystem or MediaStore.
 */
object PhotoNaming {

    private const val PREFIX = "FRAME"
    private const val EXTENSION = "jpg"

    /**
     * Produce a sortable, collision-resistant JPEG filename, e.g.
     * `FRAME_20261111_153002_123.jpg`.
     *
     * @param timestampMillis capture time (defaults to now)
     */
    fun jpegFileName(timestampMillis: Long = System.currentTimeMillis()): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestampMillis))
        return "${PREFIX}_$stamp.$EXTENSION"
    }
}
