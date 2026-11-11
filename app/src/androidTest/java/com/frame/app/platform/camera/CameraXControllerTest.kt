package com.frame.app.platform.camera

import android.content.pm.PackageManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for the CameraX provider acquisition. Runs on an emulator
 * or device (needs a camera). Verifies the provider is obtainable, which is the
 * prerequisite for binding the Preview use case in [CameraXController.bind].
 *
 * Note: full bind requires a LifecycleOwner + PreviewView on the main thread,
 * which is exercised by launching the app; here we assert the provider future
 * resolves, keeping the test lightweight and device-tolerant.
 */
@RunWith(AndroidJUnit4::class)
class CameraXControllerTest {

    @Test
    fun cameraProvider_isAvailableOnDeviceWithCamera() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assumeTrue(
            "No camera on this device; skipping",
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
        )

        val provider = ProcessCameraProvider.getInstance(context).get()
        assertNotNull(provider)
        // At least one lens should be present on a camera-equipped device.
        val hasAnyCamera = provider.availableCameraInfos.isNotEmpty()
        assertNotNull(provider.availableCameraInfos)
        assert(hasAnyCamera) { "Expected at least one camera on a camera-equipped device" }
    }
}
