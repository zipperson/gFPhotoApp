package com.frame.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 4/8-point spacing scale. Use these instead of ad-hoc dp values so spacing
 * stays consistent and on-grid. Related elements sit closer together; the gap
 * between unrelated groups should be about double.
 */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    /** Every value in the scale, for tests and tooling. */
    val all = listOf(xxs, xs, sm, md, lg, xl, xxl)
}

/** Shared component sizes. */
object Sizes {
    /** Minimum tappable area (Android accessibility guidance). */
    val touchTarget = 48.dp

    /** Visual height of a pill chip; its tappable area is still [touchTarget]. */
    val pillHeight = 40.dp

    /** Standard icon size inside controls. */
    val icon = 24.dp

    /** Small icon used inside pills next to text. */
    val iconSmall = 16.dp

    val thumbnail = 56.dp
    val shutter = 80.dp
}
