package com.frame.app.platform.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Routes each analysis frame to exactly one detector: the single-person pose
 * detector normally, the face-based group detector in group mode. Each
 * delegate is responsible for closing the frame, as before. Running only one
 * model at a time keeps group mode as cool as the existing mode.
 */
class ModeSwitchingAnalyzer(
    private val single: ImageAnalysis.Analyzer,
    private val group: ImageAnalysis.Analyzer,
) : ImageAnalysis.Analyzer {

    @Volatile
    var groupMode: Boolean = false

    override fun analyze(image: ImageProxy) {
        if (groupMode) group.analyze(image) else single.analyze(image)
    }
}
