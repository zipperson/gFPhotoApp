package com.frame.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frame.app.domain.scene.SceneAdvice
import com.frame.app.domain.scene.SceneAnalysisResult
import com.frame.app.domain.scene.SceneAnalyzer
import com.frame.app.domain.scene.SceneContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the on-demand AI scene advice sheet.
 */
sealed interface SceneAdviceUiState {
    data object Idle : SceneAdviceUiState
    data object Loading : SceneAdviceUiState
    data class Ready(val result: SceneAnalysisResult) : SceneAdviceUiState
}

/**
 * Orchestrates the on-demand AI tip: capture a frame -> analyze with the current
 * composition context -> expose loading/result state.
 *
 * Two pieces of state have different lifetimes on purpose:
 *  - [state] is the *sheet* (loading / result / error); dismissing it returns to Idle.
 *  - [activeAdvice] is the last successful advice. It survives dismissing the
 *    sheet AND navigating away from the camera (e.g. to the gallery), so the AI
 *    target stays on screen and the advice can be reopened until the user clears it.
 *
 * Dependencies are injected so this is unit-testable without camera/network.
 * The frame source is swappable ([setCaptureSource]) because the camera
 * controller is recreated whenever the camera screen is re-entered, while this
 * ViewModel outlives it.
 *
 * @param analyzer      the (provider-agnostic) scene analyzer
 * @param captureFrame  suspending frame grab returning JPEG bytes (null on fail)
 * @param isOnline      connectivity check; when false we short-circuit to Offline
 */
class SceneAdviceViewModel(
    private val analyzer: SceneAnalyzer,
    private var captureFrame: suspend () -> ByteArray?,
    private val isOnline: () -> Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow<SceneAdviceUiState>(SceneAdviceUiState.Idle)
    val state: StateFlow<SceneAdviceUiState> = _state.asStateFlow()

    private val _activeAdvice = MutableStateFlow<SceneAdvice?>(null)
    /** Last successful advice; drives the on-screen AI target + the reopen action. */
    val activeAdvice: StateFlow<SceneAdvice?> = _activeAdvice.asStateFlow()

    private var inFlight: Job? = null

    /**
     * Point frame capture at the *current* camera. Call whenever the camera
     * controller is (re)created; otherwise requests would hit a stale, released
     * camera after returning from another screen.
     */
    fun setCaptureSource(source: suspend () -> ByteArray?) {
        captureFrame = source
    }

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
            if (result is SceneAnalysisResult.Success) {
                _activeAdvice.value = result.advice
            }
            _state.value = SceneAdviceUiState.Ready(result)
        }
    }

    /** Close the sheet. The active advice (and its on-screen target) is kept. */
    fun dismiss() {
        inFlight?.cancel()
        _state.value = SceneAdviceUiState.Idle
    }

    /** Reopen the sheet with the last successful advice (no new request). */
    fun reopen() {
        val advice = _activeAdvice.value ?: return
        _state.value = SceneAdviceUiState.Ready(SceneAnalysisResult.Success(advice))
    }

    /** Clear the active advice, its on-screen target, and close the sheet. */
    fun clearAdvice() {
        inFlight?.cancel()
        _activeAdvice.value = null
        _state.value = SceneAdviceUiState.Idle
    }
}
