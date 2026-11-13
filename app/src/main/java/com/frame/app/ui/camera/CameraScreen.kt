package com.frame.app.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frame.app.domain.composition.CompositionTarget
import com.frame.app.domain.composition.DefaultCompositionEngine
import com.frame.app.domain.composition.toBrief
import com.frame.app.domain.composition.toThresholds
import com.frame.app.domain.scene.SceneContext
import com.frame.app.domain.service.TargetSpec
import com.frame.app.domain.model.CameraMode
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState
import com.frame.app.platform.camera.CameraLens
import com.frame.app.platform.camera.CameraXController
import com.frame.app.platform.camera.CaptureResult
import com.frame.app.platform.camera.next
import com.frame.app.platform.overlay.DefaultOverlayRenderer
import com.frame.app.platform.pose.MlKitPoseDetector
import com.frame.app.platform.sensor.DeviceLevelSensor
import com.frame.app.ui.camera.overlay.CompositionOverlay
import com.frame.app.viewmodel.OverlayViewModel
import com.frame.app.viewmodel.OverlayViewModelFactory
import com.frame.app.viewmodel.SceneAdviceUiState
import com.frame.app.viewmodel.SceneAdviceViewModel
import com.frame.app.viewmodel.SceneAdviceViewModelFactory
import com.frame.app.viewmodel.StyleViewModel
import com.frame.app.viewmodel.StyleViewModelFactory

/**
 * The live camera screen with capture (Task 4).
 *
 * Layout: a full-screen [PreviewView] (hosted via [AndroidView]) with the
 * shutter and transient feedback layered on top in the same [Box] — the slot
 * where overlays (Task 8) and the rest of the controls (Tasks 5-6) will sit.
 */
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    onOpenStylePicker: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Phase 3: ML Kit pose detection feeding the ImageAnalysis use case. Its
    // analyzer runs per frame; detected subjects flow out via `results` and into
    // the overlay. (NoOpPoseDetector/FrameAnalyzer remain for tests/fallback.)
    val poseDetector = remember { MlKitPoseDetector() }
    val controller = remember {
        CameraXController(
            appContext = context.applicationContext,
            frameAnalyzer = poseDetector.analyzer,
        )
    }
    val cameraState by controller.state.collectAsStateWithLifecycle()
    val rawSubject by poseDetector.results.collectAsStateWithLifecycle(
        initialValue = SubjectFrame.empty(0L),
    )

    // Composition analysis on/off. Off = stop ML Kit entirely (no detection,
    // no composition) to cut heat/battery; preview + capture keep working.
    var analysisEnabled by remember { mutableStateOf(true) }

    // When analysis is off, present no subject so overlays/feedback clear.
    val subject = if (analysisEnabled) rawSubject else SubjectFrame.empty(0L)

    DisposableEffect(analysisEnabled) {
        if (analysisEnabled) poseDetector.start() else poseDetector.stop()
        onDispose { /* lifecycle stop handled below */ }
    }

    DisposableEffect(Unit) {
        onDispose {
            poseDetector.close()
            controller.shutdown()
        }
    }

    // Keep the detector's lens in sync so front-camera mirroring stays correct.
    LaunchedEffect(cameraState.lens) {
        poseDetector.setLens(cameraState.lens)
    }

    // Device level/tilt sensor.
    val levelSensor = remember { DeviceLevelSensor(context.applicationContext) }
    val tilt by levelSensor.tilt.collectAsStateWithLifecycle(initialValue = TiltState.UNAVAILABLE)

    // Overlay mode + settings (persisted via DataStore in OverlayViewModel).
    val overlayViewModel: OverlayViewModel = viewModel(
        factory = OverlayViewModelFactory(context.applicationContext),
    )
    val overlayPrefs by overlayViewModel.state.collectAsStateWithLifecycle()
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Selected photography style (for the style pill; drives the engine in Task 4).
    val styleViewModel: StyleViewModel = viewModel(
        factory = StyleViewModelFactory(context.applicationContext),
    )
    val stylePrefs by styleViewModel.state.collectAsStateWithLifecycle()

    // AI scene advice (Phase 5): on-demand OpenAI analysis of a captured frame.
    val sceneAdviceViewModel: SceneAdviceViewModel = viewModel(
        factory = SceneAdviceViewModelFactory(
            appContext = context.applicationContext,
            captureFrame = { suspendCaptureFrame(controller) },
        ),
    )
    val sceneAdviceState by sceneAdviceViewModel.state.collectAsStateWithLifecycle()

    // Composition engine, tuned to the selected style. Recreated when the style
    // changes so advice/scoring tracks the chosen look. Pure + cheap.
    val compositionEngine = remember(stylePrefs.style) {
        DefaultCompositionEngine(stylePrefs.style.toThresholds())
    }
    // Evaluate the current (smoothed) subject + tilt. Portrait preview ~ 9:16.
    val composition = compositionEngine.evaluate(
        subject = subject,
        tilt = tilt,
        frameAspectRatio = PREVIEW_ASPECT_RATIO,
    )

    // Target placement guidance: where the subject should sit for this style.
    val target = if (subject.hasSubject) {
        val thresholds = stylePrefs.style.toThresholds()
        val rect = CompositionTarget.idealRectFor(thresholds, subject)
        TargetSpec(
            targetRect = rect,
            subjectCenter = subject.boundingBox?.center,
            aligned = CompositionTarget.isAligned(subject, rect),
            label = "Place subject here",
            alignedLabel = "Perfect — ${stylePrefs.style.displayName.lowercase()}!",
        )
    } else null

    // AI-suggested target persists on the preview (independent of the advice
    // sheet) so the user can move the camera to line up with it. Cleared via the
    // dismiss control or when a new request starts.
    var aiTarget by remember { mutableStateOf<com.frame.app.domain.model.NormalizedRect?>(null) }

    // When a successful analysis arrives, adopt its suggested target.
    LaunchedEffect(sceneAdviceState) {
        val success = (sceneAdviceState as? SceneAdviceUiState.Ready)?.result
                as? com.frame.app.domain.scene.SceneAnalysisResult.Success
        success?.advice?.suggestedTarget?.let {
            aiTarget = com.frame.app.domain.model.NormalizedRect(it.left, it.top, it.right, it.bottom)
        }
    }

    // Is the live subject lined up with the AI target? Drives the "nailed it"
    // confirmation and greens the AI zone.
    val aiAligned = aiTarget?.let { t ->
        subject.hasSubject && CompositionTarget.isAligned(subject, t)
    } ?: false



    // Overlay pipeline: renderer builds a spec from current state + settings.
    val overlayRenderer = remember { DefaultOverlayRenderer() }
    val aiTargetSpec = aiTarget?.let { rect ->
        TargetSpec(
            targetRect = rect,
            subjectCenter = subject.boundingBox?.center,
            aligned = aiAligned,
        )
    }
    val overlaySpec = overlayRenderer.buildSpec(
        subject = subject,                       // live ML Kit detection (Phase 3)
        tilt = tilt,
        composition = composition,               // live composition result (Phase 4)
        settings = overlayPrefs.settings,
        target = target,
        aiTarget = aiTargetSpec,
    )

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

    // Fire a one-shot confirmation the moment the subject lines up with the AI
    // target (declared here so `feedback` is in scope).
    var aiWasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(aiAligned) {
        if (aiAligned && !aiWasAligned) feedback = "Matched the AI shot — take it!"
        aiWasAligned = aiAligned
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

        // Composition overlay: thirds grid, crosshair, horizon (and, in later
        // phases, subject box + reference outline) — all from the OverlaySpec.
        CompositionOverlay(spec = overlaySpec)

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

        // Top bar: two rows so nothing overlaps regardless of style-name width.
        //  Row 1 (centered): style pill.
        //  Row 2: mode/tune on the left, flash/torch/switch on the right.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                CameraModeControls(
                    mode = overlayPrefs.mode,
                    onToggleMode = { overlayViewModel.toggleMode() },
                    onOpenSettings = { showSettingsSheet = true },
                )
                CameraTopControls(
                    flashMode = cameraState.flashMode,
                    torchEnabled = cameraState.torchEnabled,
                    hasFlashUnit = cameraState.hasFlashUnit,
                    lens = cameraState.lens,
                    analysisEnabled = analysisEnabled,
                    onCycleFlash = { controller.setFlashMode(cameraState.flashMode.next()) },
                    onToggleTorch = { controller.setTorch(!cameraState.torchEnabled) },
                    onSwitchLens = {
                        val target = if (cameraState.lens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
                        controller.setLens(target)
                    },
                    onToggleAnalysis = { analysisEnabled = !analysisEnabled },
                )
            }
            StylePill(
                styleName = stylePrefs.style.displayName,
                onClick = onOpenStylePicker,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        // Zoom readout, shown only when zoomed past the minimum. Sits clear of
        // the two-row top bar.
        if (cameraState.zoomRatio > cameraState.minZoomRatio + 0.05f) {
            ZoomIndicator(
                zoomRatio = cameraState.zoomRatio,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 120.dp),
            )
        }

        // Live composition feedback (Guided mode only; advisory). Sits above the
        // exposure slider / shutter so it reads without crowding the controls.
        if (overlayPrefs.mode == CameraMode.GUIDED) {
            CompositionFeedback(
                result = composition,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 180.dp),
            )
        }

        // AI advice button (Guided mode), bottom-right above the shutter row.
        // When an AI target is active, this becomes a "clear" control instead.
        if (overlayPrefs.mode == CameraMode.GUIDED) {
            AiAdviceButton(
                active = aiTarget != null,
                onClick = {
                    if (aiTarget != null) {
                        aiTarget = null
                    } else {
                        sceneAdviceViewModel.requestAdvice(
                            SceneContext(
                                styleName = stylePrefs.style.displayName,
                                styleTagline = stylePrefs.style.tagline,
                                ruleSummary = composition.toRuleSummary(),
                                styleBrief = stylePrefs.style.toBrief(),
                            )
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 44.dp),
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
        // Transient confirmation ("Photo saved", "Matched the AI shot") — centered
        // on screen so it never collides with the busy top HUD.
        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CaptureFeedbackPill(text = feedback.orEmpty())
        }

        cameraState.error?.let { message ->
            CameraErrorNotice(message = message)
        }
    }

    if (showSettingsSheet) {
        OverlaySettingsSheet(
            settings = overlayPrefs.settings,
            onSettingsChange = { transform -> overlayViewModel.updateSettings(transform) },
            onDismiss = { showSettingsSheet = false },
        )
    }

    // AI advice result sheet (loading / result / offline / error).
    SceneAdviceSheet(
        state = sceneAdviceState,
        onDismiss = { sceneAdviceViewModel.dismiss() },
    )
}

/** Portrait preview aspect ratio (width / height), ~9:16, used for composition
 *  evaluation. The exact value isn't critical for the current rules. */
private const val PREVIEW_ASPECT_RATIO = 9f / 16f

/** Bridge the controller's callback-based frame capture to a suspend function. */
private suspend fun suspendCaptureFrame(controller: CameraXController): ByteArray? =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        controller.captureForAnalysis { bytes ->
            if (cont.isActive) cont.resumeWith(Result.success(bytes))
        }
    }

/** Short, model-friendly summary of the current rule-engine read for context. */
private fun com.frame.app.domain.model.CompositionResult.toRuleSummary(): String {
    val scorePart = score?.let { "score $it" } ?: "no subject yet"
    val topIssues = aspects
        .filter { it.verdict == com.frame.app.domain.model.AspectVerdict.NEEDS_ADJUSTMENT }
        .mapNotNull { it.detail }
        .take(2)
        .joinToString("; ")
    return buildString {
        append("rating ").append(overall.name).append(", ").append(scorePart)
        if (topIssues.isNotEmpty()) append("; issues: ").append(topIssues)
    }
}

/** Sparkle button that requests on-demand AI advice; becomes a "clear" control
 *  while an AI target is active on screen. */
@Composable
private fun AiAdviceButton(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (active) Icons.Filled.Close else Icons.Filled.AutoAwesome,
            contentDescription = if (active) "Clear AI target" else "Get AI advice",
            tint = com.frame.app.ui.theme.FrameYellow,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = if (active) "Clear AI" else "AI advice",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 6.dp),
        )
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
