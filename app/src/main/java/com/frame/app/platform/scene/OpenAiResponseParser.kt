package com.frame.app.platform.scene

import com.frame.app.domain.scene.SceneAdvice
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses an OpenAI chat-completions response into [SceneAdvice]. Pure (string
 * in / advice out), so it's unit-tested against canned API JSON.
 *
 * The response envelope is:
 *   { "choices": [ { "message": { "content": "<json or text>" } } ] }
 * With response_format=json_object, `content` is itself a JSON object with our
 * fields. We parse that; if it's plain text instead, we fall back to using the
 * whole content as the overall tip so nothing is lost.
 */
class OpenAiResponseParser(
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {

    fun parse(responseBody: String): SceneAdvice? {
        val content = extractContent(responseBody) ?: return null
        val trimmed = content.trim()

        // Try to parse the content as our JSON object.
        val inner = runCatching {
            json.parseToJsonElement(trimmed).jsonObject
        }.getOrNull()

        return if (inner != null) {
            SceneAdvice(
                cameraMovement = inner.stringOrNull("cameraMovement"),
                lighting = inner.stringOrNull("lighting"),
                background = inner.stringOrNull("background"),
                overall = inner.stringOrNull("overall"),
                rawText = trimmed,
            )
        } else {
            // Model returned prose; keep it as the overall tip.
            SceneAdvice(
                cameraMovement = null,
                lighting = null,
                background = null,
                overall = trimmed,
                rawText = trimmed,
            )
        }
    }

    /** Pull choices[0].message.content out of the response envelope. */
    private fun extractContent(responseBody: String): String? = runCatching {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val choices = root["choices"]?.jsonArray ?: return null
        val first = choices.firstOrNull()?.jsonObject ?: return null
        first["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
    }.getOrNull()

    private fun kotlinx.serialization.json.JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" }
}
