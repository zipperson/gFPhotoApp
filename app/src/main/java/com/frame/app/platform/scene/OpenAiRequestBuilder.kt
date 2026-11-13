package com.frame.app.platform.scene

import com.frame.app.domain.scene.SceneContext
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Builds the OpenAI chat-completions request body for scene analysis. Pure
 * (string in / string out), so the prompt + payload shape are unit-testable
 * without a network.
 */
object OpenAiRequestBuilder {

    private const val MODEL = "gpt-4o-mini"

    /**
     * @param base64Jpeg base64 (no-wrap) of the downscaled frame
     * @param context    style + rule-engine summary to focus the advice
     */
    fun build(base64Jpeg: String, context: SceneContext): String {
        val prompt = buildPrompt(context)
        val dataUrl = "data:image/jpeg;base64,$base64Jpeg"

        val json = buildJsonObject {
            put("model", MODEL)
            put("max_tokens", 400)
            put("temperature", 0.4)
            // Ask for strict JSON so parsing is reliable.
            putJsonObject("response_format") { put("type", "json_object") }
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", SYSTEM)
                }
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") { put("url", dataUrl) }
                        }
                    }
                }
            }
        }
        return json.toString()
    }

    private fun buildPrompt(context: SceneContext): String = """
        The user is composing a photo in the "${context.styleName}" style
        (${context.styleTagline}). Our on-device analysis currently reads:
        ${context.ruleSummary}.

        STYLE GUIDE for "${context.styleName}": ${context.styleBrief}
        Your advice and the target box MUST follow this style guide.

        Look at the image and give concise, practical guidance to improve this
        exact shot for that style. Focus on how to MOVE THE CAMERA/PHONE and the
        scene — not editing. Call out lighting and background issues.

        Respond with a JSON object with these fields:
        - "cameraMovement": (string) the single most important camera/positioning change
        - "lighting": (string) one lighting observation + fix
        - "background": (string) one background/environment observation + fix
        - "overall": (string) a one-line overall tip
        - "target": (object) where the SUBJECT should be placed in the final frame,
          as normalized coordinates 0..1 with keys "left","top","right","bottom"
          (0,0 = top-left of the image; 1,1 = bottom-right). Give your best
          estimate of the ideal subject box for this style.
        Keep each text field to one short sentence, friendly and specific.
    """.trimIndent()

    private const val SYSTEM =
        "You are a professional photographer giving quick, practical framing and " +
            "lighting coaching for phone photos. Be concise and encouraging."
}
