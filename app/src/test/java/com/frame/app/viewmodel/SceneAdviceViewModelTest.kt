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

    private fun onlineVm(source: suspend () -> ByteArray? = { ByteArray(4) }) =
        SceneAdviceViewModel(FakeSceneAnalyzer(), source, { true })

    @Test
    fun success_setsActiveAdvice() = runTest(dispatcher) {
        val vm = onlineVm()
        vm.requestAdvice(context)
        advanceUntilIdle()
        assertTrue(vm.activeAdvice.value != null)
    }

    @Test
    fun dismiss_keepsActiveAdvice() = runTest(dispatcher) {
        val vm = onlineVm()
        vm.requestAdvice(context)
        advanceUntilIdle()
        vm.dismiss()
        assertTrue(vm.state.value is SceneAdviceUiState.Idle)
        assertTrue("target must survive closing the sheet", vm.activeAdvice.value != null)
    }

    @Test
    fun reopen_showsLastAdviceWithoutNewRequest() = runTest(dispatcher) {
        var captures = 0
        val vm = onlineVm { captures++; ByteArray(4) }
        vm.requestAdvice(context)
        advanceUntilIdle()
        vm.dismiss()
        vm.reopen()

        val state = vm.state.value
        assertTrue(state is SceneAdviceUiState.Ready)
        assertTrue((state as SceneAdviceUiState.Ready).result is SceneAnalysisResult.Success)
        assertTrue("reopen must not capture/analyze again", captures == 1)
    }

    @Test
    fun reopen_withNoAdvice_isNoOp() = runTest(dispatcher) {
        val vm = onlineVm()
        vm.reopen()
        assertTrue(vm.state.value is SceneAdviceUiState.Idle)
    }

    @Test
    fun clearAdvice_removesAdviceAndClosesSheet() = runTest(dispatcher) {
        val vm = onlineVm()
        vm.requestAdvice(context)
        advanceUntilIdle()
        vm.clearAdvice()
        assertTrue(vm.activeAdvice.value == null)
        assertTrue(vm.state.value is SceneAdviceUiState.Idle)
    }

    @Test
    fun failedRequest_doesNotReplaceExistingAdvice() = runTest(dispatcher) {
        var ok = true
        val vm = onlineVm { if (ok) ByteArray(4) else null }
        vm.requestAdvice(context)
        advanceUntilIdle()
        val first = vm.activeAdvice.value
        vm.dismiss()

        ok = false
        vm.requestAdvice(context)
        advanceUntilIdle()
        assertTrue(vm.activeAdvice.value === first)
    }

    @Test
    fun setCaptureSource_replacesStaleSource() = runTest(dispatcher) {
        // Simulates returning from the gallery: the old camera is gone and a
        // new frame source must be used for the next request.
        val vm = onlineVm { null } // stale source: always fails
        vm.setCaptureSource { ByteArray(4) }
        vm.requestAdvice(context)
        advanceUntilIdle()
        val result = (vm.state.value as SceneAdviceUiState.Ready).result
        assertTrue(result is SceneAnalysisResult.Success)
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
