package com.frame.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import com.frame.app.ui.camera.LocalShutterKeys
import com.frame.app.ui.camera.ShutterKeyDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.frame.app.ui.FrameApp
import com.frame.app.ui.theme.FrameTheme

/**
 * Single-activity host. All screens are Composables; this activity just sets up
 * the theme and hands off to [FrameApp]. Keeping it this thin means the camera
 * lifecycle is owned by Compose + the CameraController (Task 3), not the Activity.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalShutterKeys provides shutterKeys) {
                FrameTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        FrameApp()
                    }
                }
            }
        }
    }

    // Volume keys act as a shutter while the camera screen has registered.
    private val shutterKeys = ShutterKeyDispatcher()

    private fun isVolumeKey(keyCode: Int) =
        keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        if (isVolumeKey(keyCode) && shutterKeys.onKeyDown(event.repeatCount)) true
        else super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        if (isVolumeKey(keyCode) && shutterKeys.onKeyUp()) true
        else super.onKeyUp(keyCode, event)
}
