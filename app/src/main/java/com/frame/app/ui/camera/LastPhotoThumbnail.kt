package com.frame.app.ui.camera

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

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
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.4f))
            .border(BorderStroke(2.dp, Color.White), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Open gallery",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp),
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
