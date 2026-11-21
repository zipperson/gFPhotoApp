package com.frame.app.platform.storage

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.frame.app.domain.gallery.PhotoItem
import com.frame.app.domain.gallery.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads Siyot's photos back from MediaStore (the Pictures/Siyot album that
 * [MediaStorePhotoStore] writes to), newest first.
 *
 * Filtering differs by API level:
 *  - API 29+: RELATIVE_PATH = "Pictures/FRAME/" (scoped storage; an app can read
 *    media it created without any storage permission).
 *  - API 26-28: legacy DATA path contains "/Pictures/FRAME/".
 */
class MediaStorePhotoRepository(
    private val context: Context,
) : PhotoRepository {

    override suspend fun loadPhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
        )

        // Albums: the current "Siyot" folder, plus "FRAME", the app's earlier
        // name, so photos taken before the rename still show up.
        val (selection, args) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val col = MediaStore.Images.Media.RELATIVE_PATH
            "$col LIKE ? OR $col LIKE ?" to arrayOf("%Pictures/Siyot%", "%Pictures/FRAME%")
        } else {
            @Suppress("DEPRECATION")
            val col = MediaStore.Images.Media.DATA
            "$col LIKE ? OR $col LIKE ?" to arrayOf("%/Pictures/Siyot/%", "%/Pictures/FRAME/%")
        }

        val results = mutableListOf<PhotoItem>()
        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                args,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    results += PhotoItem(
                        id = id,
                        uriString = ContentUris.withAppendedId(collection, id).toString(),
                        dateAddedSec = cursor.getLong(dateCol),
                    )
                }
            }
        }
        results
    }
}
