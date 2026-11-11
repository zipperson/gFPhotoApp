package com.frame.app.viewmodel

import app.cash.turbine.test
import com.frame.app.domain.model.CameraMode
import com.frame.app.domain.service.OverlaySettings
import com.frame.app.platform.prefs.OverlayPreferencesStore
import com.frame.app.platform.prefs.OverlayPrefsState
import com.frame.app.platform.prefs.toPreset
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
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for OverlayViewModel using an in-memory fake store. No Android /
 * DataStore needed because the ViewModel depends on OverlayPreferencesStore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OverlayViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** In-memory store mimicking DataStore semantics (mode reset -> preset). */
    private class FakeStore : OverlayPreferencesStore {
        private val _state = MutableStateFlow(
            OverlayPrefsState(CameraMode.GUIDED, CameraMode.GUIDED.toPreset())
        )
        override val state: Flow<OverlayPrefsState> = _state

        override suspend fun setMode(mode: CameraMode) {
            _state.value = OverlayPrefsState(mode, mode.toPreset())
        }

        override suspend fun setSettings(settings: OverlaySettings) {
            _state.value = _state.value.copy(settings = settings)
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun toggleMode_switchesGuidedToMinimalAndResetsPreset() = runTest(dispatcher) {
        val vm = OverlayViewModel(FakeStore())
        vm.state.test {
            assertEquals(CameraMode.GUIDED, awaitItem().mode)

            vm.toggleMode()
            val minimal = awaitItem()
            assertEquals(CameraMode.MINIMAL, minimal.mode)
            // Minimal preset hides the crosshair.
            assertEquals(false, minimal.settings.showCenterCrosshair)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setOpacity_persistsThroughStore() = runTest(dispatcher) {
        val vm = OverlayViewModel(FakeStore())
        vm.state.test {
            awaitItem() // initial
            vm.setOpacity(0.25f)
            assertEquals(0.25f, awaitItem().settings.opacity, 1e-4f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateSettings_togglesIndividualGuide() = runTest(dispatcher) {
        val vm = OverlayViewModel(FakeStore())
        vm.state.test {
            val initial = awaitItem()
            val wasThirds = initial.settings.showThirds
            vm.updateSettings { it.copy(showThirds = !wasThirds) }
            assertEquals(!wasThirds, awaitItem().settings.showThirds)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
