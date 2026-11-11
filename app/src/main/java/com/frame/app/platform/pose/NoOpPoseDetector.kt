package com.frame.app.platform.pose

import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.service.PoseDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 1 placeholder. Emits no subject detections. Exists so the camera
 * pipeline and the Phase 3 integration seam can be wired and verified
 * end-to-end before ML Kit is introduced. Replace with the ML Kit-backed
 * detector in Phase 3 without touching the camera binding or ViewModel.
 */
class NoOpPoseDetector : PoseDetector {

    private val _results = MutableStateFlow(SubjectFrame.empty(timestampMs = 0L))
    override val results: Flow<SubjectFrame> = _results.asStateFlow()

    override fun start() {
        // No-op: no frames are consumed in Phase 1.
    }

    override fun stop() {
        // No-op.
    }
}
