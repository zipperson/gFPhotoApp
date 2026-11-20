package com.frame.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * FRAME's type scale: one font family, FOUR sizes (20 / 16 / 14 / 12) and TWO
 * weights (Normal / SemiBold). Every Material slot maps onto one of these, so
 * any text in the app automatically stays inside the system. Hierarchy comes
 * from size, weight and opacity, not from adding more styles.
 *
 *   20 SemiBold  titles, sheet and screen headings
 *   16 SemiBold  card names, emphasised body
 *   16 Normal    long-form body
 *   14 Normal    default body / secondary text
 *   14 SemiBold  chips, buttons, status text
 *   12 Normal    captions
 *   12 SemiBold  small labels, counters
 */
private val Title = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
private val SubtitleStrong = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
private val BodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
private val Body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
private val LabelStrong = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
private val Caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
private val CaptionStrong = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)

val FrameTypography = Typography(
    displayLarge = Title,
    displayMedium = Title,
    displaySmall = Title,
    headlineLarge = Title,
    headlineMedium = Title,
    headlineSmall = Title,
    titleLarge = Title,
    titleMedium = SubtitleStrong,
    titleSmall = SubtitleStrong,
    bodyLarge = BodyLarge,
    bodyMedium = Body,
    bodySmall = Caption,
    labelLarge = LabelStrong,
    labelMedium = CaptionStrong,
    labelSmall = CaptionStrong,
)
