package com.frame.app.platform.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.frame.app.platform.storage.MediaStorePhotoStore
import com.frame.app.platform.storage.PhotoNaming
import com.frame.app.platform.storage.PhotoStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    private val photoStore: PhotoStore = MediaStorePhotoStore(appContext),
    /**
     * Optional frame analyzer for the ImageAnalysis use case. Null in a pure
     * preview/capture configuration; Phase 3 passes an ML Kit analyzer here.
     * Task 10 passes a no-op [com.frame.app.platform.pose.FrameAnalyzer] to
     * prove the seam.
     */
    private val frameAnalyzer: ImageAnalysis.Analyzer? = null,
) : CameraController {

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)
    // Dedicated single thread for frame analysis, off the main thread.
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var boundCamera: androidx.camera.core.Camera? = null

    // Retained so a lens switch can rebind against the same UI surface.
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null

    private var currentSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var currentFlashMode: FlashMode = FlashMode.OFF

    private val _state = MutableStateFlow(CameraState())
    override val state: StateFlow<CameraState> = _state.asStateFlow()

    /**
     * Acquire the camera provider (async) and bind use cases to [lifecycleOwner]
     * for display in [previewView]. Safe to call again (e.g. after a lens switch
     * or config change); it rebinds cleanly.
     */
    fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView
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

    /** Rebind use cases using the retained lifecycle/preview (after a lens switch). */
    private fun rebind() {
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        val view = previewView ?: return
        bindUseCases(provider, owner, view)
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

        val captureUseCase = ImageCapture.Builder()
            // Prioritize low shutter latency for a responsive feel; FRAME is a
            // composition tool, so capture should feel immediate.
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
            .apply { flashMode = currentFlashMode.toImageCaptureFlashMode() }
        imageCapture = captureUseCase

        // ImageAnalysis use case (Phase 3 seam). Only bound when an analyzer is
        // supplied. KEEP_ONLY_LATEST drops stale frames so detection never
        // builds a backlog; analysis runs on its own executor off the main thread.
        val analysisUseCase = frameAnalyzer?.let { analyzer ->
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, analyzer) }
        }
        imageAnalysis = analysisUseCase

        try {
            provider.unbindAll()
            val useCases = listOfNotNull(previewUseCase, captureUseCase, analysisUseCase)
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                currentSelector,
                *useCases.toTypedArray(),
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
        val info = camera.cameraInfo
        val zoom = info.zoomState.value
        val hasFlash = info.hasFlashUnit()
        val exposure = info.exposureState
        val exposureRange = exposure.exposureCompensationRange
        _state.update {
            it.copy(
                isReady = true,
                lens = currentSelector.toLens(),
                flashMode = currentFlashMode,
                // A lens with no flash unit can't hold a torch on; reset.
                torchEnabled = if (hasFlash) it.torchEnabled else false,
                hasFlashUnit = hasFlash,
                zoomRatio = zoom?.zoomRatio ?: 1f,
                minZoomRatio = zoom?.minZoomRatio ?: 1f,
                maxZoomRatio = zoom?.maxZoomRatio ?: 1f,
                linearZoom = zoom?.linearZoom ?: 0f,
                exposureIndex = if (exposure.isExposureCompensationSupported) {
                    exposure.exposureCompensationIndex
                } else 0,
                minExposureIndex = if (exposure.isExposureCompensationSupported) exposureRange.lower else 0,
                maxExposureIndex = if (exposure.isExposureCompensationSupported) exposureRange.upper else 0,
                error = null,
            )
        }
    }

    /** Release the camera provider binding. Usually unnecessary (lifecycle does
     *  this), but exposed for explicit teardown in tests or edge cases. */
    fun unbind() {
        imageAnalysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        boundCamera = null
        _state.update { it.copy(isReady = false) }
    }

    /** Fully release resources, including the analysis thread. Call when the
     *  controller is permanently done (not on a routine lifecycle stop). */
    fun shutdown() {
        unbind()
        analysisExecutor.shutdown()
    }

    // --- CameraController members. Task 3 implements only readiness/preview; the
    //     control methods below land in Tasks 5-6. They no-op for now so the
    //     interface is satisfied and the UI can compile against the full API. ---

    override fun setLens(lens: CameraLens) {
        val selector = when (lens) {
            CameraLens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraLens.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        if (selector == currentSelector) return
        currentSelector = selector
        // Front cameras generally have no flash; torch can't survive the switch.
        _state.update { it.copy(torchEnabled = false) }
        rebind()
    }

    override fun setFlashMode(mode: FlashMode) {
        currentFlashMode = mode
        imageCapture?.flashMode = mode.toImageCaptureFlashMode()
        _state.update { it.copy(flashMode = mode) }
    }

    override fun setTorch(enabled: Boolean) {
        val camera = boundCamera ?: return
        if (!camera.cameraInfo.hasFlashUnit()) return

        // enableTorch returns a future that can complete exceptionally. Reflect
        // the *actual* torch state CameraX reports rather than assuming success,
        // so the icon tracks the hardware.
        val future = camera.cameraControl.enableTorch(enabled)
        future.addListener({
            val actual = runCatching {
                future.get()
                camera.cameraInfo.torchState.value == TorchState.ON
            }.getOrDefault(false)
            _state.update { it.copy(torchEnabled = actual) }
        }, mainExecutor)
    }

    override fun setZoomRatio(ratio: Float) {
        val camera = boundCamera ?: return
        val zoom = camera.cameraInfo.zoomState.value ?: return
        val clamped = CameraMath.clampZoom(ratio, zoom.minZoomRatio, zoom.maxZoomRatio)
        camera.cameraControl.setZoomRatio(clamped)
        // Reflect optimistically; zoomState also updates, but this keeps the UI
        // responsive during a pinch without waiting for the LiveData tick.
        _state.update { it.copy(zoomRatio = clamped) }
    }

    override fun setLinearZoom(value: Float) {
        val camera = boundCamera ?: return
        val clamped = CameraMath.clampLinearZoom(value)
        camera.cameraControl.setLinearZoom(clamped)
        _state.update { it.copy(linearZoom = clamped) }
    }

    override fun focusAt(x: Float, y: Float) {
        val camera = boundCamera ?: return
        val view = previewView ?: return
        // Build a metering point in the preview's coordinate space from the
        // normalized tap, then request AF+AE metering there.
        val factory = view.meteringPointFactory
        val point = factory.createPoint(
            CameraMath.clampNormalized(x) * view.width,
            CameraMath.clampNormalized(y) * view.height,
        )
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
        ).setAutoCancelDuration(FOCUS_AUTO_CANCEL_SECONDS, TimeUnit.SECONDS).build()

        // Guard against devices/points that don't support the requested metering.
        if (!camera.cameraInfo.isFocusMeteringSupported(action)) {
            _state.update { it.copy(lastFocusResult = FocusResult.UNSUPPORTED) }
            return
        }

        _state.update { it.copy(lastFocusResult = FocusResult.IN_PROGRESS) }
        val future = camera.cameraControl.startFocusAndMetering(action)
        future.addListener({
            val result = runCatching {
                if (future.get().isFocusSuccessful) FocusResult.SUCCESS else FocusResult.FAILED
            }.getOrDefault(FocusResult.FAILED)
            _state.update { it.copy(lastFocusResult = result) }
        }, mainExecutor)
    }

    override fun setExposureIndex(index: Int) {
        val camera = boundCamera ?: return
        val exposure = camera.cameraInfo.exposureState
        if (!exposure.isExposureCompensationSupported) return
        val range = exposure.exposureCompensationRange
        val clamped = CameraMath.clampExposure(index, range.lower, range.upper)
        camera.cameraControl.setExposureCompensationIndex(clamped)
        _state.update { it.copy(exposureIndex = clamped) }
    }

    override fun capturePhoto(onResult: (CaptureResult) -> Unit) {
        val capture = imageCapture
        if (capture == null) {
            onResult(CaptureResult.Failure("Camera not ready"))
            return
        }

        val destination = photoStore.createDestination(PhotoNaming.jpegFileName())
        if (destination == null) {
            onResult(CaptureResult.Failure("Could not create a file to save the photo"))
            return
        }

        val outputStream = runCatching {
            appContext.contentResolver.openOutputStream(destination.uri)
        }.getOrNull()
        if (outputStream == null) {
            photoStore.discard(destination)
            onResult(CaptureResult.Failure("Could not open the photo file for writing"))
            return
        }

        val options = ImageCapture.OutputFileOptions.Builder(outputStream).build()

        capture.takePicture(
            options,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runCatching { outputStream.close() }
                    photoStore.finalize(destination)
                    onResult(CaptureResult.Success(destination.uri.toString()))
                }

                override fun onError(exception: ImageCaptureException) {
                    runCatching { outputStream.close() }
                    photoStore.discard(destination)
                    onResult(
                        CaptureResult.Failure(
                            message = exception.message ?: "Failed to capture photo",
                            cause = exception,
                        )
                    )
                }
            },
        )
    }

    /**
     * Capture one frame to memory (not the gallery) for AI scene analysis,
     * downscaled + JPEG-compressed to keep the upload small/cheap. Delivers the
     * bytes on a background thread via [onBytes]; null on failure.
     */
    fun captureForAnalysis(onBytes: (ByteArray?) -> Unit) {
        val capture = imageCapture ?: run { onBytes(null); return }
        capture.takePicture(
            analysisExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes = runCatching {
                        FrameEncoder.toDownscaledJpeg(image, MAX_ANALYSIS_EDGE, ANALYSIS_JPEG_QUALITY)
                    }.getOrNull()
                    image.close()
                    onBytes(bytes)
                }

                override fun onError(exception: ImageCaptureException) {
                    onBytes(null)
                }
            },
        )
    }

    private companion object {
        const val FOCUS_AUTO_CANCEL_SECONDS = 4L
        // Keep the analysis upload small: longest edge ~1024px, moderate quality.
        const val MAX_ANALYSIS_EDGE = 1024
        const val ANALYSIS_JPEG_QUALITY = 80
    }
}

private fun CameraSelector.toLens(): CameraLens =
    if (this == CameraSelector.DEFAULT_FRONT_CAMERA) CameraLens.FRONT else CameraLens.BACK

private fun FlashMode.toImageCaptureFlashMode(): Int = when (this) {
    FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
    FlashMode.ON -> ImageCapture.FLASH_MODE_ON
    FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
}
