package com.frame.app.domain.composition

import com.frame.app.domain.model.PhotoStyle

/**
 * Maps a selectable [PhotoStyle] to its composition [CompositionThresholds]
 * preset. Single source of truth for "what does this style mean to the engine".
 */
fun PhotoStyle.toThresholds(): CompositionThresholds = when (this) {
    PhotoStyle.AESTHETIC_FULL_BODY -> CompositionThresholds.AESTHETIC_FULL_BODY
    PhotoStyle.LOW_ANGLE -> CompositionThresholds.LOW_ANGLE
    PhotoStyle.CENTERED_SYMMETRICAL -> CompositionThresholds.CENTERED_SYMMETRICAL
    PhotoStyle.CLASSIC_PORTRAIT -> CompositionThresholds.CLASSIC_PORTRAIT
}
