package com.frame.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.frame.app.platform.prefs.StylePreferences

/** Builds [StyleViewModel] with a DataStore-backed store on the app context. */
class StyleViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val preferences = StylePreferences(context.applicationContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(StyleViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return StyleViewModel(preferences) as T
    }
}
