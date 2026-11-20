package com.frame.app.ui.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.frame.app.ui.components.Glass
import com.frame.app.ui.theme.Sizes

/**
 * Rounded thumbnail of the most recent photo (stock-camera style). Shows a
 * gallery icon placeholder when there are no photos yet. Tapping opens the
 * gallery; the thumbnail updating is the cue that a capture succeeded.
 */
@Composable
fun LastPhotoThumbnail(
    photoUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)

    // Pop when a new photo lands (not on first composition), confirming the save.
    val pop = remember { Animatable(1f) }
    var lastUri by remember { mutableStateOf(photoUri) }
    LaunchedEffect(photoUri) {
        if (photoUri != null && photoUri != lastUri) {
            pop.snapTo(1.25f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        lastUri = photoUri
    }

    Box(
        modifier = modifier
            .scale(pop.value)
            .size(Sizes.thumbnail)
            .clip(shape)
            .background(Glass.Surface)
            .border(BorderStroke(2.dp, Color.White), shape)
            .clickable(onClickLabel = "Open gallery", role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Last photo. Open gallery",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(Sizes.thumbnail),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = "Open gallery",
                tint = Color.White,
            )
        }
    }
}
