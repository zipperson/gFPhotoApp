package com.frame.app.ui.style

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frame.app.domain.composition.CompositionTarget
import com.frame.app.domain.composition.toThresholds
import com.frame.app.domain.model.PhotoStyle
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Spacing

/**
 * Full-screen visual style picker. Used both as the first-run chooser and when
 * reopened from the camera. Each style is a tappable card with a small diagram
 * of where the subject sits in the frame; the current selection is highlighted.
 * Picking a style invokes [onStyleChosen] (persist + proceed).
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
            .padding(horizontal = Spacing.lg),
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = "Choose your look",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Text(
            text = "Siyot tunes its guidance to the style you're going for. You can change this anytime.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Spacer(Modifier.height(Spacing.lg))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
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
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(vertical = Spacing.md)
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
    val shape = RoundedCornerShape(Spacing.md)
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
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StyleDiagram(style = style, selected = selected)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.md),
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
                modifier = Modifier.padding(top = Spacing.xxs),
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

/**
 * A tiny portrait frame showing where the subject sits for [style]: thirds
 * guides plus the style's ideal subject rectangle, taken from the same
 * [CompositionTarget] the live camera uses, so the picker never drifts from
 * what the camera actually asks for. Decorative; the card text carries meaning.
 */
@Composable
private fun StyleDiagram(style: PhotoStyle, selected: Boolean, modifier: Modifier = Modifier) {
    val rect = CompositionTarget.idealRect(style.toThresholds(), preferLeftThird = false)
    val accent = if (selected) FrameYellow else Color.White

    Canvas(
        modifier = modifier
            .size(width = 40.dp, height = 72.dp)
            .clearAndSetSemantics { },
    ) {
        val corner = CornerRadius(Spacing.xs.toPx(), Spacing.xs.toPx())
        drawRoundRect(Color.White.copy(alpha = 0.08f), cornerRadius = corner)
        drawRoundRect(
            Color.White.copy(alpha = 0.25f),
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx()),
        )
        // Thirds guides.
        val guide = Color.White.copy(alpha = 0.15f)
        listOf(1f / 3f, 2f / 3f).forEach { f ->
            drawLine(guide, Offset(size.width * f, 0f), Offset(size.width * f, size.height), 1f)
            drawLine(guide, Offset(0f, size.height * f), Offset(size.width, size.height * f), 1f)
        }
        // Ideal subject zone.
        val topLeft = Offset(rect.left * size.width, rect.top * size.height)
        val zone = Size(rect.width * size.width, rect.height * size.height)
        drawRoundRect(
            accent.copy(alpha = 0.18f),
            topLeft = topLeft,
            size = zone,
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
        drawRoundRect(
            accent,
            topLeft = topLeft,
            size = zone,
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}
