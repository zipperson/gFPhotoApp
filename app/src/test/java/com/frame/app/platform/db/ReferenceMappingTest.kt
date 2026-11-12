package com.frame.app.platform.db

import com.frame.app.domain.model.CompositionStyle
import com.frame.app.domain.model.NormalizedRect
import com.frame.app.domain.model.ReferenceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure tests for the ReferenceEntity <-> ReferenceProfile mapping, including the
 * subject-box null handling and tag (de)serialization. No Room/Android needed.
 */
class ReferenceMappingTest {

    @Test
    fun roundTrip_preservesFields() {
        val profile = ReferenceProfile(
            id = "ref-1",
            displayName = "Golden hour portrait",
            subjectBox = NormalizedRect(0.3f, 0.1f, 0.7f, 0.9f),
            subjectSizeRatio = 0.32f,
            cameraAngleDegrees = -12f,
            horizonY = 0.6f,
            headroomRatio = 0.08f,
            negativeSpaceRatio = 0.5f,
            aspectRatio = 0.75f,
            style = CompositionStyle.GOLDEN_HOUR,
            tags = listOf("couple", "travel"),
        )

        val restored = profile.toEntity(createdAtMillis = 1_000L).toDomain()

        assertEquals(profile.id, restored.id)
        assertEquals(profile.displayName, restored.displayName)
        assertEquals(profile.subjectBox, restored.subjectBox)
        assertEquals(profile.subjectSizeRatio, restored.subjectSizeRatio)
        assertEquals(profile.cameraAngleDegrees, restored.cameraAngleDegrees)
        assertEquals(profile.horizonY, restored.horizonY)
        assertEquals(CompositionStyle.GOLDEN_HOUR, restored.style)
        assertEquals(listOf("couple", "travel"), restored.tags)
    }

    @Test
    fun nullSubjectBox_mapsToNull() {
        val profile = ReferenceProfile(
            id = "ref-2",
            displayName = "No subject",
            subjectBox = null,
            subjectSizeRatio = null,
            cameraAngleDegrees = null,
            horizonY = null,
            headroomRatio = null,
            negativeSpaceRatio = null,
            aspectRatio = null,
            style = CompositionStyle.UNKNOWN,
            tags = emptyList(),
        )

        val restored = profile.toEntity().toDomain()
        assertNull(restored.subjectBox)
        assertEquals(emptyList<String>(), restored.tags)
    }

    @Test
    fun unknownStyleString_fallsBackToUnknown() {
        val entity = ReferenceProfile(
            id = "ref-3",
            displayName = "x",
            subjectBox = null,
            subjectSizeRatio = null,
            cameraAngleDegrees = null,
            horizonY = null,
            headroomRatio = null,
            negativeSpaceRatio = null,
            aspectRatio = null,
            style = CompositionStyle.CINEMATIC_PORTRAIT,
            tags = emptyList(),
        ).toEntity().copy(style = "NOT_A_REAL_STYLE")

        assertEquals(CompositionStyle.UNKNOWN, entity.toDomain().style)
    }
}
