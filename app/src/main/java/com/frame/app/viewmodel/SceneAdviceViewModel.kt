package com.frame.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frame.app.domain.scene.SceneAnalysisResult
import com.frame.app.domain.scene.SceneAnalyzer
import com.frame.app.domain.scene.SceneContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the on-demand AI scene advice feature.
 */
sealed interface SceneAdviceUiState {
    data object Idle : SceneAdviceUiState
    data object Loading : SceneAdviceUiState
    data class Ready(val result: SceneAnalysisResult) : SceneAdviceUiState
}

/**
 * Orchestrates the on-demand AI tip: capture a frame -> analyze with the current
 * composition context -> expose loading/result state. Debounces repeat taps and
 * cancels any in-flight request when a new one starts.
 *
 * Dependencies are injected so this is unit-testable without camera/network:
 * @param analyzer      the (provider-agnostic) scene analyzer
 * @param captureFrame  suspending frame grab returning JPEG bytes (null on fail)
 * @param isOnline      connectivity check; when false we short-circuit to Offline
 */
class SceneAdviceViewModel(
    private val analyzer: SceneAnalyzer,
    private val captureFrame: suspend () -> ByteArray?,
    private val isOnline: () -> Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow<SceneAdviceUiState>(SceneAdviceUiState.Idle)
    val state: StateFlow<SceneAdviceUiState> = _state.asStateFlow()

    private var inFlight: Job? = null

    /** Request advice for the current frame + [context]. No-op while loading. */
    fun requestAdvice(context: SceneContext) {
        if (_state.value is SceneAdviceUiState.Loading) return

        inFlight?.cancel()
        inFlight = viewModelScope.launch {
            _state.value = SceneAdviceUiState.Loading

            if (!isOnline()) {
                _state.value = SceneAdviceUiState.Ready(SceneAnalysisResult.Offline)
                return@launch
            }

            val bytes = captureFrame()
            if (bytes == null) {
                _state.value = SceneAdviceUiState.Ready(
                    SceneAnalysisResult.Error("Couldn't capture a frame to analyze.")
                )
                return@launch
            }

            val result = analyzer.analyze(bytes, context)
            _state.value = SceneAdviceUiState.Ready(result)
        }
    }

    /** Dismiss the result / return to idle. */
    fun dismiss() {
        inFlight?.cancel()
        _state.value = SceneAdviceUiState.Idle
    }
}
