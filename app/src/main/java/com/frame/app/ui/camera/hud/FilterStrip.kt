package com.frame.app.ui.camera.hud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.frame.app.domain.model.PhotoFilter
import com.frame.app.ui.components.GlassPill
import com.frame.app.ui.theme.Spacing

/**
 * A horizontally scrolling row of look chips. The selected look is solid yellow
 * (same "active" treatment as every other control). Picking one applies it to
 * the live preview straight away.
 */
@Composable
fun FilterStrip(
    selected: PhotoFilter,
    onSelect: (PhotoFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Bring the selected look into view when the strip opens or the look changes.
    LaunchedEffect(selected) {
        listState.animateScrollToItem(PhotoFilter.entries.indexOf(selected))
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(PhotoFilter.entries) { filter ->
            val isSelected = filter == selected
            GlassPill(
                active = isSelected,
                onClick = { onSelect(filter) },
                onClickLabel = "Use ${filter.displayName} look",
                modifier = Modifier.semantics { this.selected = isSelected },
            ) {
                Text(
                    text = filter.displayName,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
