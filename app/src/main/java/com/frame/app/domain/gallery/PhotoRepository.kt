package com.frame.app.domain.gallery

/**
 * A photo FRAME captured, as shown in the in-app gallery.
 *
 * @property id           stable MediaStore row id
 * @property uriString    content URI string (resolved by the image loader)
 * @property dateAddedSec capture time, seconds since epoch (newest-first sort key)
 */
data class PhotoItem(
    val id: Long,
    val uriString: String,
    val dateAddedSec: Long,
)

/**
 * Source of the user's FRAME photos. Abstracted so the gallery ViewModel is
 * testable with an in-memory fake; the MediaStore implementation lives in the
 * platform layer.
 */
interface PhotoRepository {

    /** All FRAME photos, newest first. */
    suspend fun loadPhotos(): List<PhotoItem>

    /** The most recent FRAME photo, or null if there are none. */
    suspend fun latest(): PhotoItem? = loadPhotos().firstOrNull()
}
