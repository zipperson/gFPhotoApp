package com.frame.app.platform.storage

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Pure unit tests for capture filename generation. No Android dependency.
 */
class PhotoNamingTest {

    @Test
    fun fileName_hasFramePrefixAndJpgExtension() {
        val name = PhotoNaming.jpegFileName()
        assertTrue("expected FRAME_ prefix, was: $name", name.startsWith("FRAME_"))
        assertTrue("expected .jpg extension, was: $name", name.endsWith(".jpg"))
    }

    @Test
    fun fileName_encodesTheTimestamp() {
        // 2026-11-11 15:30:02.123 UTC
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 11, 15, 30, 2)
            set(Calendar.MILLISECOND, 123)
        }
        val name = PhotoNaming.jpegFileName(cal.timeInMillis)
        // Format: FRAME_yyyyMMdd_HHmmss_SSS.jpg -> FRAME_20261111_153002_123.jpg
        assertTrue("was: $name", name.matches(Regex("""FRAME_\d{8}_\d{6}_\d{3}\.jpg""")))
        assertTrue("was: $name", name.contains("20261111_153002_123"))
    }

    @Test
    fun fileName_isUniqueAcrossDifferentTimestamps() {
        val a = PhotoNaming.jpegFileName(1_000L)
        val b = PhotoNaming.jpegFileName(2_000L)
        assertNotEquals(a, b)
    }
}
