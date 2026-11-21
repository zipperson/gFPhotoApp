package com.frame.app.platform.face

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.frame.app.domain.composition.GroupFraming
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.platform.camera.CameraLens
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Finds every face in the frame (ML Kit face detection, fast mode, no landmarks
 * or classification) and reports them as a group [SubjectFrame]. Mirrors the
 * shape of [com.frame.app.platform.pose.MlKitPoseDetector]: an [analyzer] for
 * CameraX, a [results] flow, start/stop, lens awareness, and the same ~12 Hz
 * throttle so group mode doesn't run hotter than single-person mode.
 *
 * Only one of the two detectors processes any given frame (see
 * [com.frame.app.platform.camera.ModeSwitchingAnalyzer]).
 */
class MlKitFaceGroupDetector {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(MIN_FACE_SIZE)
            .build()
    )

    private val _results = MutableStateFlow(SubjectFrame.empty(0L))
    val results: Flow<SubjectFrame> = _results.asStateFlow()

    private val running = AtomicBoolean(false)
    private val smoother = GroupSmoother()

    @Volatile private var lens: CameraLens = CameraLens.BACK
    @Volatile private var lastProcessedMs = 0L

    val analyzer: ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { processFrame(it) }

    fun start() = running.set(true)

    fun stop() = running.set(false)

    fun setLens(lens: CameraLens) {
        this.lens = lens
        smoother.reset()
    }

    /** Forget the last group, e.g. when group mode is switched on. */
    fun reset() {
        smoother.reset()
        _results.value = SubjectFrame.empty(0L)
    }

    fun close() {
        running.set(false)
        runCatching { detector.close() }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (!running.get() || mediaImage == null) {
            imageProxy.close()
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastProcessedMs < MIN_FRAME_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        lastProcessedMs = now

        val rotation = imageProxy.imageInfo.rotationDegrees
        val sideways = rotation == 90 || rotation == 270
        val width = if (sideways) imageProxy.height else imageProxy.width
        val height = if (sideways) imageProxy.width else imageProxy.height
        val mirror = lens == CameraLens.FRONT
        val timestamp = imageProxy.imageInfo.timestamp

        detector.process(InputImage.fromMediaImage(mediaImage, rotation))
            .addOnSuccessListener { faces ->
                val rects = faces.map { face ->
                    val b = face.boundingBox
                    val left = (b.left.toFloat() / width).coerceIn(0f, 1f)
                    val right = (b.right.toFloat() / width).coerceIn(0f, 1f)
                    NormalizedRect(
                        left = if (mirror) 1f - right else left,
                        top = (b.top.toFloat() / height).coerceIn(0f, 1f),
                        right = if (mirror) 1f - left else right,
                        bottom = (b.bottom.toFloat() / height).coerceIn(0f, 1f),
                    )
                }
                _results.value = smoother.process(GroupFraming.toSubjectFrame(rects, timestamp))
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private companion object {
        const val MIN_FRAME_INTERVAL_MS = 80L

        // Ignore faces smaller than 5% of the image width (far-away bystanders).
        const val MIN_FACE_SIZE = 0.05f
    }
}
