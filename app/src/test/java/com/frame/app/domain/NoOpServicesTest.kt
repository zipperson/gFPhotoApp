package com.frame.app.domain

import app.cash.turbine.test
import com.frame.app.domain.model.CompositionResult
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState
import com.frame.app.domain.service.NoOpCompositionEngine
import com.frame.app.platform.pose.NoOpPoseDetector
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Verifies the Phase 1 placeholder services honor their contracts so the
 * end-to-end pipeline is safe to wire before the real implementations exist.
 */
class NoOpServicesTest {

    @Test
    fun noOpCompositionEngine_returnsNone() {
        val engine = NoOpCompositionEngine()
        val result = engine.evaluate(
            subject = SubjectFrame.empty(0L),
            tilt = TiltState.UNAVAILABLE,
            frameAspectRatio = 0.75f,
        )
        assertEquals(CompositionResult.NONE, result)
    }

    @Test
    fun noOpPoseDetector_emitsEmptyFrame() = runTest {
        val detector = NoOpPoseDetector()
        detector.start()
        detector.results.test {
            val frame = awaitItem()
            assertFalse(frame.hasSubject)
            cancelAndIgnoreRemainingEvents()
        }
        detector.stop()
    }
}
