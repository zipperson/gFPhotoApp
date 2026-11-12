package com.frame.app.platform.prefs

import com.frame.app.domain.model.PhotoStyle
import kotlinx.coroutines.flow.Flow

/** Bundles the selected style with whether the user has made an explicit choice. */
data class StylePrefsState(
    val style: PhotoStyle,
    /** False until the user picks a style (drives the first-run selection screen). */
    val hasChosen: Boolean,
)

/**
 * Persistence contract for the selected [PhotoStyle]. Abstracted so the
 * ViewModel can be unit-tested with an in-memory fake instead of DataStore.
 */
interface StylePreferencesStore {
    val state: Flow<StylePrefsState>
    suspend fun setStyle(style: PhotoStyle)
}
