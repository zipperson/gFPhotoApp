package com.frame.app.domain.scene

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure tests for the scene-analysis domain layer: the fake analyzer honors the
 * contract and the result types behave. No Android / network.
 */
class SceneAnalyzerTest {

    private val context = SceneContext(
        styleName = "Aesthetic Full-Body",
        styleTagline = "The TikTok look",
        ruleSummary = "rating FAIR, score 60; subject centered, camera tilted",
    )

    @Test
    fun fakeAnalyzer_returnsSuccessWithAdvice() = runTest {
        val result = FakeSceneAnalyzer().analyze(ByteArray(0), context)
        assertTrue(result is SceneAnalysisResult.Success)
        val advice = (result as SceneAnalysisResult.Success).advice
        assertTrue(advice.cameraMovement!!.isNotBlank())
        assertTrue(advice.rawText.isNotBlank())
    }

    @Test
    fun fakeAnalyzer_returnsProvidedCannedAdvice() = runTest {
        val canned = SceneAdvice(
            cameraMovement = "move left",
            lighting = null,
            background = null,
            overall = "nice",
            rawText = "raw",
        )
        val result = FakeSceneAnalyzer(canned).analyze(ByteArray(0), context)
        assertEquals(canned, (result as SceneAnalysisResult.Success).advice)
    }

    @Test
    fun resultTypes_distinguishStates() {
        val offline: SceneAnalysisResult = SceneAnalysisResult.Offline
        val error: SceneAnalysisResult = SceneAnalysisResult.Error("boom")
        assertTrue(offline is SceneAnalysisResult.Offline)
        assertEquals("boom", (error as SceneAnalysisResult.Error).message)
    }
}
