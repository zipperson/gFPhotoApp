package com.frame.app.platform.db

import com.frame.app.domain.model.CompositionStyle
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.ReferenceProfile
import com.frame.app.domain.service.ReferenceLibrary
import com.frame.app.platform.db.dao.ReferenceDao
import com.frame.app.platform.db.entity.ReferenceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [ReferenceLibrary]. Fully implemented at the persistence level so
 * Phase 6 only needs to call it; nothing writes here in Phase 1. Handles mapping
 * between the flat [ReferenceEntity] and the rich [ReferenceProfile] domain model.
 */
class RoomReferenceLibrary(
    private val dao: ReferenceDao,
) : ReferenceLibrary {

    override fun observeAll(): Flow<List<ReferenceProfile>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCollection(tag: String): Flow<List<ReferenceProfile>> =
        dao.observeByTag(tag).map { list -> list.map { it.toDomain() } }

    override suspend fun save(profile: ReferenceProfile) {
        dao.upsert(profile.toEntity())
    }

    override suspend fun get(id: String): ReferenceProfile? =
        dao.getById(id)?.toDomain()

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}

/* --- Mapping between entity and domain model --- */

internal fun ReferenceEntity.toDomain(): ReferenceProfile {
    val box = if (subjectLeft != null && subjectTop != null &&
        subjectRight != null && subjectBottom != null
    ) {
        NormalizedRect(subjectLeft, subjectTop, subjectRight, subjectBottom)
    } else null

    return ReferenceProfile(
        id = id,
        displayName = displayName,
        subjectBox = box,
        subjectSizeRatio = subjectSizeRatio,
        cameraAngleDegrees = cameraAngleDegrees,
        horizonY = horizonY,
        headroomRatio = headroomRatio,
        negativeSpaceRatio = negativeSpaceRatio,
        aspectRatio = aspectRatio,
        style = runCatching { CompositionStyle.valueOf(style) }
            .getOrDefault(CompositionStyle.UNKNOWN),
        tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
    )
}

internal fun ReferenceProfile.toEntity(createdAtMillis: Long = System.currentTimeMillis()): ReferenceEntity =
    ReferenceEntity(
        id = id,
        displayName = displayName,
        subjectLeft = subjectBox?.left,
        subjectTop = subjectBox?.top,
        subjectRight = subjectBox?.right,
        subjectBottom = subjectBox?.bottom,
        subjectSizeRatio = subjectSizeRatio,
        cameraAngleDegrees = cameraAngleDegrees,
        horizonY = horizonY,
        headroomRatio = headroomRatio,
        negativeSpaceRatio = negativeSpaceRatio,
        aspectRatio = aspectRatio,
        style = style.name,
        tags = tags.joinToString(","),
        createdAtMillis = createdAtMillis,
    )
