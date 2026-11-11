package com.frame.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** FRAME's signature guide color. */
val FrameYellow = Color(0xFFFFD400)
val FrameWhite = Color(0xFFFFFFFF)
val FrameBlack = Color(0xFF000000)

/**
 * FRAME is a camera-first app, so the theme is intentionally dark and minimal —
 * the preview should dominate and chrome should recede. We use a single dark
 * scheme regardless of system setting to keep the photography UI consistent.
 */
private val FrameColorScheme = darkColorScheme(
    primary = FrameYellow,
    onPrimary = FrameBlack,
    secondary = FrameWhite,
    background = FrameBlack,
    onBackground = FrameWhite,
    surface = FrameBlack,
    onSurface = FrameWhite,
)

@Composable
fun FrameTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FrameColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
