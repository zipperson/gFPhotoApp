package com.frame.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frame.app.domain.model.CameraMode
import com.frame.app.domain.service.OverlaySettings
import com.frame.app.platform.prefs.OverlayPreferencesStore
import com.frame.app.platform.prefs.OverlayPrefsState
import com.frame.app.platform.prefs.toPreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the camera mode + overlay settings, backed by [OverlayPreferences] for
 * persistence. The UI observes [state]; toggles/opacity edits write through to
 * DataStore so they survive restarts.
 */
class OverlayViewModel(
    private val preferences: OverlayPreferencesStore,
) : ViewModel() {

    val state: StateFlow<OverlayPrefsState> = preferences.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OverlayPrefsState(
            mode = CameraMode.GUIDED,
            settings = CameraMode.GUIDED.toPreset(),
        ),
    )

    fun setMode(mode: CameraMode) {
        viewModelScope.launch { preferences.setMode(mode) }
    }

    fun toggleMode() {
        val next = if (state.value.mode == CameraMode.GUIDED) CameraMode.MINIMAL else CameraMode.GUIDED
        setMode(next)
    }

    fun updateSettings(transform: (OverlaySettings) -> OverlaySettings) {
        val updated = transform(state.value.settings)
        viewModelScope.launch { preferences.setSettings(updated) }
    }

    fun setOpacity(opacity: Float) {
        updateSettings { it.copy(opacity = opacity) }
    }
}
