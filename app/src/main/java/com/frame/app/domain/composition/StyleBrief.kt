package com.frame.app.domain.composition

import com.frame.app.domain.model.PhotoStyle

/**
 * A concise, natural-language brief describing how a [PhotoStyle] should be
 * framed, derived from the same [CompositionThresholds] the rule engine uses.
 * Fed to the AI so its advice + suggested target follow the chosen style rather
 * than generic portraiture — keeping AI and rule guidance consistent.
 */
fun PhotoStyle.toBrief(): String {
    val t = toThresholds()
    val placement = when {
        t.thirdsTolerance <= 0f -> "Place the subject centered (symmetrical)."
        else -> "Place the subject off-center on a vertical third."
    }
    val vertical = when {
        t.preferLowSubject && t.idealVerticalCenter >= 0.65f ->
            "Position them low in the frame with lots of space above (low-angle look)."
        t.preferLowSubject ->
            "Position them a bit below center with generous headroom."
        else ->
            "Keep headroom moderate."
    }
    val size = "Target subject size: roughly ${(t.sizeIdealMin * 100).toInt()}-" +
        "${(t.sizeIdealMax * 100).toInt()}% of the frame height."
    return "$placement $vertical $size"
}
