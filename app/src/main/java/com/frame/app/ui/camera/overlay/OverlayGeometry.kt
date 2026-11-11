package com.frame.app.ui.camera.overlay

/**
 * Pure mapping between normalized overlay coordinates (0..1 of the *visible*
 * preview) and pixel coordinates on the Compose Canvas. Kept Android-free so the
 * mapping — the part that later aligns subject boxes (Phase 3) and reference
 * outlines (Phase 5) to the live image — is unit-testable.
 *
 * Phase 1 guides (thirds, crosshair) are defined against the visible canvas, so
 * the mapping is a straightforward scale. The structure anticipates Phase 3,
 * where detector output in *source image* space must be mapped through the
 * preview's FILL_CENTER crop; [forImageInView] computes that transform.
 */
data class OverlayMapping(
    val canvasWidth: Float,
    val canvasHeight: Float,
    /** Offset applied to source-space content before scaling (crop, in px). */
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    /** Scale from source-normalized to canvas px (1.0 = fills canvas exactly). */
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
) {
    /** Map a normalized x (0..1 of the canvas) to a canvas pixel x. */
    fun x(normalizedX: Float): Float = normalizedX * canvasWidth * scaleX + offsetX

    /** Map a normalized y (0..1 of the canvas) to a canvas pixel y. */
    fun y(normalizedY: Float): Float = normalizedY * canvasHeight * scaleY + offsetY

    companion object {
        /** Simple 1:1 canvas mapping (used for Phase 1 guides). */
        fun forCanvas(width: Float, height: Float): OverlayMapping =
            OverlayMapping(canvasWidth = width, canvasHeight = height)

        /**
         * Mapping for content expressed in the *source image's* normalized space
         * displayed with a center-crop (FILL_CENTER) into the canvas. Scales the
         * source to cover the canvas and centers the overflow, matching
         * PreviewView.ScaleType.FILL_CENTER so overlays line up with the pixels
         * the user actually sees.
         *
         * @param canvasWidth/Height   visible canvas size (px)
         * @param sourceWidth/Height   analyzed image size (px); aspect matters
         */
        fun forImageInView(
            canvasWidth: Float,
            canvasHeight: Float,
            sourceWidth: Float,
            sourceHeight: Float,
        ): OverlayMapping {
            if (sourceWidth <= 0f || sourceHeight <= 0f) {
                return forCanvas(canvasWidth, canvasHeight)
            }
            // Cover scale: the larger ratio so the image fully covers the canvas.
            val scale = maxOf(canvasWidth / sourceWidth, canvasHeight / sourceHeight)
            val scaledW = sourceWidth * scale
            val scaledH = sourceHeight * scale
            val offX = (canvasWidth - scaledW) / 2f
            val offY = (canvasHeight - scaledH) / 2f
            return OverlayMapping(
                canvasWidth = sourceWidth,
                canvasHeight = sourceHeight,
                offsetX = offX,
                offsetY = offY,
                scaleX = scale,
                scaleY = scale,
            )
        }
    }
}
