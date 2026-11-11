package com.frame.app.platform.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Saves photos to the device gallery via [MediaStore], under
 * `Pictures/FRAME`, so they appear in the user's gallery app.
 *
 * Handles the storage model split:
 *  - API 29+ : scoped storage. We set RELATIVE_PATH + IS_PENDING, write bytes,
 *              then clear IS_PENDING in [finalize]. No storage permission needed.
 *  - API 26-28: legacy. We set the absolute DATA path under the public Pictures
 *              dir (requires WRITE_EXTERNAL_STORAGE, declared maxSdk=28 in the
 *              manifest). IS_PENDING doesn't exist, so finalize is a no-op.
 */
class MediaStorePhotoStore(
    private val context: Context,
) : PhotoStore {

    private val resolver get() = context.contentResolver

    override fun createDestination(displayName: String): PhotoDestination? {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_JPEG)
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val albumDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    ALBUM,
                ).apply { if (!exists()) mkdirs() }
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, File(albumDir, displayName).absolutePath)
            }
        }

        val uri = resolver.insert(collection, values) ?: return null
        return PhotoDestination(uri)
    }

    override fun finalize(destination: PhotoDestination) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(destination.uri, values, null, null)
        }
        // API < 29: file is already visible; a media scan is triggered by
        // MediaStore.insert with a DATA path, so nothing more to do.
    }

    override fun discard(destination: PhotoDestination) {
        runCatching { resolver.delete(destination.uri, null, null) }
    }

    private companion object {
        const val MIME_JPEG = "image/jpeg"
        const val ALBUM = "FRAME"
    }
}
