package com.frame.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frame.app.domain.model.PhotoStyle
import com.frame.app.platform.prefs.StylePreferencesStore
import com.frame.app.platform.prefs.StylePrefsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the selected [PhotoStyle], backed by [StylePreferencesStore]. The UI
 * observes [state] (style + whether the user has chosen); setting a style
 * persists it.
 */
class StyleViewModel(
    private val preferences: StylePreferencesStore,
) : ViewModel() {

    val state: StateFlow<StylePrefsState> = preferences.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StylePrefsState(style = PhotoStyle.DEFAULT, hasChosen = false),
    )

    fun setStyle(style: PhotoStyle) {
        viewModelScope.launch { preferences.setStyle(style) }
    }
}
