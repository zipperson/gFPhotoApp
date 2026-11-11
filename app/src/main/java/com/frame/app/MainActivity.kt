package com.frame.app

import android.os.Bundle
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
            FrameTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FrameApp()
                }
            }
        }
    }
}
