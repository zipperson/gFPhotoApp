package com.frame.app.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Sizes
import com.frame.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** A tap-to-focus event: where (in pixels within the overlay) and a unique id. */
data class FocusTap(val position: Offset, val id: Long)

/** Maps a top-to-bottom drag fraction (0 = top) to an EV index; top is brightest. */
internal fun exposureIndexForFraction(fraction: Float, minIndex: Int, maxIndex: Int): Int {
    val f = (1f - fraction).coerceIn(0f, 1f)
    return (minIndex + f * (maxIndex - minIndex)).roundToInt().coerceIn(minIndex, maxIndex)
}

/** Inverse of [exposureIndexForFraction]: top-to-bottom fraction for an EV index. */
internal fun fractionForExposureIndex(index: Int, minIndex: Int, maxIndex: Int): Float {
    if (maxIndex <= minIndex) return 0.5f
    return 1f - (index - minIndex).toFloat() / (maxIndex - minIndex)
}

private val SliderHeight = 160.dp
private val SliderWidth = Sizes.touchTarget
private val ThumbSize = 32.dp

/**
 * Vertical exposure slider that appears beside the focus point after a tap and
 * fades out a few seconds after the last interaction, like the stock camera.
 * Sits to the right of the focus ring (left if there is no room). Only the
 * slider itself takes touches; the rest of this layer is transparent.
 */
@Composable
fun ExposureOnFocus(
    tap: FocusTap?,
    index: Int,
    minIndex: Int,
    maxIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    var interactions by remember { mutableIntStateOf(0) }
    val currentOnIndexChange by rememberUpdatedState(onIndexChange)

    // Re-show on every tap; restart the hide timer on every drag.
    LaunchedEffect(tap?.id, interactions) {
        if (tap != null) {
            visible = true
            // Start fading exactly when the focus ring does.
            delay((FOCUS_RING_SETTLE_MS + FOCUS_RING_HOLD_MS).toLong())
            visible = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (tap == null) return@BoxWithConstraints
        val density = LocalDensity.current
        val ringGap = with(density) { RingGap.roundToPx() }
        val w = with(density) { SliderWidth.roundToPx() }
        val h = with(density) { SliderHeight.roundToPx() }
        val maxW = with(density) { maxWidth.roundToPx() }
        val maxH = with(density) { maxHeight.roundToPx() }

        val rightX = tap.position.x + ringGap
        val x = if (rightX + w <= maxW - with(density) { Spacing.xs.roundToPx() }) {
            rightX
        } else {
            tap.position.x - ringGap - w
        }.roundToInt()
        val y = (tap.position.y - h / 2f).roundToInt().coerceIn(0, (maxH - h).coerceAtLeast(0))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(tween(FOCUS_RING_FADE_MS)),
            modifier = Modifier.offset { IntOffset(x, y) },
        ) {
            val fraction = fractionForExposureIndex(index, minIndex, maxIndex)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(SliderWidth)
                        .height(SliderHeight)
                        .semantics {
                            contentDescription = "Exposure"
                            stateDescription = if (index > 0) "+$index" else "$index"
                            progressBarRangeInfo = ProgressBarRangeInfo(
                                current = index.toFloat(),
                                range = minIndex.toFloat()..maxIndex.toFloat(),
                                steps = (maxIndex - minIndex - 1).coerceAtLeast(0),
                            )
                            setProgress { target ->
                                currentOnIndexChange(target.roundToInt().coerceIn(minIndex, maxIndex))
                                interactions++
                                true
                            }
                        }
                        .pointerInput(minIndex, maxIndex) {
                            detectTapGestures { pos ->
                                currentOnIndexChange(
                                    exposureIndexForFraction(pos.y / size.height, minIndex, maxIndex),
                                )
                                interactions++
                            }
                        }
                        .pointerInput(minIndex, maxIndex) {
                            detectVerticalDragGestures { change, _ ->
                                change.consume()
                                currentOnIndexChange(
                                    exposureIndexForFraction(
                                        change.position.y / size.height, minIndex, maxIndex,
                                    ),
                                )
                                interactions++
                            }
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    // Value readout floats above the track without taking layout
                    // space, so the track itself stays centred on the focus point.
                    Text(
                        text = if (index > 0) "+$index" else "$index",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (index != 0) FrameYellow else Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = -Spacing.lg),
                    )
                    // Track, with the span between "no change" and the thumb in yellow.
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val inset = ThumbSize.toPx() / 2f
                        val top = inset
                        val bottom = size.height - inset
                        drawLine(
                            Color.White.copy(alpha = 0.4f), Offset(cx, top), Offset(cx, bottom), 3.dp.toPx(),
                        )
                        val zero = fractionForExposureIndex(0, minIndex, maxIndex)
                        val yZero = top + (bottom - top) * zero
                        val yThumb = top + (bottom - top) * fraction
                        drawLine(FrameYellow, Offset(cx, yZero), Offset(cx, yThumb), 3.dp.toPx())
                    }
                    // Thumb top edge: its centre travels the track, inset by half its size.
                    val thumbY = ((h - with(density) { ThumbSize.roundToPx() }) * fraction).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, thumbY) }
                            .size(ThumbSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = null,
                            tint = FrameYellow,
                            modifier = Modifier.size(Sizes.icon),
                        )
                    }
                }
            }
        }
    }
}

/** Distance from the focus point to the slider box edge. The box is 48dp wide
 *  with the track centred, so this puts the visible track ~12dp off the ring. */
private val RingGap = 32.dp
