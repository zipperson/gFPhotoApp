package com.frame.app.platform.pose

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicLong

/**
 * CameraX [ImageAnalysis.Analyzer] that bridges camera frames to a pose
 * detector. In Phase 1 the detector is a no-op, so this just proves the seam:
 * frames genuinely arrive here at camera rate, get counted, and are released.
 *
 * Phase 3 swaps the body of [analyze] to hand the frame's image to ML Kit. The
 * CameraX binding and this analyzer stay unchanged — that's the seam Task 10
 * verifies.
 *
 * @param onFrame invoked with each frame's timestamp; lets the app observe that
 *        the pipeline is live (used to drive the no-op detector's emissions).
 */
class FrameAnalyzer(
    private val onFrame: (timestampMs: Long, frameIndex: Long) -> Unit,
) : ImageAnalysis.Analyzer {

    private val frameCount = AtomicLong(0)

    override fun analyze(image: ImageProxy) {
        try {
            val index = frameCount.incrementAndGet()
            // Phase 3: pass image (image.image + rotation) to ML Kit here.
            onFrame(image.imageInfo.timestamp, index)
        } finally {
            // Must always close, or the analyzer stalls (backpressure).
            image.close()
        }
    }

    fun framesProcessed(): Long = frameCount.get()
}
