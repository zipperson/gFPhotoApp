package com.frame.app.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frame.app.domain.model.TiltState
import com.frame.app.platform.camera.CameraLens
import com.frame.app.platform.camera.CameraXController
import com.frame.app.platform.camera.CaptureResult
import com.frame.app.platform.camera.next
import com.frame.app.platform.sensor.DeviceLevelSensor

/**
 * The live camera screen with capture (Task 4).
 *
 * Layout: a full-screen [PreviewView] (hosted via [AndroidView]) with the
 * shutter and transient feedback layered on top in the same [Box] — the slot
 * where overlays (Task 8) and the rest of the controls (Tasks 5-6) will sit.
 */
@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember { CameraXController(context.applicationContext) }
    val cameraState by controller.state.collectAsStateWithLifecycle()

    // Device level/tilt sensor.
    val levelSensor = remember { DeviceLevelSensor(context.applicationContext) }
    val tilt by levelSensor.tilt.collectAsStateWithLifecycle(initialValue = TiltState.UNAVAILABLE)

    // Transient capture state, local to the screen.
    var isCapturing by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    // Auto-dismiss the feedback message after a short delay.
    LaunchedEffect(feedback) {
        if (feedback != null) {
            kotlinx.coroutines.delay(2000)
            feedback = null
        }
    }
    // The capture flash is a brief white overlay.
    LaunchedEffect(showFlash) {
        if (showFlash) {
            kotlinx.coroutines.delay(90)
            showFlash = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller.bind(lifecycleOwner, this)
                }
            },
        )

        // Gesture layer: pinch-to-zoom + tap-to-focus (with focus ring).
        CameraGestureOverlay(
            focusResult = cameraState.lastFocusResult,
            onZoomBy = { scaleFactor ->
                controller.setZoomRatio(cameraState.zoomRatio * scaleFactor)
            },
            onFocus = { nx, ny -> controller.focusAt(nx, ny) },
        )

        // Level / horizon indicator (centered).
        LevelIndicator(tilt = tilt)

        // Capture flash.
        val flashAlpha by animateFloatAsState(
            targetValue = if (showFlash) 0.8f else 0f,
            animationSpec = tween(durationMillis = 90),
            label = "captureFlash",
        )
        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha)),
            )
        }

        // Shutter row, pinned to the bottom.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ShutterButton(
                enabled = cameraState.isReady && !isCapturing,
                onCapture = {
                    isCapturing = true
                    showFlash = true
                    controller.capturePhoto { result ->
                        isCapturing = false
                        feedback = when (result) {
                            is CaptureResult.Success -> "Photo saved"
                            is CaptureResult.Failure -> result.message
                        }
                    }
                },
            )
        }

        // Top control bar: flash, torch, lens switch.
        CameraTopControls(
            flashMode = cameraState.flashMode,
            torchEnabled = cameraState.torchEnabled,
            hasFlashUnit = cameraState.hasFlashUnit,
            lens = cameraState.lens,
            onCycleFlash = { controller.setFlashMode(cameraState.flashMode.next()) },
            onToggleTorch = { controller.setTorch(!cameraState.torchEnabled) },
            onSwitchLens = {
                val target = if (cameraState.lens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
                controller.setLens(target)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 12.dp),
        )

        // Zoom readout, shown only when zoomed past the minimum.
        if (cameraState.zoomRatio > cameraState.minZoomRatio + 0.05f) {
            ZoomIndicator(
                zoomRatio = cameraState.zoomRatio,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp),
            )
        }

        // Exposure slider, sitting just above the shutter when supported.
        if (cameraState.supportsExposure) {
            ExposureSlider(
                index = cameraState.exposureIndex,
                minIndex = cameraState.minExposureIndex,
                maxIndex = cameraState.maxExposureIndex,
                onIndexChange = { controller.setExposureIndex(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 128.dp),
            )
        }

        // Transient confirmation / error.
        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
        ) {
            CaptureFeedbackPill(text = feedback.orEmpty())
        }

        cameraState.error?.let { message ->
            CameraErrorNotice(message = message)
        }
    }
}

@Composable
private fun CaptureFeedbackPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.large,
            )
            .fillMaxWidth(0.7f)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
