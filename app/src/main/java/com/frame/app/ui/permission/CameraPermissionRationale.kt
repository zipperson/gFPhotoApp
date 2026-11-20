package com.frame.app.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frame.app.R
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Spacing

/**
 * Explains why FRAME needs the camera and offers the appropriate action.
 *
 * @param isPermanentlyDenied when true the user chose "don't ask again", so the
 *        in-app request will no longer show a dialog — we deep-link to the app's
 *        system settings instead of re-requesting.
 * @param onRequest invoked to (re)launch the runtime permission request.
 */
@Composable
fun CameraPermissionRationale(
    isPermanentlyDenied: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = FrameYellow,
            modifier = Modifier.size(Spacing.xxl),
        )
        Text(
            text = stringResource(R.string.permission_camera_rationale_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = Spacing.md),
        )
        Text(
            text = stringResource(
                if (isPermanentlyDenied) R.string.permission_denied_body
                else R.string.permission_camera_rationale_body
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Button(
            onClick = {
                if (isPermanentlyDenied) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } else {
                    onRequest()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = FrameYellow,
                contentColor = Color.Black,
            ),
            modifier = Modifier.padding(top = Spacing.lg),
        ) {
            Text(
                style = MaterialTheme.typography.labelLarge,
                text = stringResource(
                    if (isPermanentlyDenied) R.string.permission_open_settings
                    else R.string.permission_grant
                )
            )
        }
    }
}
