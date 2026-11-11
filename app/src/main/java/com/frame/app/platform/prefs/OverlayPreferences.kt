package com.frame.app.platform.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.frame.app.domain.model.CameraMode
import com.frame.app.domain.service.OverlaySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** App-level DataStore instance (one per process). */
private val Context.overlayDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "frame_overlay_prefs",
)

/** Bundles the persisted mode with its (possibly customized) overlay settings. */
data class OverlayPrefsState(
    val mode: CameraMode,
    val settings: OverlaySettings,
)

/**
 * Persists the camera mode and overlay settings via DataStore so the user's
 * choices survive restarts. Reads surface as a [Flow]; writes are suspend.
 *
 * The stored settings are seeded from the mode's preset the first time, then
 * updated independently as the user tweaks individual guides/opacity.
 */
class OverlayPreferences(private val context: Context) : OverlayPreferencesStore {

    override val state: Flow<OverlayPrefsState> = context.overlayDataStore.data.map { prefs ->
        val mode = runCatching {
            CameraMode.valueOf(prefs[KEY_MODE] ?: CameraMode.GUIDED.name)
        }.getOrDefault(CameraMode.GUIDED)

        val preset = mode.toPreset()
        val settings = OverlaySettings(
            showThirds = prefs[KEY_THIRDS] ?: preset.showThirds,
            showCenterCrosshair = prefs[KEY_CROSSHAIR] ?: preset.showCenterCrosshair,
            showHorizon = prefs[KEY_HORIZON] ?: preset.showHorizon,
            showSubjectBox = prefs[KEY_SUBJECT_BOX] ?: preset.showSubjectBox,
            showReferenceOutline = prefs[KEY_REFERENCE] ?: preset.showReferenceOutline,
            opacity = prefs[KEY_OPACITY] ?: preset.opacity,
        )
        OverlayPrefsState(mode = mode, settings = settings)
    }

    /** Switch mode and reset overlay settings to that mode's preset. */
    override suspend fun setMode(mode: CameraMode) {
        val preset = mode.toPreset()
        context.overlayDataStore.edit { p ->
            p[KEY_MODE] = mode.name
            p[KEY_THIRDS] = preset.showThirds
            p[KEY_CROSSHAIR] = preset.showCenterCrosshair
            p[KEY_HORIZON] = preset.showHorizon
            p[KEY_SUBJECT_BOX] = preset.showSubjectBox
            p[KEY_REFERENCE] = preset.showReferenceOutline
            p[KEY_OPACITY] = preset.opacity
        }
    }

    /** Persist a customized settings set (keeps the current mode). */
    override suspend fun setSettings(settings: OverlaySettings) {
        context.overlayDataStore.edit { p ->
            p[KEY_THIRDS] = settings.showThirds
            p[KEY_CROSSHAIR] = settings.showCenterCrosshair
            p[KEY_HORIZON] = settings.showHorizon
            p[KEY_SUBJECT_BOX] = settings.showSubjectBox
            p[KEY_REFERENCE] = settings.showReferenceOutline
            p[KEY_OPACITY] = settings.opacity.coerceIn(0f, 1f)
        }
    }

    private companion object {
        val KEY_MODE = stringPreferencesKey("mode")
        val KEY_THIRDS = booleanPreferencesKey("show_thirds")
        val KEY_CROSSHAIR = booleanPreferencesKey("show_crosshair")
        val KEY_HORIZON = booleanPreferencesKey("show_horizon")
        val KEY_SUBJECT_BOX = booleanPreferencesKey("show_subject_box")
        val KEY_REFERENCE = booleanPreferencesKey("show_reference")
        val KEY_OPACITY = floatPreferencesKey("opacity")
    }
}

/** Default overlay preset for a mode. */
fun CameraMode.toPreset(): OverlaySettings = when (this) {
    CameraMode.MINIMAL -> OverlaySettings.MINIMAL
    CameraMode.GUIDED -> OverlaySettings.GUIDED
}
