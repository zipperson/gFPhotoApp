package com.frame.app.ui.camera.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frame.app.domain.model.CompositionRating
import com.frame.app.domain.model.CompositionResult
import com.frame.app.ui.components.GlassPill
import com.frame.app.ui.theme.FrameGreen
import com.frame.app.ui.theme.FrameYellow
import com.frame.app.ui.theme.Sizes

/**
 * What the status pill should show, derived purely from a [CompositionResult] so
 * the wording rules are unit-testable without any UI.
 *
 * @property visible whether there is anything to judge (hidden with no subject)
 * @property message the single most useful line: the top suggestion if there is
 *                   one, otherwise a short read of the rating
 * @property score   0..100, or null
 * @property isGood  drives the colour: green when good, yellow ("act on this")
 *                   otherwise. Red is deliberately not used.
 */
data class StatusPillModel(
    val visible: Boolean,
    val message: String,
    val score: Int?,
    val isGood: Boolean,
)

/**
 * @param peopleCount set in Group mode: the "all good" line then names the
 *        group ("Great group shot · 3 people"). Suggestions are unchanged.
 */
fun CompositionResult.toStatusPillModel(peopleCount: Int? = null): StatusPillModel {
    val hasRead = overall != CompositionRating.UNKNOWN && score != null
    if (!hasRead) return StatusPillModel(visible = false, message = "", score = null, isGood = false)

    val isGood = overall == CompositionRating.GOOD
    val goodLine = if (peopleCount != null) {
        "Great group shot · $peopleCount ${if (peopleCount == 1) "person" else "people"}"
    } else "Great composition"
    val message = suggestions.firstOrNull()
        ?: if (isGood) goodLine else "Keep adjusting"
    return StatusPillModel(visible = true, message = message, score = score, isGood = isGood)
}

/**
 * One line of live guidance: a status dot, the top suggestion, and the score.
 * Replaces the old rating chip + separate suggestion pill.
 */
@Composable
fun StatusPill(
    result: CompositionResult,
    modifier: Modifier = Modifier,
    peopleCount: Int? = null,
) {
    val model = result.toStatusPillModel(peopleCount)

    AnimatedVisibility(
        visible = model.visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val accent by animateColorAsState(
            targetValue = if (model.isGood) FrameGreen else FrameYellow,
            label = "statusAccent",
        )
        GlassPill {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Text(
                text = model.message,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            model.score?.let { s ->
                Text(
                    text = "$s",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
            }
        }
    }
}

/**
 * Shown instead of [StatusPill] when composition guidance is turned off, so
 * pausing is never invisible. Tapping it resumes guidance.
 */
@Composable
fun GuidancePausedPill(
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPill(
        modifier = modifier,
        onClick = onResume,
        onClickLabel = "Resume composition guidance",
    ) {
        Icon(
            imageVector = Icons.Filled.VisibilityOff,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(Sizes.iconSmall),
        )
        Text(
            text = "Guidance paused. Tap to resume",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}
