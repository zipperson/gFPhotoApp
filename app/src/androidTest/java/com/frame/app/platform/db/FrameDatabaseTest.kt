package com.frame.app.platform.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frame.app.domain.model.CompositionStyle
import com.frame.app.domain.model.ReferenceProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test exercising the Room scaffold end to end on an in-memory DB:
 * the schema builds, and the ReferenceLibrary save/get/observe/delete round-trip
 * works. Proves the Phase 6 persistence surface is sound even though the app
 * writes nothing in Phase 1.
 */
@RunWith(AndroidJUnit4::class)
class FrameDatabaseTest {

    private lateinit var db: FrameDatabase
    private lateinit var library: RoomReferenceLibrary

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FrameDatabase::class.java).build()
        library = RoomReferenceLibrary(db.referenceDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun saveAndGet_roundTrips() = runBlocking {
        val profile = ReferenceProfile(
            id = "a",
            displayName = "Test",
            subjectBox = null,
            subjectSizeRatio = 0.4f,
            cameraAngleDegrees = null,
            horizonY = null,
            headroomRatio = null,
            negativeSpaceRatio = null,
            aspectRatio = 0.75f,
            style = CompositionStyle.SYMMETRICAL,
            tags = listOf("architecture"),
        )
        library.save(profile)

        val loaded = library.get("a")
        assertEquals("Test", loaded?.displayName)
        assertEquals(CompositionStyle.SYMMETRICAL, loaded?.style)
        assertEquals(listOf("architecture"), loaded?.tags)

        val all = library.observeAll().first()
        assertEquals(1, all.size)

        library.delete("a")
        assertNull(library.get("a"))
    }
}
