package com.frame.app.platform.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executor

/**
 * CameraX-backed [CameraController].
 *
 * Responsibilities in Task 3: obtain the [ProcessCameraProvider], bind the
 * Preview use case to a supplied [LifecycleOwner] + [PreviewView], and publish
 * readiness through [state]. Later tasks add ImageCapture (Task 4), lens/flash
 * (Task 5), zoom/focus/exposure (Task 6), and ImageAnalysis (Task 10) by
 * extending [bind]; the public surface stays the same.
 *
 * Lifecycle: binding is tied to the provided [LifecycleOwner], so CameraX
 * automatically starts/stops the camera with the UI and releases it on destroy.
 * That keeps us leak-free without manual teardown in the common case.
 */
class CameraXController(
    private val appContext: Context,
) : CameraController {

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var boundCamera: androidx.camera.core.Camera? = null

    private var currentSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private val _state = MutableStateFlow(CameraState())
    override val state: StateFlow<CameraState> = _state.asStateFlow()

    /**
     * Acquire the camera provider (async) and bind use cases to [lifecycleOwner]
     * for display in [previewView]. Safe to call again (e.g. after a lens switch
     * or config change); it rebinds cleanly.
     */
    fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider
                bindUseCases(provider, lifecycleOwner, previewView)
            } catch (t: Throwable) {
                _state.update { it.copy(isReady = false, error = t.message ?: "Camera init failed") }
            }
        }, mainExecutor)
    }

    private fun bindUseCases(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        val previewUseCase = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        preview = previewUseCase

        try {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                currentSelector,
                previewUseCase,
            )
            boundCamera = camera
            publishReady(camera)
        } catch (t: Throwable) {
            _state.update {
                it.copy(isReady = false, error = t.message ?: "Failed to bind camera use cases")
            }
        }
    }

    /** Reflect the bound camera's capabilities into [state]. */
    private fun publishReady(camera: androidx.camera.core.Camera) {
        val zoom = camera.cameraInfo.zoomState.value
        _state.update {
            it.copy(
                isReady = true,
                lens = currentSelector.toLens(),
                hasFlashUnit = camera.cameraInfo.hasFlashUnit(),
                zoomRatio = zoom?.zoomRatio ?: 1f,
                minZoomRatio = zoom?.minZoomRatio ?: 1f,
                maxZoomRatio = zoom?.maxZoomRatio ?: 1f,
                linearZoom = zoom?.linearZoom ?: 0f,
                error = null,
            )
        }
    }

    /** Release the camera provider binding. Usually unnecessary (lifecycle does
     *  this), but exposed for explicit teardown in tests or edge cases. */
    fun unbind() {
        cameraProvider?.unbindAll()
        boundCamera = null
        _state.update { it.copy(isReady = false) }
    }

    // --- CameraController members. Task 3 implements only readiness/preview; the
    //     control methods below land in Tasks 5-6. They no-op for now so the
    //     interface is satisfied and the UI can compile against the full API. ---

    override fun setLens(lens: CameraLens) {
        // Task 5
    }

    override fun setFlashMode(mode: FlashMode) {
        // Task 5
    }

    override fun setTorch(enabled: Boolean) {
        // Task 5
    }

    override fun setZoomRatio(ratio: Float) {
        // Task 6
    }

    override fun setLinearZoom(value: Float) {
        // Task 6
    }

    override fun focusAt(x: Float, y: Float) {
        // Task 6
    }

    override fun setExposureIndex(index: Int) {
        // Task 6
    }

    override fun capturePhoto(onResult: (CaptureResult) -> Unit) {
        // Task 4
        onResult(CaptureResult.Failure("Capture not implemented yet"))
    }
}

private fun CameraSelector.toLens(): CameraLens =
    if (this == CameraSelector.DEFAULT_FRONT_CAMERA) CameraLens.FRONT else CameraLens.BACK
