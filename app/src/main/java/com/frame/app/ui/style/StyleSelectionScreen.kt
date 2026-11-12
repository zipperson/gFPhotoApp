package com.frame.app.ui.style

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.frame.app.domain.model.PhotoStyle
import com.frame.app.ui.theme.FrameYellow

/**
 * Full-screen visual style picker. Used both as the first-run chooser and when
 * reopened from the camera. Each style is a tappable card; the current selection
 * is highlighted. Picking a style invokes [onStyleChosen] (persist + proceed).
 *
 * @param current     currently selected style (highlighted), or null on first run
 * @param showBack    whether to show a back affordance (true when reopened)
 * @param onBack      invoked when dismissing without changing (only if showBack)
 */
@Composable
fun StyleSelectionScreen(
    current: PhotoStyle?,
    onStyleChosen: (PhotoStyle) -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Choose your look",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "FRAME tunes its guidance to the style you're going for. You can change this anytime.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(PhotoStyle.values()) { style ->
                StyleCard(
                    style = style,
                    selected = style == current,
                    onClick = { onStyleChosen(style) },
                )
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }

        if (showBack) {
            Text(
                text = "Back",
                color = FrameYellow,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBack)
                    .padding(vertical = 16.dp)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun StyleCard(
    style: PhotoStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (selected) FrameYellow.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.05f),
                shape = shape,
            )
            .border(
                if (selected) BorderStroke(2.dp, FrameYellow)
                else BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shape,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = style.icon(),
                contentDescription = null,
                tint = if (selected) FrameYellow else Color.White,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(
                text = style.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = style.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = FrameYellow,
            )
        }
    }
}

private fun PhotoStyle.icon(): ImageVector = when (this) {
    PhotoStyle.AESTHETIC_FULL_BODY -> Icons.Outlined.Person
    PhotoStyle.LOW_ANGLE -> Icons.Outlined.PhotoCamera
    PhotoStyle.CENTERED_SYMMETRICAL -> Icons.Outlined.CenterFocusStrong
    PhotoStyle.CLASSIC_PORTRAIT -> Icons.Outlined.AccountBox
}
