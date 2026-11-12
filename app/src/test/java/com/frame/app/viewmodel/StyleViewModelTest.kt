package com.frame.app.viewmodel

import app.cash.turbine.test
import com.frame.app.domain.model.PhotoStyle
import com.frame.app.platform.prefs.StylePreferencesStore
import com.frame.app.platform.prefs.StylePrefsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StyleViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeStore : StylePreferencesStore {
        private val _state = MutableStateFlow(
            StylePrefsState(PhotoStyle.DEFAULT, hasChosen = false)
        )
        override val state: Flow<StylePrefsState> = _state
        override suspend fun setStyle(style: PhotoStyle) {
            _state.value = StylePrefsState(style, hasChosen = true)
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun initialState_defaultAndNotChosen() {
        val vm = StyleViewModel(FakeStore())
        val s = vm.state.value
        assertEquals(PhotoStyle.DEFAULT, s.style)
        assertFalse(s.hasChosen)
    }

    @Test
    fun setStyle_persistsAndMarksChosen() = runTest(dispatcher) {
        val vm = StyleViewModel(FakeStore())
        vm.state.test {
            assertFalse(awaitItem().hasChosen) // initial
            vm.setStyle(PhotoStyle.LOW_ANGLE)
            val next = awaitItem()
            assertEquals(PhotoStyle.LOW_ANGLE, next.style)
            assertTrue(next.hasChosen)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
