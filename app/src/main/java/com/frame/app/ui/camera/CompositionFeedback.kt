package com.frame.app.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frame.app.domain.model.CompositionRating
import com.frame.app.domain.model.CompositionResult
import com.frame.app.ui.theme.FrameYellow

private val Good = Color(0xFF4CAF50)
private val Fair = FrameYellow
private val Poor = Color(0xFFE57373)

/**
 * Live composition feedback: a compact status chip (rating dot + label + score)
 * with the single most important suggestion beneath it. Deliberately minimal and
 * advisory — it informs, never blocks. Hidden when there's no subject to judge.
 */
@Composable
fun CompositionFeedback(
    result: CompositionResult,
    modifier: Modifier = Modifier,
) {
    val visible = result.overall != CompositionRating.UNKNOWN && result.score != null

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val color by animateColorAsState(
            targetValue = when (result.overall) {
                CompositionRating.GOOD -> Good
                CompositionRating.FAIR -> Fair
                CompositionRating.POOR -> Poor
                CompositionRating.UNKNOWN -> Color.White
            },
            label = "ratingColor",
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Status chip.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Text(
                    text = result.overall.label(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
                result.score?.let { s ->
                    Text(
                        text = "$s",
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            // Top suggestion.
            result.suggestions.firstOrNull()?.let { suggestion ->
                Text(
                    text = suggestion,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun CompositionRating.label(): String = when (this) {
    CompositionRating.GOOD -> "Great composition"
    CompositionRating.FAIR -> "Good — minor tweaks"
    CompositionRating.POOR -> "Keep adjusting"
    CompositionRating.UNKNOWN -> ""
}
