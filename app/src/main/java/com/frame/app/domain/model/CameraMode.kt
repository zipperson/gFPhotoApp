package com.frame.app.domain.model

/**
 * The two interface modes FRAME offers, sharing one camera + overlay engine.
 *
 *  MINIMAL - a clean camera with discreet assistance (subtle grid + horizon).
 *  GUIDED  - the full "Framed"-style interactive overlay set.
 *
 * Each mode maps to a default [com.frame.app.domain.service.OverlaySettings]
 * preset, which the user can then further tweak.
 */
enum class CameraMode {
    MINIMAL,
    GUIDED,
}
