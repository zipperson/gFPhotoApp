package com.frame.app.domain.service

import com.frame.app.domain.model.CompositionResult
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState

/**
 * Describes *what* overlays should be drawn over the camera preview, decoupled
 * from *how* they are drawn. The Compose Canvas overlay (platform/UI layer)
 * consumes an [OverlaySpec] and renders it; this keeps the "what to show"
 * decision testable and independent of rendering.
 */
interface OverlayRenderer {

    /**
     * Build the overlay spec for the current frame state.
     *
     * @param subject     latest subject detection (Phase 3+); may have no subject
     * @param tilt        current device tilt
     * @param composition latest composition result (Phase 4+)
     * @param settings    user overlay preferences (which guides, opacity)
     */
    fun buildSpec(
        subject: SubjectFrame,
        tilt: TiltState,
        composition: CompositionResult,
        settings: OverlaySettings,
    ): OverlaySpec
}

/**
 * User-controlled overlay preferences. Persisted via DataStore (Phase 1, Task 9).
 */
data class OverlaySettings(
    val showThirds: Boolean = true,
    val showCenterCrosshair: Boolean = true,
    val showHorizon: Boolean = true,
    val showSubjectBox: Boolean = true,
    val showReferenceOutline: Boolean = true,
    /** Guide opacity, 0f..1f. */
    val opacity: Float = 0.6f,
) {
    companion object {
        val MINIMAL = OverlaySettings(
            showThirds = true,
            showCenterCrosshair = false,
            showHorizon = true,
            showSubjectBox = false,
            showReferenceOutline = false,
            opacity = 0.4f,
        )

        val GUIDED = OverlaySettings(
            showThirds = true,
            showCenterCrosshair = true,
            showHorizon = true,
            showSubjectBox = true,
            showReferenceOutline = true,
            opacity = 0.7f,
        )
    }
}

/**
 * A declarative description of the overlay to draw this frame. Expressed in
 * normalized space; the renderer maps it to pixels. Elements that are not
 * applicable are simply null/empty, so Phase 1 emits only the static guides.
 */
data class OverlaySpec(
    val showThirds: Boolean,
    val showCenterCrosshair: Boolean,
    val horizon: HorizonSpec?,
    val subjectBox: com.frame.app.domain.model.NormalizedRect?,
    val referenceOutline: com.frame.app.domain.model.NormalizedRect?,
    val opacity: Float,
) {
    companion object {
        val EMPTY = OverlaySpec(
            showThirds = false,
            showCenterCrosshair = false,
            horizon = null,
            subjectBox = null,
            referenceOutline = null,
            opacity = 0.6f,
        )
    }
}

/**
 * Horizon/level indicator state.
 *
 * @property rollDegrees current roll for drawing the tilt line
 * @property isLevel     true when within the level tolerance (drives color)
 */
data class HorizonSpec(
    val rollDegrees: Float,
    val isLevel: Boolean,
)
