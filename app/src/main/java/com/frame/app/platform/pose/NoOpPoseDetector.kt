package com.frame.app.platform.pose

import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.service.PoseDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 1 placeholder detector. It performs NO detection, but it does complete
 * the pipeline: when the camera's [FrameAnalyzer] reports a frame via [onFrame],
 * this emits an (empty) [SubjectFrame] on [results]. That proves the full
 * camera -> analyzer -> detector -> results path is live before ML Kit arrives.
 *
 * Phase 3 replaces this with an ML Kit-backed detector that emits real subjects;
 * nothing else in the pipeline changes.
 */
class NoOpPoseDetector : PoseDetector {

    private val _results = MutableStateFlow(SubjectFrame.empty(timestampMs = 0L))
    override val results: Flow<SubjectFrame> = _results.asStateFlow()

    @Volatile private var running = false

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    /** Called by [FrameAnalyzer] for each camera frame. No detection, just
     *  emits an empty result so consumers see the pipeline is flowing. */
    fun onFrame(timestampMs: Long) {
        if (!running) return
        _results.value = SubjectFrame.empty(timestampMs)
    }

    /** Latest emitted frame timestamp; used to verify the seam in tests. */
    fun lastTimestamp(): Long = _results.value.timestampMs
}
