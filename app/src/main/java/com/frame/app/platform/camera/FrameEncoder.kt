package com.frame.app.platform.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Converts a captured [ImageProxy] into downscaled, rotation-corrected JPEG
 * bytes suitable for a small, cheap upload to the scene analyzer.
 *
 * ImageCapture delivers JPEG-format frames, so the first plane's buffer is the
 * encoded JPEG; we decode, scale the longest edge down to [maxEdge], apply the
 * capture rotation, and re-encode at [quality].
 */
object FrameEncoder {

    fun toDownscaledJpeg(image: ImageProxy, maxEdge: Int, quality: Int): ByteArray {
        val buffer = image.planes[0].buffer
        val jpegBytes = ByteArray(buffer.remaining()).also { buffer.get(it) }

        val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: return jpegBytes // fall back to the original if decode fails

        val rotation = image.imageInfo.rotationDegrees
        val scaled = scaleToMaxEdge(decoded, maxEdge)
        val rotated = if (rotation != 0) rotate(scaled, rotation) else scaled

        val out = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, quality, out)

        if (rotated != decoded) rotated.recycle()
        if (scaled != decoded && scaled != rotated) scaled.recycle()
        decoded.recycle()
        return out.toByteArray()
    }

    private fun scaleToMaxEdge(src: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxEdge) return src
        val factor = maxEdge.toFloat() / longest
        val w = (src.width * factor).toInt().coerceAtLeast(1)
        val h = (src.height * factor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }
}
