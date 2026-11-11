package com.frame.app.platform.prefs

import com.frame.app.domain.model.CameraMode
import com.frame.app.domain.service.OverlaySettings
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for camera mode + overlay settings. Abstracted so the
 * ViewModel depends on an interface (testable with an in-memory fake) rather
 * than the DataStore-backed [OverlayPreferences] directly.
 */
interface OverlayPreferencesStore {
    val state: Flow<OverlayPrefsState>
    suspend fun setMode(mode: CameraMode)
    suspend fun setSettings(settings: OverlaySettings)
}
