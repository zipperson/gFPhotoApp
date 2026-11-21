package com.frame.app.ui.camera

import com.frame.app.domain.model.CameraMode
import com.frame.app.domain.model.SelfTimer
import com.frame.app.ui.camera.gesture.SwipeDirection
import com.frame.app.ui.camera.gesture.afterSwipe
import com.frame.app.ui.camera.gesture.classifySwipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShutterControlsTest {

    @Test
    fun exposureMapping_topIsBrightest_bottomIsDarkest() {
        assertEquals(12, exposureIndexForFraction(0f, -12, 12))
        assertEquals(-12, exposureIndexForFraction(1f, -12, 12))
        assertEquals(0, exposureIndexForFraction(0.5f, -12, 12))
        // Out-of-range drags clamp.
        assertEquals(12, exposureIndexForFraction(-0.5f, -12, 12))
        assertEquals(-12, exposureIndexForFraction(2f, -12, 12))
    }

    @Test
    fun exposureMapping_roundTrips() {
        for (i in -6..6) {
            val f = fractionForExposureIndex(i, -6, 6)
            assertEquals(i, exposureIndexForFraction(f, -6, 6))
        }
    }

    @Test
    fun selfTimer_cyclesOffThreeTenOff() {
        assertEquals(SelfTimer.THREE, SelfTimer.OFF.next())
        assertEquals(SelfTimer.TEN, SelfTimer.THREE.next())
        assertEquals(SelfTimer.OFF, SelfTimer.TEN.next())
        assertFalse(SelfTimer.OFF.isOn)
        assertTrue(SelfTimer.THREE.isOn)
        assertEquals("10s", SelfTimer.TEN.label)
    }

    @Test
    fun swipe_belowThreshold_isIgnored() {
        assertNull(classifySwipe(dx = 30f, dy = 0f, thresholdPx = 100f))
    }

    @Test
    fun swipe_horizontalPastThreshold_isClassified() {
        assertEquals(SwipeDirection.LEFT, classifySwipe(-120f, 10f, 100f))
        assertEquals(SwipeDirection.RIGHT, classifySwipe(120f, -10f, 100f))
    }

    @Test
    fun swipe_mostlyVertical_isIgnored() {
        assertNull(classifySwipe(dx = 120f, dy = 100f, thresholdPx = 100f))
    }

    @Test
    fun swipeMapping_stepsThroughGuidedGroupMinimal_andStopsAtTheEnds() {
        // Swipe left = move to the right-hand neighbour in the strip.
        assertEquals(CameraMode.GROUP, CameraMode.GUIDED.afterSwipe(SwipeDirection.LEFT))
        assertEquals(CameraMode.MINIMAL, CameraMode.GROUP.afterSwipe(SwipeDirection.LEFT))
        assertEquals(CameraMode.MINIMAL, CameraMode.MINIMAL.afterSwipe(SwipeDirection.LEFT))
        // Swipe right = move to the left-hand neighbour.
        assertEquals(CameraMode.GROUP, CameraMode.MINIMAL.afterSwipe(SwipeDirection.RIGHT))
        assertEquals(CameraMode.GUIDED, CameraMode.GROUP.afterSwipe(SwipeDirection.RIGHT))
        assertEquals(CameraMode.GUIDED, CameraMode.GUIDED.afterSwipe(SwipeDirection.RIGHT))
    }

    @Test
    fun dispatcher_withoutHandler_doesNotConsume() {
        val d = ShutterKeyDispatcher()
        assertFalse(d.onKeyDown(0))
        assertFalse(d.onKeyUp())
    }

    @Test
    fun dispatcher_firesOncePerPress_andConsumesRepeats() {
        val d = ShutterKeyDispatcher()
        var count = 0
        d.register { count++ }
        assertTrue(d.onKeyDown(0))
        assertTrue(d.onKeyDown(1))
        assertTrue(d.onKeyDown(2))
        assertEquals(1, count)
        assertTrue(d.onKeyUp())
    }

    @Test
    fun dispatcher_unregister_releasesKeys() {
        val d = ShutterKeyDispatcher()
        val unregister = d.register { }
        unregister()
        assertFalse(d.onKeyDown(0))
    }

    @Test
    fun dispatcher_staleUnregister_doesNotRemoveNewerHandler() {
        val d = ShutterKeyDispatcher()
        val oldUnregister = d.register { }
        var fired = false
        d.register { fired = true }
        oldUnregister()
        assertTrue(d.onKeyDown(0))
        assertTrue(fired)
    }
}
