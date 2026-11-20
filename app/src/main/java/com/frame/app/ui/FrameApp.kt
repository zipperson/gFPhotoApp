package com.frame.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frame.app.ui.camera.CameraScreen
import com.frame.app.ui.gallery.GalleryScreen
import com.frame.app.ui.permission.CameraPermissionGate
import com.frame.app.ui.style.StyleSelectionScreen
import com.frame.app.viewmodel.GalleryViewModel
import com.frame.app.viewmodel.GalleryViewModelFactory
import com.frame.app.viewmodel.StyleViewModel
import com.frame.app.viewmodel.StyleViewModelFactory

/**
 * Root composable for FRAME.
 *
 * Flow: CAMERA permission gate -> first-run style picker (if the user hasn't
 * chosen a style yet) -> live camera. From the camera the user can reopen the
 * style picker or open the photo gallery (via the last-photo thumbnail).
 */
@Composable
fun FrameApp() {
    val context = LocalContext.current
    val styleViewModel: StyleViewModel = viewModel(
        factory = StyleViewModelFactory(context.applicationContext),
    )
    val styleState by styleViewModel.state.collectAsStateWithLifecycle()

    val galleryViewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModelFactory(context.applicationContext),
    )
    val galleryState by galleryViewModel.state.collectAsStateWithLifecycle()

    // Load existing photos once so the camera thumbnail is populated at launch.
    LaunchedEffect(Unit) { galleryViewModel.refresh() }

    // Lets the camera reopen the picker after a style has already been chosen.
    var reopenStyle by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }

    CameraPermissionGate {
        when {
            !styleState.hasChosen -> StyleSelectionScreen(
                current = null,
                onStyleChosen = { styleViewModel.setStyle(it) },
            )

            reopenStyle -> StyleSelectionScreen(
                current = styleState.style,
                onStyleChosen = {
                    styleViewModel.setStyle(it)
                    reopenStyle = false
                },
                showBack = true,
                onBack = { reopenStyle = false },
            )

            showGallery -> GalleryScreen(
                photos = galleryState.photos,
                isLoaded = galleryState.isLoaded,
                onBack = { showGallery = false },
            )

            else -> CameraScreen(
                onOpenStylePicker = { reopenStyle = true },
                latestPhotoUri = galleryState.latest?.uriString,
                onOpenGallery = {
                    galleryViewModel.refresh()
                    showGallery = true
                },
                onPhotoCaptured = { galleryViewModel.refresh() },
            )
        }
    }
}
