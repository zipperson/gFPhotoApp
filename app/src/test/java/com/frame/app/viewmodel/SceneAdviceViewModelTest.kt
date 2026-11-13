package com.frame.app.viewmodel

import app.cash.turbine.test
import com.frame.app.domain.scene.FakeSceneAnalyzer
import com.frame.app.domain.scene.SceneAdvice
import com.frame.app.domain.scene.SceneAnalysisResult
import com.frame.app.domain.scene.SceneAnalyzer
import com.frame.app.domain.scene.SceneContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SceneAdviceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val context = SceneContext("Aesthetic Full-Body", "TikTok look", "FAIR, 60")

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun online_success_emitsReadySuccess() = runTest(dispatcher) {
        val vm = SceneAdviceViewModel(
            analyzer = FakeSceneAnalyzer(),
            captureFrame = { ByteArray(4) },
            isOnline = { true },
        )
        vm.requestAdvice(context)
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is SceneAdviceUiState.Ready)
        assertTrue((state as SceneAdviceUiState.Ready).result is SceneAnalysisResult.Success)
    }

    @Test
    fun offline_shortCircuitsToOffline_withoutCapturing() = runTest(dispatcher) {
        var captured = false
        val vm = SceneAdviceViewModel(
            analyzer = FakeSceneAnalyzer(),
            captureFrame = { captured = true; ByteArray(4) },
            isOnline = { false },
        )
        vm.requestAdvice(context)
        advanceUntilIdle()
        val result = (vm.state.value as SceneAdviceUiState.Ready).result
        assertTrue(result is SceneAnalysisResult.Offline)
        assertTrue("should not capture when offline", !captured)
    }

    @Test
    fun captureFails_emitsError() = runTest(dispatcher) {
        val vm = SceneAdviceViewModel(
            analyzer = FakeSceneAnalyzer(),
            captureFrame = { null },
            isOnline = { true },
        )
        vm.requestAdvice(context)
        advanceUntilIdle()
        val result = (vm.state.value as SceneAdviceUiState.Ready).result
        assertTrue(result is SceneAnalysisResult.Error)
    }

    @Test
    fun dismiss_returnsToIdle() = runTest(dispatcher) {
        val vm = SceneAdviceViewModel(
            analyzer = FakeSceneAnalyzer(),
            captureFrame = { ByteArray(4) },
            isOnline = { true },
        )
        vm.requestAdvice(context)
        advanceUntilIdle()
        vm.dismiss()
        assertTrue(vm.state.value is SceneAdviceUiState.Idle)
    }

    @Test
    fun analyzerError_propagatesAsReadyError() = runTest(dispatcher) {
        val failing = object : SceneAnalyzer {
            override suspend fun analyze(jpegBytes: ByteArray, context: SceneContext) =
                SceneAnalysisResult.Error("nope")
        }
        val vm = SceneAdviceViewModel(failing, { ByteArray(4) }, { true })
        vm.requestAdvice(context)
        advanceUntilIdle()
        val result = (vm.state.value as SceneAdviceUiState.Ready).result
        assertTrue(result is SceneAnalysisResult.Error)
    }
}
