package com.frame.app.ui.camera.hud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.frame.app.domain.model.CameraMode
import com.frame.app.domain.model.CompositionResult
import com.frame.app.domain.model.SelfTimer
import com.frame.app.ui.camera.LastPhotoThumbnail
import com.frame.app.ui.camera.ShutterButton
import com.frame.app.ui.components.GlassCircleButton
import androidx.compose.ui.graphics.Color
import com.frame.app.ui.theme.FrameGreen
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Spacing

/**
 * The thumb-zone stack, bottom-up:
 *   1. thumbnail | shutter | flip camera
 *   2. GUIDED / MINIMAL mode strip, with the AI button at the right
 *   3. live status line (Guided only)
 *
 * Everything the user taps while shooting is within thumb reach.
 */
@Composable
fun CameraBottomBar(
    composition: CompositionResult,
    mode: CameraMode,
    onModeChange: (CameraMode) -> Unit,
    analysisEnabled: Boolean,
    onResumeAnalysis: () -> Unit,
    adviceActive: Boolean,
    onAiClick: () -> Unit,
    latestPhotoUri: String?,
    onOpenGallery: () -> Unit,
    shutterEnabled: Boolean,
    onCapture: () -> Unit,
    onFlipCamera: () -> Unit,
    timer: SelfTimer,
    onCycleTimer: () -> Unit,
    countingDown: Boolean,
    shotReady: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // 3. Status line (Guided only).
        if (mode == CameraMode.GUIDED) {
            if (analysisEnabled) {
                StatusPill(result = composition)
            } else {
                GuidancePausedPill(onResume = onResumeAnalysis)
            }
        }

        // 2. Mode strip, with the AI button at the end.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // Timer sits opposite the AI button; locked while a countdown runs.
            if (!countingDown) {
                TimerButton(
                    timer = timer,
                    onClick = onCycleTimer,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
            ModeStrip(mode = mode, onSelect = onModeChange)
            // Minimal hides the AI button, except while advice is active, so an
            // on-screen AI target can always be reopened or cleared.
            if (mode == CameraMode.GUIDED || adviceActive) {
                AiButton(
                    active = adviceActive,
                    onClick = onAiClick,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }

        // 1. Thumbnail | shutter | flip.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            LastPhotoThumbnail(
                photoUri = latestPhotoUri,
                onClick = onOpenGallery,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            ShutterButton(
                enabled = shutterEnabled || countingDown,
                onCapture = onCapture,
                countingDown = countingDown,
                ringColor = if (shotReady) FrameGreen else Color.White,
            )
            GlassCircleButton(
                icon = Icons.Filled.Cameraswitch,
                contentDescription = "Switch camera",
                onClick = onFlipCamera,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/**
 * The single AI control. Idle: glass circle with a yellow sparkle. When advice
 * is waiting: solid yellow with a black sparkle (the shape is the same, the fill
 * changes). Tapping always opens the advice or requests it; clearing lives
 * inside the advice sheet.
 */
@Composable
private fun AiButton(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCircleButton(
        icon = Icons.Filled.AutoAwesome,
        contentDescription = if (active) "AI tip ready" else "Get AI advice",
        onClickLabel = if (active) "Open AI advice" else "Ask the AI photo coach",
        onClick = onClick,
        modifier = modifier,
        active = active,
        tint = if (active) null else FrameYellow,
    )
}
