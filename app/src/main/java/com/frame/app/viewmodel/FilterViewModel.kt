package com.frame.app.viewmodel

import androidx.lifecycle.ViewModel
import com.frame.app.domain.model.PhotoFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the selected [PhotoFilter]. Lives in a ViewModel (not screen state) so
 * the choice survives a trip to the gallery and back. It is not persisted: the
 * app opens on the Original look.
 */
class FilterViewModel : ViewModel() {
    private val _filter = MutableStateFlow(PhotoFilter.NONE)
    val filter: StateFlow<PhotoFilter> = _filter.asStateFlow()

    fun setFilter(filter: PhotoFilter) {
        _filter.value = filter
    }
}
