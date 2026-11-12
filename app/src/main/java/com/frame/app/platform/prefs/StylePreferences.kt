package com.frame.app.platform.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.frame.app.domain.model.PhotoStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.styleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "frame_style_prefs",
)

/**
 * DataStore-backed [StylePreferencesStore]. Persists the chosen [PhotoStyle];
 * absence of a stored value means the user hasn't chosen yet (first run).
 */
class StylePreferences(private val context: Context) : StylePreferencesStore {

    override val state: Flow<StylePrefsState> = context.styleDataStore.data.map { prefs ->
        val stored = prefs[KEY_STYLE]
        val style = stored?.let {
            runCatching { PhotoStyle.valueOf(it) }.getOrNull()
        } ?: PhotoStyle.DEFAULT
        StylePrefsState(style = style, hasChosen = stored != null)
    }

    override suspend fun setStyle(style: PhotoStyle) {
        context.styleDataStore.edit { it[KEY_STYLE] = style.name }
    }

    private companion object {
        val KEY_STYLE = stringPreferencesKey("photo_style")
    }
}
