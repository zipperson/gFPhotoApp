package com.frame.app.platform.pose

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.service.PoseDetector
import com.frame.app.platform.camera.CameraLens
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [PoseDetector] backed by ML Kit Pose Detection (base/fast model, STREAM_MODE
 * for real-time video). Provides an [ImageAnalysis.Analyzer] ([analyzer]) that
 * the camera controller binds; each frame is handed to ML Kit, and results are
 * mapped to a normalized [SubjectFrame] via [PoseMapping] and emitted on
 * [results].
 *
 * Lens awareness: the active lens is set via [setLens]; the front camera's
 * mirrored preview is corrected by x-flipping landmark coordinates in the
 * mapping so overlays line up with what the user sees.
 *
 * Threading: ML Kit's processing runs off the analysis executor CameraX calls
 * the analyzer on; we keep the ImageProxy open until ML Kit finishes, then close
 * it (required, or the analyzer stalls under KEEP_ONLY_LATEST backpressure).
 */
class MlKitPoseDetector : PoseDetector {

    private val mlDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private val _results = MutableStateFlow(SubjectFrame.empty(0L))
    override val results: Flow<SubjectFrame> = _results.asStateFlow()

    private val running = AtomicBoolean(false)

    // Smooths jitter and holds through brief detection dropouts. Touched only on
    // the single analysis thread (ML Kit callbacks run on that executor).
    private val smoother = SubjectSmoother()

    @Volatile private var lens: CameraLens = CameraLens.BACK

    // Throttle: process at most one frame per MIN_FRAME_INTERVAL_MS (~12Hz).
    // Caps CPU/GPU work and heat; guidance still feels live. Frames arriving
    // sooner are dropped (and closed) without running ML Kit.
    @Volatile private var lastProcessedMs: Long = 0L

    /** The analyzer to bind to CameraX's ImageAnalysis use case. */
    val analyzer: ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { imageProxy ->
        processFrame(imageProxy)
    }

    override fun start() {
        running.set(true)
    }

    override fun stop() {
        running.set(false)
    }

    /** Update the active lens so mirroring is applied correctly after a switch. */
    fun setLens(lens: CameraLens) {
        this.lens = lens
        // New camera geometry; drop smoothing history so it doesn't lerp across.
        smoother.reset()
    }

    /** Release the underlying ML Kit detector. Call once, when permanently done. */
    fun close() {
        running.set(false)
        runCatching { mlDetector.close() }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (!running.get() || mediaImage == null) {
            imageProxy.close()
            return
        }

        // Throttle to the target rate: drop (and close) frames that arrive too soon.
        val now = System.currentTimeMillis()
        if (now - lastProcessedMs < MIN_FRAME_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        lastProcessedMs = now

        val rotation = imageProxy.imageInfo.rotationDegrees
        // After applying rotation, ML Kit reports coordinates in the upright
        // image space. Compute that space's dimensions for normalization.
        val uprightWidth: Int
        val uprightHeight: Int
        if (rotation == 90 || rotation == 270) {
            uprightWidth = imageProxy.height
            uprightHeight = imageProxy.width
        } else {
            uprightWidth = imageProxy.width
            uprightHeight = imageProxy.height
        }

        val mirror = lens == CameraLens.FRONT
        val timestamp = imageProxy.imageInfo.timestamp
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        mlDetector.process(inputImage)
            .addOnSuccessListener { pose ->
                val raw = mapPose(pose, uprightWidth, uprightHeight, mirror, timestamp)
                _results.value = smoother.process(raw)
            }
            .addOnFailureListener {
                // Drop this frame's result; keep the last-known subject state.
            }
            .addOnCompleteListener {
                // Always close so CameraX can deliver the next frame.
                imageProxy.close()
            }
    }

    private fun mapPose(
        pose: Pose,
        width: Int,
        height: Int,
        mirror: Boolean,
        timestampMs: Long,
    ): SubjectFrame {
        val raw = pose.allPoseLandmarks.mapNotNull { lm ->
            val type = MlKitToDomainLandmark[lm.landmarkType] ?: return@mapNotNull null
            PoseMapping.RawLandmark(
                type = type,
                x = lm.position.x,
                y = lm.position.y,
                inFrameLikelihood = lm.inFrameLikelihood,
            )
        }
        return PoseMapping.toSubjectFrame(
            landmarks = raw,
            imageWidth = width,
            imageHeight = height,
            mirrorX = mirror,
            timestampMs = timestampMs,
        )
    }

    private companion object {
        // ~12 Hz: smooth enough for live guidance, much lighter than full rate.
        const val MIN_FRAME_INTERVAL_MS = 80L
    }
}
