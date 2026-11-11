package com.frame.app.platform.storage

import android.net.Uri

/**
 * Abstraction over where captured photos are written. Keeps MediaStore/scoped-
 * storage details out of the camera controller and lets capture be unit-tested
 * or redirected (e.g. app-private storage) without touching capture logic.
 */
interface PhotoStore {

    /**
     * Create a destination for a new JPEG and return the handle needed to write
     * to it. The caller writes bytes, then calls [finalize].
     */
    fun createDestination(displayName: String): PhotoDestination?

    /**
     * Mark a destination as complete and visible to the gallery. On API 29+ this
     * clears the IS_PENDING flag; on older APIs it's a no-op.
     */
    fun finalize(destination: PhotoDestination)

    /** Remove a partially-written entry if capture failed after [createDestination]. */
    fun discard(destination: PhotoDestination)
}

/**
 * A pending photo target.
 *
 * @property uri the MediaStore content URI to write image bytes into
 */
data class PhotoDestination(
    val uri: Uri,
)
