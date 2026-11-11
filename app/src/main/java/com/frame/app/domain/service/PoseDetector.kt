package com.frame.app.domain.service

import com.frame.app.domain.model.SubjectFrame
import kotlinx.coroutines.flow.Flow

/**
 * Detects a human subject + body landmarks from camera frames and emits
 * normalized [SubjectFrame] results.
 *
 * The contract is intentionally frame-source-agnostic: the platform camera
 * layer feeds frames in via its own adapter, and consumers observe [results].
 * The ML Kit-backed implementation lands in Phase 3; Phase 1 uses
 * [NoOpPoseDetector] to prove the seam without doing detection.
 */
interface PoseDetector {

    /**
     * Stream of detection results. Emitted on the detector's own cadence
     * (typically throttled below camera rate); consumers should treat it as the
     * latest-known subject state.
     */
    val results: Flow<SubjectFrame>

    /** Begin consuming frames / allocating detector resources. */
    fun start()

    /** Stop detection and release resources. Safe to call when already stopped. */
    fun stop()
}
