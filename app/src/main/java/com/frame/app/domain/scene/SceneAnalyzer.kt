package com.frame.app.domain.scene

/**
 * Analyzes a single captured frame and returns higher-level photographic advice.
 *
 * Provider-agnostic by design: the Gemini cloud implementation lives in the
 * platform layer, but the app depends only on this interface — so the provider
 * can be swapped (or the feature gated behind a paywall) without touching
 * callers. Suspending + one-shot; called on demand, off the main thread, never
 * per frame.
 */
interface SceneAnalyzer {

    /**
     * @param jpegBytes a single frame encoded as JPEG (already downscaled for a
     *        small, cheap upload)
     * @param context   the current style + rule-engine summary, so the advice
     *        complements the live guidance
     */
    suspend fun analyze(jpegBytes: ByteArray, context: SceneContext): SceneAnalysisResult
}

/**
 * Test/offline placeholder returning canned advice. Lets the whole Phase 5
 * pipeline (trigger, state, UI) be built and verified before/without the real
 * Gemini backend or a network.
 */
class FakeSceneAnalyzer(
    private val canned: SceneAdvice = SceneAdvice(
        cameraMovement = "Crouch a little and step back so her full body fits with room above.",
        lighting = "Light is a touch harsh from the left — turn her slightly toward it.",
        background = "Lots of empty wall on the right; shift left to use the doorway as a frame.",
        overall = "Strong aesthetic full-body setup — lower the phone for taller proportions.",
        suggestedTarget = SuggestedTarget(0.20f, 0.30f, 0.45f, 0.95f),
        rawText = "Canned advice (FakeSceneAnalyzer).",
    ),
) : SceneAnalyzer {
    override suspend fun analyze(jpegBytes: ByteArray, context: SceneContext): SceneAnalysisResult =
        SceneAnalysisResult.Success(canned)
}
