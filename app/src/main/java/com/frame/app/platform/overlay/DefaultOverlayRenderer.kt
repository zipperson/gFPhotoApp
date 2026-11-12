package com.frame.app.platform.overlay

import com.frame.app.domain.model.CompositionResult
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState
import com.frame.app.domain.service.HorizonSpec
import com.frame.app.domain.service.OverlayRenderer
import com.frame.app.domain.service.OverlaySettings
import com.frame.app.domain.service.OverlaySpec

/**
 * Default [OverlayRenderer]: pure logic that turns the current frame state and
 * user [OverlaySettings] into a declarative [OverlaySpec]. No drawing here — the
 * Compose Canvas consumes the spec — which keeps "what to show" unit-testable.
 *
 * Phase 1 emits the static guides (thirds, crosshair, horizon). Subject box and
 * reference outline are already plumbed through so Phases 3/5 only need to feed
 * real data; today they stay null unless a subject is present.
 */
class DefaultOverlayRenderer : OverlayRenderer {

    override fun buildSpec(
        subject: SubjectFrame,
        tilt: TiltState,
        composition: CompositionResult,
        settings: OverlaySettings,
    ): OverlaySpec {
        val horizon = if (settings.showHorizon && tilt.isAvailable) {
            HorizonSpec(rollDegrees = tilt.rollDegrees, isLevel = tilt.isLevel())
        } else null

        val subjectBox = if (settings.showSubjectBox) subject.boundingBox else null
        val landmarks = if (settings.showSubjectBox) subject.landmarks else emptyList()

        return OverlaySpec(
            showThirds = settings.showThirds,
            showCenterCrosshair = settings.showCenterCrosshair,
            horizon = horizon,
            subjectBox = subjectBox,
            landmarks = landmarks,
            referenceOutline = null, // Phase 5
            opacity = settings.opacity.coerceIn(0f, 1f),
        )
    }
}
