package com.frame.app.domain.model

/**
 * A composition "fingerprint" extracted from a reference photograph (Phase 5),
 * stored in the reference library (Phase 6), and compared against the live
 * camera to guide the user toward recreating a look.
 *
 * The goal is to recreate composition, framing, perspective, and style — not to
 * reproduce a photo pixel for pixel — so every field here is a measurable,
 * environment-independent composition trait rather than raw image data.
 *
 * Defined now (Phase 1) so the camera/composition engine can be built against a
 * stable target and Phases 5-6 slot in without reshaping the core.
 */
data class ReferenceProfile(
    val id: String,
    val displayName: String,
    /** Normalized subject bounding box within the reference frame. */
    val subjectBox: NormalizedRect?,
    /** Subject area as a fraction of the whole frame, 0f..1f. */
    val subjectSizeRatio: Float?,
    /** Estimated vertical camera angle in degrees; negative = low angle looking up. */
    val cameraAngleDegrees: Float?,
    /** Normalized horizon line height (0 = top, 1 = bottom), or null if none found. */
    val horizonY: Float?,
    /** Headroom above the subject as a fraction of frame height. */
    val headroomRatio: Float?,
    /** Overall negative-space fraction of the frame. */
    val negativeSpaceRatio: Float?,
    /** Source image aspect ratio (width / height). */
    val aspectRatio: Float?,
    val style: CompositionStyle,
    /** Free-form tags / collection labels for the reference library. */
    val tags: List<String> = emptyList(),
)

/** Broad photographic styles FRAME can classify and guide toward. */
enum class CompositionStyle {
    UNKNOWN,
    CINEMATIC_PORTRAIT,
    LOW_ANGLE_PORTRAIT,
    FULL_BODY_STREET,
    ENVIRONMENTAL_PORTRAIT,
    ARCHITECTURAL,
    SYMMETRICAL,
    GOLDEN_HOUR,
    FLASH_AESTHETIC,
    CANDID,
    MIRROR,
    SILHOUETTE,
    COUPLE,
    NATURE_TRAVEL,
    CREATIVE_PERSPECTIVE,
}
