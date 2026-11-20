package com.frame.app.ui.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Large centered countdown numeral. Pulses (pops in, settles) on every tick.
 * Deliberate exception to the 4-size type scale: it has to be readable from
 * arm's length when the user is posing away from the phone.
 */
@Composable
fun CountdownOverlay(secondsLeft: Int, modifier: Modifier = Modifier) {
    val pulse = remember { Animatable(1.3f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(secondsLeft) {
        pulse.snapTo(1.3f)
        fade.snapTo(1f)
        pulse.animateTo(1f, tween(durationMillis = 300))
        fade.animateTo(0.85f, tween(durationMillis = 500))
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = secondsLeft.toString(),
            style = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier
                .scale(pulse.value)
                .alpha(fade.value)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
