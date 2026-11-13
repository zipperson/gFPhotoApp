package com.frame.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.frame.app.BuildConfig
import com.frame.app.platform.net.Connectivity
import com.frame.app.platform.scene.OpenAiSceneAnalyzer

/**
 * Builds [SceneAdviceViewModel] with the OpenAI analyzer (key from BuildConfig),
 * a connectivity check, and the camera's frame-capture function supplied by the
 * caller (it owns the controller).
 */
class SceneAdviceViewModelFactory(
    appContext: Context,
    private val captureFrame: suspend () -> ByteArray?,
) : ViewModelProvider.Factory {

    private val analyzer = OpenAiSceneAnalyzer(apiKey = BuildConfig.OPENAI_API_KEY)
    private val onlineCheck: () -> Boolean = { Connectivity.isOnline(appContext) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SceneAdviceViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SceneAdviceViewModel(
            analyzer = analyzer,
            captureFrame = captureFrame,
            isOnline = onlineCheck,
        ) as T
    }
}
