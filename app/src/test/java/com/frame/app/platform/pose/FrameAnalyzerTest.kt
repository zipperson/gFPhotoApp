package com.frame.app.platform.pose

import androidx.camera.core.ImageProxy
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the Phase 3 seam adapter: frames are forwarded to the sink and the
 * ImageProxy is always closed (or CameraX's analyzer stalls). Uses a mocked
 * ImageProxy, so this is a plain JVM test.
 */
class FrameAnalyzerTest {

    @Test
    fun analyze_forwardsTimestampAndClosesImage() {
        var receivedTs = -1L
        var receivedIndex = -1L
        val analyzer = FrameAnalyzer { ts, index ->
            receivedTs = ts
            receivedIndex = index
        }

        val image = mockk<ImageProxy>(relaxed = true)
        every { image.imageInfo.timestamp } returns 12345L

        analyzer.analyze(image)

        assertEquals(12345L, receivedTs)
        assertEquals(1L, receivedIndex)
        assertEquals(1L, analyzer.framesProcessed())
        verify(exactly = 1) { image.close() }
    }

    @Test
    fun analyze_closesImageEvenIfSinkThrows() {
        val analyzer = FrameAnalyzer { _, _ -> throw RuntimeException("boom") }
        val image = mockk<ImageProxy>(relaxed = true)
        every { image.imageInfo.timestamp } returns 1L

        runCatching { analyzer.analyze(image) }

        // The finally block must still close the frame.
        verify(exactly = 1) { image.close() }
    }

    @Test
    fun frameCount_incrementsPerFrame() {
        val analyzer = FrameAnalyzer { _, _ -> }
        val image = mockk<ImageProxy>(relaxed = true)
        every { image.imageInfo.timestamp } returns 0L

        repeat(3) { analyzer.analyze(image) }
        assertEquals(3L, analyzer.framesProcessed())
    }
}
