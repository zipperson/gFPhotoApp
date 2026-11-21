package com.frame.app.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign
import com.frame.app.ui.theme.Spacing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.frame.app.domain.gallery.PhotoItem
import com.frame.app.ui.theme.FrameYellow

/**
 * In-app gallery of FRAME photos: a 3-column grid; tapping a photo opens a
 * full-screen, swipeable viewer starting at that photo. Back from the viewer
 * returns to the grid; back from the grid returns to the camera.
 */
@Composable
fun GalleryScreen(
    photos: List<PhotoItem>,
    isLoaded: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Index of the photo open in the full-screen viewer, or null for the grid.
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler {
        if (viewerIndex != null) viewerIndex = null else onBack()
    }

    val index = viewerIndex
    if (index != null && photos.isNotEmpty()) {
        PhotoViewer(
            photos = photos,
            startIndex = index.coerceIn(0, photos.lastIndex),
            onClose = { viewerIndex = null },
            modifier = modifier,
        )
    } else {
        GalleryGrid(
            photos = photos,
            isLoaded = isLoaded,
            onOpen = { viewerIndex = it },
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun GalleryGrid(
    photos: List<PhotoItem>,
    isLoaded: Boolean,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to camera",
                tint = Color.White,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBack)
                    .padding(10.dp),
            )
            Text(
                text = "Photos",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        if (isLoaded && photos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.xl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = FrameYellow,
                    modifier = Modifier.size(Spacing.xxl),
                )
                Text(
                    text = "No photos yet",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = Spacing.md),
                )
                Text(
                    text = "Photos you take with Siyot show up here.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FrameYellow,
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.padding(top = Spacing.lg),
                ) {
                    Text("Open camera", style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                itemsIndexed(photos, key = { _, p -> p.id }) { i, photo ->
                    AsyncImage(
                        model = photo.uriString,
                        contentDescription = "Photo ${i + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(1.dp)
                            .aspectRatio(1f)
                            .background(Color.DarkGray)
                            .clickable { onOpen(i) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoViewer(
    photos: List<PhotoItem>,
    startIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { photos.size }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            AsyncImage(
                model = photos[page].uriString,
                contentDescription = "Photo ${page + 1} of ${photos.size}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Close viewer",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .size(44.dp)
                .clickable(onClick = onClose)
                .padding(10.dp),
        )

        Text(
            text = "${pagerState.currentPage + 1} / ${photos.size}",
            color = FrameYellow,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        )
    }
}
