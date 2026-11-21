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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.frame.app.domain.model.SelfTimer
import com.frame.app.ui.camera.gesture.afterSwipe
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frame.app.domain.composition.CompositionTarget
import com.frame.app.domain.composition.DefaultCompositionEngine
import com.frame.app.domain.composition.toBrief
import com.frame.app.domain.composition.toThresholds
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState
import com.frame.app.domain.scene.SceneContext
import com.frame.app.domain.service.TargetSpec
import com.frame.app.platform.camera.CameraLens
import com.frame.app.platform.camera.CameraXController
import com.frame.app.platform.camera.CaptureResult
import com.frame.app.platform.camera.next
import com.frame.app.platform.overlay.DefaultOverlayRenderer
import com.frame.app.platform.pose.MlKitPoseDetector
import com.frame.app.platform.sensor.DeviceLevelSensor
import com.frame.app.ui.camera.hud.CameraBottomBar
import com.frame.app.ui.camera.hud.CameraTopBar
import com.frame.app.ui.camera.overlay.CompositionOverlay
import com.frame.app.ui.components.GlassPill
import com.frame.app.ui.theme.Sizes
import com.frame.app.ui.theme.Spacing
import com.frame.app.domain.composition.GroupCompositionEngine
import com.frame.app.domain.model.CompositionRating
import com.frame.app.domain.service.CompositionEngine
import com.frame.app.platform.camera.ModeSwitchingAnalyzer
import com.frame.app.platform.face.MlKitFaceGroupDetector
import com.frame.app.domain.model.CameraMode
import com.frame.app.platform.filter.applyPhotoFilter
import com.frame.app.ui.camera.hud.FilterStrip
import com.frame.app.viewmodel.FilterViewModel
import com.frame.app.viewmodel.OverlayViewModel
import com.frame.app.viewmodel.OverlayViewModelFactory
import com.frame.app.viewmodel.SceneAdviceViewModel
import com.frame.app.viewmodel.SceneAdviceViewModelFactory
import com.frame.app.viewmodel.StyleViewModel
import com.frame.app.viewmodel.StyleViewModelFactory

/**
 * The live camera screen.
 *
 * A full-screen [PreviewView] (hosted via [AndroidView]) with the guidance
 * overlay and gesture layer on top, then the HUD: a slim top bar and a
 * thumb-zone bottom stack (status, mode strip + AI, thumbnail/shutter/flip).
 */
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    onOpenStylePicker: () -> Unit = {},
    /** URI of the most recent FRAME photo, shown as the shutter-side thumbnail. */
    latestPhotoUri: String? = null,
    onOpenGallery: () -> Unit = {},
    /** Called after a photo is saved so the gallery/thumbnail can refresh. */
    onPhotoCaptured: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ML Kit pose detection feeding the ImageAnalysis use case. Its analyzer
    // runs per frame; detected subjects flow out via `results` into the overlay.
    val poseDetector = remember { MlKitPoseDetector() }
    // Group mode swaps in a face detector. Only one of the two sees each frame.
    val faceDetector = remember { MlKitFaceGroupDetector() }
    val frameAnalyzer = remember {
        ModeSwitchingAnalyzer(single = poseDetector.analyzer, group = faceDetector.analyzer)
    }

    // Overlay mode + settings (persisted via DataStore in OverlayViewModel).
    val overlayViewModel: OverlayViewModel = viewModel(
        factory = OverlayViewModelFactory(context.applicationContext),
    )
    val overlayPrefs by overlayViewModel.state.collectAsStateWithLifecycle()

    // Group is a camera mode (GUIDED / GROUP / MINIMAL), not a separate toggle.
    val groupMode = overlayPrefs.mode == CameraMode.GROUP
    LaunchedEffect(groupMode) {
        faceDetector.reset()
        frameAnalyzer.groupMode = groupMode
    }

    val controller = remember {
        CameraXController(
            appContext = context.applicationContext,
            frameAnalyzer = frameAnalyzer,
        )
    }
    val cameraState by controller.state.collectAsStateWithLifecycle()
    val rawSubject by poseDetector.results.collectAsStateWithLifecycle(
        initialValue = SubjectFrame.empty(0L),
    )

    // Composition analysis on/off. Off = stop ML Kit entirely (no detection,
    // no composition) to cut heat/battery; preview + capture keep working.
    var analysisEnabled by remember { mutableStateOf(true) }

    val groupSubject by faceDetector.results.collectAsStateWithLifecycle(
        initialValue = SubjectFrame.empty(0L),
    )

    // When analysis is off, present no subject so overlays/feedback clear.
    val subject = when {
        !analysisEnabled -> SubjectFrame.empty(0L)
        groupMode -> groupSubject
        else -> rawSubject
    }
    val groupCount = subject.group?.count ?: 0

    DisposableEffect(analysisEnabled) {
        if (analysisEnabled) {
            poseDetector.start()
            faceDetector.start()
        } else {
            poseDetector.stop()
            faceDetector.stop()
        }
        onDispose { /* lifecycle stop handled below */ }
    }

    DisposableEffect(Unit) {
        onDispose {
            poseDetector.close()
            faceDetector.close()
            controller.shutdown()
        }
    }

    // Keep the detectors' lens in sync so front-camera mirroring stays correct.
    LaunchedEffect(cameraState.lens) {
        poseDetector.setLens(cameraState.lens)
        faceDetector.setLens(cameraState.lens)
    }

    // Device level/tilt sensor.
    val levelSensor = remember { DeviceLevelSensor(context.applicationContext) }
    val tilt by levelSensor.tilt.collectAsStateWithLifecycle(initialValue = TiltState.UNAVAILABLE)

    var showMoreSheet by remember { mutableStateOf(false) }
    var focusTap by remember { mutableStateOf<FocusTap?>(null) }

    // Colour look: applied to the live preview and baked into saved photos.
    // Held in a ViewModel so it survives a trip to the gallery.
    val filterViewModel: FilterViewModel = viewModel()
    val filter by filterViewModel.filter.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    // Selected photography style; drives the composition engine.
    val styleViewModel: StyleViewModel = viewModel(
        factory = StyleViewModelFactory(context.applicationContext),
    )
    val stylePrefs by styleViewModel.state.collectAsStateWithLifecycle()

    // AI scene advice: on-demand OpenAI analysis of a captured frame.
    val sceneAdviceViewModel: SceneAdviceViewModel = viewModel(
        factory = SceneAdviceViewModelFactory(
            appContext = context.applicationContext,
            captureFrame = { suspendCaptureFrame(controller) },
        ),
    )
    val sceneAdviceState by sceneAdviceViewModel.state.collectAsStateWithLifecycle()
    val activeAdvice by sceneAdviceViewModel.activeAdvice.collectAsStateWithLifecycle()

    // The ViewModel outlives this screen (e.g. across a gallery visit) but the
    // controller doesn't, so always point frame capture at the *current* one.
    DisposableEffect(controller) {
        sceneAdviceViewModel.setCaptureSource { suspendCaptureFrame(controller) }
        onDispose { sceneAdviceViewModel.setCaptureSource { null } }
    }

    // Composition engine, tuned to the selected style. Recreated when the style
    // changes so advice/scoring tracks the chosen look. Pure + cheap.
    val compositionEngine: CompositionEngine = remember(stylePrefs.style, groupMode) {
        if (groupMode) GroupCompositionEngine()
        else DefaultCompositionEngine(stylePrefs.style.toThresholds())
    }
    // Evaluate the current (smoothed) subject + tilt. Portrait preview ~ 9:16.
    val composition = compositionEngine.evaluate(
        subject = subject,
        tilt = tilt,
        frameAspectRatio = PREVIEW_ASPECT_RATIO,
    )

    // Target placement guidance: where the subject should sit for this style.
    // (Groups have no single ideal zone, so group mode shows no target.)
    val target = if (subject.hasSubject && !groupMode) {
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

    // AI-suggested target, derived from the ViewModel's active advice so it
    // survives navigating to the gallery and back; cleared only via the sheet.
    val aiTarget = activeAdvice?.suggestedTarget?.let {
        com.frame.app.domain.model.NormalizedRect(it.left, it.top, it.right, it.bottom)
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
            // Arrow anchors at screen center in the overlay, so subjectCenter is
            // unused for the AI target (null keeps it persistent when analysis
            // is hidden and there's no detected subject).
            subjectCenter = null,
            aligned = aiAligned,
        )
    }
    val overlaySpec = overlayRenderer.buildSpec(
        subject = subject,
        tilt = tilt,
        composition = composition,
        settings = overlayPrefs.settings,
        target = target,
        aiTarget = aiTargetSpec,
    )

    // Transient capture state, local to the screen.
    var isCapturing by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    // Self-timer (session-only). `countdown` is the seconds left while running.
    var timer by remember { mutableStateOf(SelfTimer.OFF) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val performCapture: () -> Unit = {
        isCapturing = true
        showFlash = true
        controller.capturePhoto(filter) { result ->
            isCapturing = false
            when (result) {
                // Like the stock camera: success is signalled by the
                // thumbnail updating, not a text pill.
                is CaptureResult.Success -> {
                    onPhotoCaptured()
                    if (result.filterFailed) feedback = "Saved without the filter"
                }
                is CaptureResult.Failure -> feedback = result.message
            }
        }
    }

    // Shared by the shutter button and the volume keys: cancels a running
    // countdown, starts one if a timer is set, otherwise captures immediately.
    val onShutterPressed: () -> Unit = {
        if (countdown != null) {
            countdownJob?.cancel()
            countdownJob = null
            countdown = null
        } else if (cameraState.isReady && !isCapturing) {
            if (!timer.isOn) {
                performCapture()
            } else {
                countdownJob = scope.launch {
                    try {
                        for (s in timer.seconds downTo 1) {
                            countdown = s
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            delay(1000)
                        }
                    } finally {
                        // Runs on cancel too; the capture below is skipped then
                        // because cancellation propagates out of the try.
                        countdown = null
                    }
                    performCapture()
                }
            }
        }
    }

    // Volume keys act as the shutter while this screen is showing.
    val shutterKeys = LocalShutterKeys.current
    val currentOnShutterPressed by rememberUpdatedState(onShutterPressed)
    DisposableEffect(shutterKeys) {
        val unregister = shutterKeys.register { currentOnShutterPressed() }
        onDispose { unregister() }
    }

    // "Shot ready": the subject sits in whichever target zone is on screen.
    // Greens the shutter ring and ticks once on the way in.
    val shotReady = overlaySpec.target?.aligned == true ||
        overlaySpec.aiTarget?.aligned == true ||
        // Group mode has no target zone; a good group read is "ready".
        (groupMode && groupCount > 0 && composition.overall == CompositionRating.GOOD)
    var wasShotReady by remember { mutableStateOf(false) }
    LaunchedEffect(shotReady) {
        if (shotReady && !wasShotReady) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasShotReady = shotReady
    }

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
            update = { view -> view.applyPhotoFilter(filter) },
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
            onTapAt = { pos -> focusTap = FocusTap(pos, System.nanoTime()) },
            onSwipe = { direction ->
                val newMode = overlayPrefs.mode.afterSwipe(direction)
                if (newMode != overlayPrefs.mode) {
                    overlayViewModel.setMode(newMode)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
        )

        // Guidance overlay: grid, level, subject brackets, targets, arrows.
        CompositionOverlay(spec = overlaySpec)

        // Exposure slider that appears beside the focus ring after a tap.
        if (cameraState.supportsExposure) {
            ExposureOnFocus(
                tap = focusTap,
                index = cameraState.exposureIndex,
                minIndex = cameraState.minExposureIndex,
                maxIndex = cameraState.maxExposureIndex,
                onIndexChange = { controller.setExposureIndex(it) },
            )
        }

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

        // --- HUD ---------------------------------------------------------

        // Top: flash | style | more.
        CameraTopBar(
            flashMode = cameraState.flashMode,
            hasFlashUnit = cameraState.hasFlashUnit,
            currentStyle = stylePrefs.style,
            onCycleFlash = { controller.setFlashMode(cameraState.flashMode.next()) },
            onSelectStyle = { styleViewModel.setStyle(it) },
            onOpenStylePicker = onOpenStylePicker,
            onOpenMore = { showMoreSheet = true },
            filterActive = filter.isActive,
            onToggleFilters = { showFilters = !showFilters },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Look chips, just under the zoom readout position.
        AnimatedVisibility(
            visible = showFilters,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                // Directly under the top bar (48dp buttons + 8dp padding each side).
                .padding(top = Spacing.xxl + Spacing.sm),
        ) {
            FilterStrip(
                selected = filter,
                onSelect = { filterViewModel.setFilter(it) },
            )
        }

        // Zoom readout, shown only when zoomed past the minimum. Sits just
        // below the top bar.
        if (cameraState.zoomRatio > cameraState.minZoomRatio + 0.05f) {
            ZoomIndicator(
                zoomRatio = cameraState.zoomRatio,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    // Drops below the filter chips while they're open.
                    .padding(
                        top = if (showFilters) {
                            Spacing.xxl + Spacing.sm + Sizes.touchTarget + Spacing.xs
                        } else {
                            Spacing.xxl + Spacing.lg
                        },
                    ),
            )
        }

        // Bottom thumb-zone stack: status, mode strip + AI, thumbnail/shutter/flip.
        CameraBottomBar(
            composition = composition,
            mode = overlayPrefs.mode,
            onModeChange = { overlayViewModel.setMode(it) },
            analysisEnabled = analysisEnabled,
            onResumeAnalysis = { analysisEnabled = true },
            adviceActive = activeAdvice != null,
            onAiClick = {
                if (activeAdvice != null) {
                    sceneAdviceViewModel.reopen()
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
            latestPhotoUri = latestPhotoUri,
            onOpenGallery = onOpenGallery,
            shutterEnabled = cameraState.isReady && !isCapturing,
            onCapture = onShutterPressed,
            onFlipCamera = {
                val next = if (cameraState.lens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
                controller.setLens(next)
            },
            timer = timer,
            onCycleTimer = { timer = timer.next() },
            countingDown = countdown != null,
            shotReady = shotReady,
            groupCount = groupCount,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // Big centered countdown numeral while the self-timer runs.
        countdown?.let { CountdownOverlay(secondsLeft = it) }

        // Transient confirmation / error, centered so it never collides with the HUD.
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

    if (showMoreSheet) {
        CameraMoreSheet(
            settings = overlayPrefs.settings,
            onSettingsChange = { transform -> overlayViewModel.updateSettings(transform) },
            analysisEnabled = analysisEnabled,
            onAnalysisChange = { analysisEnabled = it },
            hasFlashUnit = cameraState.hasFlashUnit,
            torchEnabled = cameraState.torchEnabled,
            onTorchChange = { controller.setTorch(it) },
            onDismiss = { showMoreSheet = false },
        )
    }

    // AI advice result sheet (loading / result / offline / error).
    SceneAdviceSheet(
        state = sceneAdviceState,
        onDismiss = { sceneAdviceViewModel.dismiss() },
        onClear = { sceneAdviceViewModel.clearAdvice() },
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

/** Short centered message over the preview (errors, one-shot confirmations). */
@Composable
private fun CaptureFeedbackPill(text: String, modifier: Modifier = Modifier) {
    GlassPill(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
