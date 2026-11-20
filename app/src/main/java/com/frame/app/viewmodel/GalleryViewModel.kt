package com.frame.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.frame.app.domain.gallery.PhotoItem
import com.frame.app.domain.gallery.PhotoRepository
import com.frame.app.platform.storage.MediaStorePhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Gallery state: the photo list plus whether the first load has finished. */
data class GalleryUiState(
    val photos: List<PhotoItem> = emptyList(),
    val isLoaded: Boolean = false,
) {
    /** Newest photo, used for the camera's last-photo thumbnail. */
    val latest: PhotoItem? get() = photos.firstOrNull()
}

/**
 * Holds the FRAME photo list. [refresh] re-reads MediaStore; call it on app
 * start, after each capture, and when the gallery opens so it stays current.
 */
class GalleryViewModel(
    private val repository: PhotoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val photos = repository.loadPhotos()
            _state.value = GalleryUiState(photos = photos, isLoaded = true)
        }
    }
}

class GalleryViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val repository = MediaStorePhotoRepository(context.applicationContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return GalleryViewModel(repository) as T
    }
}
