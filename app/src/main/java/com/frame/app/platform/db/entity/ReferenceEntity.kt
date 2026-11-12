package com.frame.app.platform.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted reference photo + its extracted composition profile (Phase 5/6).
 * Scaffolded now so the schema and [com.frame.app.domain.service.ReferenceLibrary]
 * wiring exist from Phase 1; no rows are written until the reference features
 * are built. Nullable numeric fields mirror the optional traits in
 * [com.frame.app.domain.model.ReferenceProfile].
 */
@Entity(tableName = "reference_profiles")
data class ReferenceEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val subjectLeft: Float?,
    val subjectTop: Float?,
    val subjectRight: Float?,
    val subjectBottom: Float?,
    val subjectSizeRatio: Float?,
    val cameraAngleDegrees: Float?,
    val horizonY: Float?,
    val headroomRatio: Float?,
    val negativeSpaceRatio: Float?,
    val aspectRatio: Float?,
    val style: String,
    /** Comma-separated tags; normalized into [CollectionTagEntity] later if needed. */
    val tags: String,
    val createdAtMillis: Long,
)
