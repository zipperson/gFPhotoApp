package com.frame.app.domain.model

/**
 * The interface modes FRAME offers, sharing one camera + overlay engine. The
 * declaration order is the order of the on-screen mode strip, left to right, and
 * it is also the order a horizontal swipe steps through.
 *
 *  GUIDED  - the full "Framed"-style interactive overlay set, for one person.
 *  GROUP   - the same overlays, but guidance is for a group (face-based).
 *  MINIMAL - a clean camera with discreet assistance (subtle grid + horizon).
 *
 * Each mode maps to a default [com.frame.app.domain.service.OverlaySettings]
 * preset, which the user can then further tweak.
 */
enum class CameraMode {
    GUIDED,
    GROUP,
    MINIMAL,
}
