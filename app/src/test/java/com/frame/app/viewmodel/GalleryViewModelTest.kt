package com.frame.app.viewmodel

import com.frame.app.domain.gallery.PhotoItem
import com.frame.app.domain.gallery.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeRepo(var photos: List<PhotoItem>) : PhotoRepository {
        override suspend fun loadPhotos(): List<PhotoItem> = photos
    }

    private fun photo(id: Long) = PhotoItem(id, "content://media/$id", id)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun initialState_notLoadedAndEmpty() {
        val vm = GalleryViewModel(FakeRepo(emptyList()))
        assertFalse(vm.state.value.isLoaded)
        assertNull(vm.state.value.latest)
    }

    @Test
    fun refresh_loadsPhotosAndExposesLatest() = runTest(dispatcher) {
        val vm = GalleryViewModel(FakeRepo(listOf(photo(3), photo(2), photo(1))))
        vm.refresh()
        advanceUntilIdle()
        assertTrue(vm.state.value.isLoaded)
        assertEquals(3, vm.state.value.photos.size)
        assertEquals(3L, vm.state.value.latest?.id)
    }

    @Test
    fun refresh_picksUpNewPhotos() = runTest(dispatcher) {
        val repo = FakeRepo(listOf(photo(1)))
        val vm = GalleryViewModel(repo)
        vm.refresh(); advanceUntilIdle()
        repo.photos = listOf(photo(2), photo(1))
        vm.refresh(); advanceUntilIdle()
        assertEquals(2L, vm.state.value.latest?.id)
    }

    @Test
    fun emptyLibrary_isLoadedWithNoLatest() = runTest(dispatcher) {
        val vm = GalleryViewModel(FakeRepo(emptyList()))
        vm.refresh(); advanceUntilIdle()
        assertTrue(vm.state.value.isLoaded)
        assertNull(vm.state.value.latest)
    }
}
