package com.frame.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.frame.app.platform.prefs.OverlayPreferences

/**
 * Builds [OverlayViewModel] with an [OverlayPreferences] bound to the app
 * context. Kept trivial for Phase 1; a DI framework can replace it later without
 * touching the ViewModel.
 */
class OverlayViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val preferences = OverlayPreferences(context.applicationContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(OverlayViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return OverlayViewModel(preferences) as T
    }
}
