package com.frame.app.platform.camera

/**
 * Snapshot of the camera's current capabilities and settings, surfaced to the
 * UI via [CameraController.state].
 */
data class CameraState(
    val isReady: Boolean = false,
    val lens: CameraLens = CameraLens.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val torchEnabled: Boolean = false,
    val hasFlashUnit: Boolean = false,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val linearZoom: Float = 0f,
    val exposureIndex: Int = 0,
    val minExposureIndex: Int = 0,
    val maxExposureIndex: Int = 0,
    /** Outcome of the most recent tap-to-focus request. */
    val lastFocusResult: FocusResult = FocusResult.NONE,
    /** Non-null when the camera failed to bind/operate. */
    val error: String? = null,
) {
    val supportsZoom: Boolean get() = maxZoomRatio > minZoomRatio
    val supportsExposure: Boolean get() = maxExposureIndex > minExposureIndex
}

/** Result of a tap-to-focus/metering request. */
enum class FocusResult {
    NONE,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    UNSUPPORTED,
}
