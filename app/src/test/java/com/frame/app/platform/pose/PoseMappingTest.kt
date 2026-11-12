package com.frame.app.platform.pose

import com.frame.app.domain.model.LandmarkType
import com.frame.app.domain.model.NormalizedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure tests for ML Kit landmark -> SubjectFrame mapping: normalization,
 * front-camera mirroring, likelihood filtering, and bounding-box computation.
 */
class PoseMappingTest {

    private val eps = 1e-4f

    private fun raw(type: LandmarkType, x: Float, y: Float, l: Float = 0.9f) =
        PoseMapping.RawLandmark(type, x, y, l)

    @Test
    fun normalizesByImageDimensions() {
        val frame = PoseMapping.toSubjectFrame(
            landmarks = listOf(raw(LandmarkType.NOSE, 480f, 360f)),
            imageWidth = 960,
            imageHeight = 720,
            mirrorX = false,
            timestampMs = 1L,
        )
        val nose = frame.landmark(LandmarkType.NOSE)!!
        assertEquals(0.5f, nose.position.x, eps)
        assertEquals(0.5f, nose.position.y, eps)
    }

    @Test
    fun mirrorsXForFrontCamera() {
        val frame = PoseMapping.toSubjectFrame(
            landmarks = listOf(raw(LandmarkType.NOSE, 240f, 360f)), // x=0.25
            imageWidth = 960,
            imageHeight = 720,
            mirrorX = true,
            timestampMs = 1L,
        )
        val nose = frame.landmark(LandmarkType.NOSE)!!
        assertEquals(0.75f, nose.position.x, eps) // mirrored: 1 - 0.25
        assertEquals(0.5f, nose.position.y, eps)  // y unchanged
    }

    @Test
    fun dropsLowLikelihoodLandmarks() {
        val frame = PoseMapping.toSubjectFrame(
            landmarks = listOf(
                raw(LandmarkType.NOSE, 100f, 100f, l = 0.9f),
                raw(LandmarkType.LEFT_ANKLE, 200f, 700f, l = 0.1f),
            ),
            imageWidth = 1000,
            imageHeight = 1000,
            mirrorX = false,
            timestampMs = 1L,
        )
        assertTrue(frame.landmark(LandmarkType.NOSE) != null)
        assertNull(frame.landmark(LandmarkType.LEFT_ANKLE))
    }

    @Test
    fun noUsableLandmarks_isEmptyFrame() {
        val frame = PoseMapping.toSubjectFrame(
            landmarks = listOf(raw(LandmarkType.NOSE, 1f, 1f, l = 0.0f)),
            imageWidth = 1000,
            imageHeight = 1000,
            mirrorX = false,
            timestampMs = 7L,
        )
        assertFalse(frame.hasSubject)
        assertEquals(7L, frame.timestampMs)
    }

    @Test
    fun degenerateImageSize_isEmptyFrame() {
        val frame = PoseMapping.toSubjectFrame(
            landmarks = listOf(raw(LandmarkType.NOSE, 1f, 1f)),
            imageWidth = 0,
            imageHeight = 0,
            mirrorX = false,
            timestampMs = 1L,
        )
        assertFalse(frame.hasSubject)
    }

    @Test
    fun boundingBox_wrapsAllPoints() {
        val box = PoseMapping.boundingBox(
            listOf(
                NormalizedPoint(0.2f, 0.1f),
                NormalizedPoint(0.8f, 0.3f),
                NormalizedPoint(0.5f, 0.9f),
            )
        )!!
        assertEquals(0.2f, box.left, eps)
        assertEquals(0.1f, box.top, eps)
        assertEquals(0.8f, box.right, eps)
        assertEquals(0.9f, box.bottom, eps)
    }

    @Test
    fun boundingBox_emptyList_isNull() {
        assertNull(PoseMapping.boundingBox(emptyList()))
    }

    @Test
    fun subjectFrame_hasBoundingBoxFromLandmarks() {
        val frame = PoseMapping.toSubjectFrame(
            landmarks = listOf(
                raw(LandmarkType.LEFT_SHOULDER, 300f, 400f),
                raw(LandmarkType.RIGHT_SHOULDER, 700f, 400f),
                raw(LandmarkType.LEFT_HIP, 350f, 800f),
                raw(LandmarkType.RIGHT_HIP, 650f, 800f),
            ),
            imageWidth = 1000,
            imageHeight = 1000,
            mirrorX = false,
            timestampMs = 1L,
        )
        assertTrue(frame.hasSubject)
        val box = frame.boundingBox!!
        assertEquals(0.3f, box.left, eps)
        assertEquals(0.4f, box.top, eps)
        assertEquals(0.7f, box.right, eps)
        assertEquals(0.8f, box.bottom, eps)
    }
}
