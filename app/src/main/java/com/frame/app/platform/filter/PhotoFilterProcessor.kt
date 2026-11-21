package com.frame.app.platform.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.view.View
import com.frame.app.domain.model.PhotoFilter
import java.io.ByteArrayOutputStream

/** The Android colour matrix for this look, or null for the Original. */
fun PhotoFilter.toColorMatrix(): ColorMatrix? = matrix?.let { ColorMatrix(it) }

/**
 * Tints everything this view draws (here: the camera preview) with [filter].
 * A hardware layer with a colour-filter paint is applied when the layer is
 * composited, so it costs the CPU nothing. [PhotoFilter.NONE] removes the layer.
 */
fun View.applyPhotoFilter(filter: PhotoFilter) {
    val colorMatrix = filter.toColorMatrix()
    if (colorMatrix == null) {
        setLayerType(View.LAYER_TYPE_NONE, null)
    } else {
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
        setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }
}

/**
 * Bakes a [PhotoFilter] into an already-saved JPEG, in place.
 *
 * CameraX writes pixels unrotated plus an EXIF orientation tag, so we filter the
 * raw pixels and copy the tag back; the photo keeps its orientation. The result
 * is encoded to memory first and only then written over the original, so a
 * failure part-way leaves the unfiltered photo intact.
 *
 * Memory: a decoded bitmap is 4 bytes per pixel, so very large captures are
 * decoded at a reduced size (a power-of-two fraction) to stay under
 * [MAX_PIXELS]. 12 MP photos are processed at full resolution.
 *
 * Blocking and CPU-heavy: call off the main thread.
 */
class PhotoFilterProcessor(private val context: Context) {

    /** @return true if the filter was applied and saved; false leaves the original. */
    fun apply(uri: Uri, filter: PhotoFilter): Boolean {
        val colorMatrix = filter.toColorMatrix() ?: return true
        val resolver = context.contentResolver

        return runCatching {
            // 1. Orientation and timestamp from the original.
            val (orientation, dateTime) = resolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                ) to exif.getAttribute(ExifInterface.TAG_DATETIME)
            } ?: return false

            // 2. Decode (reduced if huge).
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
                inMutable = true
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return false

            // 3. Filter in place, then encode to memory.
            val encoded = try {
                val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
                Canvas(bitmap).drawBitmap(bitmap, 0f, 0f, paint)
                ByteArrayOutputStream().also {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
                }.toByteArray()
            } finally {
                bitmap.recycle()
            }

            // 4. Overwrite, then restore the EXIF tags.
            resolver.openOutputStream(uri, "wt")?.use { it.write(encoded) } ?: return false
            resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                dateTime?.let { exif.setAttribute(ExifInterface.TAG_DATETIME, it) }
                exif.saveAttributes()
            }
            true
        }.getOrDefault(false)
    }

    companion object {
        /** Cap on decoded pixels (~64 MB of ARGB_8888). */
        const val MAX_PIXELS = 16_000_000L
        private const val JPEG_QUALITY = 95

        /** Smallest power-of-two sample size that brings the image under [MAX_PIXELS]. */
        fun sampleSizeFor(width: Int, height: Int): Int {
            var sample = 1
            while ((width.toLong() / sample) * (height.toLong() / sample) > MAX_PIXELS) {
                sample *= 2
            }
            return sample
        }
    }
}
