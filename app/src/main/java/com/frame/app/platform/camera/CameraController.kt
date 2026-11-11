package com.frame.app.platform.camera

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the device camera. CameraX implements this in Task 3; the
 * rest of the app (ViewModel/UI) depends only on this interface so the camera
 * stack is swappable and the ViewModel is testable with a fake.
 *
 * Methods are deliberately high-level (lens, flash, zoom, focus, exposure,
 * capture) rather than exposing CameraX use cases, keeping the vendor API out
 * of the app's core.
 */
interface CameraController {

    /** Observable camera state (ready, bound lens, zoom, flash, ranges, errors). */
    val state: StateFlow<CameraState>

    /** Switch between front and rear lenses, rebinding use cases. */
    fun setLens(lens: CameraLens)

    /** Set flash behavior for still capture. */
    fun setFlashMode(mode: FlashMode)

    /** Toggle the torch (continuous light), where supported. */
    fun setTorch(enabled: Boolean)

    /** Set zoom as a ratio within the device's supported range. */
    fun setZoomRatio(ratio: Float)

    /** Set zoom as a linear 0f..1f value across the supported range. */
    fun setLinearZoom(value: Float)

    /**
     * Trigger tap-to-focus/metering at a normalized preview coordinate.
     * (0f,0f) = top-left, (1f,1f) = bottom-right.
     */
    fun focusAt(x: Float, y: Float)

    /** Set exposure compensation as an index within the device's supported range. */
    fun setExposureIndex(index: Int)

    /** Capture a still photo; result delivered via [onResult]. */
    fun capturePhoto(onResult: (CaptureResult) -> Unit)
}

enum class CameraLens { FRONT, BACK }

enum class FlashMode { OFF, ON, AUTO }

/** Outcome of a still capture. */
sealed interface CaptureResult {
    /** Saved successfully; [uri] is a string form of the MediaStore content URI. */
    data class Success(val uri: String) : CaptureResult
    data class Failure(val message: String, val cause: Throwable? = null) : CaptureResult
}
