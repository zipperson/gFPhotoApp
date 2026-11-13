package com.frame.app.platform.scene

import com.frame.app.domain.scene.SceneAnalysisResult
import com.frame.app.domain.scene.SceneAnalyzer
import com.frame.app.domain.scene.SceneContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * [SceneAnalyzer] backed by OpenAI's vision-capable chat completions API
 * (gpt-4o-mini). Sends one downscaled JPEG as a base64 data URL plus a prompt
 * that asks for concise, actionable photographic guidance, and parses the
 * model's reply into a [com.frame.app.domain.scene.SceneAdvice].
 *
 * Provider detail lives here only; the app depends on [SceneAnalyzer]. The key
 * is injected (from BuildConfig, sourced from gitignored local.properties) and
 * never hard-coded. An empty key is treated as "unavailable".
 *
 * Networking is off the main thread (Dispatchers.IO). Offline/timeout/HTTP
 * errors map to [SceneAnalysisResult] rather than throwing into the UI.
 */
class OpenAiSceneAnalyzer(
    private val apiKey: String,
    private val client: OkHttpClient = defaultClient(),
    private val parser: OpenAiResponseParser = OpenAiResponseParser(),
) : SceneAnalyzer {

    override suspend fun analyze(
        jpegBytes: ByteArray,
        context: SceneContext,
    ): SceneAnalysisResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext SceneAnalysisResult.Error(
                "AI advice isn't configured. Add an OpenAI API key to enable it."
            )
        }

        // java.util.Base64 is available on API 26+ (our minSdk), avoiding any
        // android.util.Base64 classpath ambiguity.
        val base64 = java.util.Base64.getEncoder().encodeToString(jpegBytes)
        val body = OpenAiRequestBuilder.build(base64, context)

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext SceneAnalysisResult.Error(
                        "AI request failed (${response.code}). Try again."
                    )
                }
                val advice = parser.parse(raw)
                    ?: return@withContext SceneAnalysisResult.Error("Couldn't read the AI response.")
                SceneAnalysisResult.Success(advice)
            }
        } catch (e: IOException) {
            // No network / DNS / timeout: treat as offline so the UI can say the
            // app still works without it.
            SceneAnalysisResult.Offline
        } catch (e: Exception) {
            SceneAnalysisResult.Error(e.message ?: "Unexpected error")
        }
    }

    private companion object {
        const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
        val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
