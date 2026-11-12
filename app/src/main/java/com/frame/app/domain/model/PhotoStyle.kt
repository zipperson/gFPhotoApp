package com.frame.app.domain.model

/**
 * A user-selectable photography style. Each style tunes the composition engine
 * (via its threshold preset) and carries display metadata for the style picker.
 *
 * Kept in the domain as a plain enum (display strings here are not localized
 * resources but plain copy; the UI can override with string resources if/when
 * localization is added). The default is [AESTHETIC_FULL_BODY], FRAME's primary
 * "TikTok aesthetic full-body" use case.
 */
enum class PhotoStyle(
    val displayName: String,
    val tagline: String,
) {
    AESTHETIC_FULL_BODY(
        displayName = "Aesthetic Full-Body",
        tagline = "The TikTok look — full body, lots of space, placed on a third.",
    ),
    LOW_ANGLE(
        displayName = "Low Angle",
        tagline = "Shot from low down to make the subject tall and dramatic.",
    ),
    CENTERED_SYMMETRICAL(
        displayName = "Centered",
        tagline = "Subject dead-center and balanced, for symmetrical shots.",
    ),
    CLASSIC_PORTRAIT(
        displayName = "Classic Portrait",
        tagline = "Traditional framing — fuller subject, tighter headroom.",
    );

    companion object {
        val DEFAULT = AESTHETIC_FULL_BODY
    }
}
