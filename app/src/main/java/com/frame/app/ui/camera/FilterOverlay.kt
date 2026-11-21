package com.frame.app.ui.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import com.frame.app.domain.model.PhotoFilter
import com.frame.app.platform.filter.NoiseTile
import kotlin.math.hypot

/** Fraction of the corner distance that stays clear. Matches the saved-photo vignette. */
private const val VIGNETTE_CLEAR_RADIUS = 0.55f

/**
 * Live-preview half of a look's vignette and grain, drawn over the camera
 * preview (the colour matrix is applied to the preview view itself). Uses the
 * same clear radius, strengths and noise tile as [com.frame.app.platform.filter.PhotoFilterProcessor],
 * so the preview matches the saved photo. Draws nothing for Original.
 */
@Composable
fun FilterOverlay(filter: PhotoFilter, modifier: Modifier = Modifier) {
    if (!filter.isActive) return

    val noise = remember {
        Bitmap.createBitmap(
            NoiseTile.pixels(), NoiseTile.SIZE, NoiseTile.SIZE, Bitmap.Config.ARGB_8888,
        ).asImageBitmap()
    }
    val grainBrush = remember(noise) {
        ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (filter.vignette > 0f) {
            drawRect(
                brush = Brush.radialGradient(
                    VIGNETTE_CLEAR_RADIUS to Color.Transparent,
                    1f to Color.Black.copy(alpha = filter.vignette),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = hypot(size.width / 2f, size.height / 2f),
                ),
            )
        }
        if (filter.grain > 0f) {
            drawRect(brush = grainBrush, alpha = filter.grain, blendMode = BlendMode.Overlay)
        }
    }
}
