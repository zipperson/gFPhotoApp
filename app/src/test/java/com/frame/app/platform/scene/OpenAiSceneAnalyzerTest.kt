package com.frame.app.platform.scene

import com.frame.app.domain.scene.SceneContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiRequestBuilderTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val context = SceneContext(
        styleName = "Aesthetic Full-Body",
        styleTagline = "The TikTok look",
        ruleSummary = "rating FAIR, score 60; subject centered",
    )

    @Test
    fun build_producesValidJsonWithModelAndImage() {
        val bodyStr = OpenAiRequestBuilder.build("QUJD", context) // base64 of "ABC"
        val body = json.parseToJsonElement(bodyStr).jsonObject

        assertEquals("gpt-4o-mini", body["model"]!!.jsonPrimitive.content)

        val messages = body["messages"]!!.jsonArray
        // system + user
        assertEquals(2, messages.size)

        val userContent = messages[1].jsonObject["content"]!!.jsonArray
        val types = userContent.map { it.jsonObject["type"]!!.jsonPrimitive.content }
        assertTrue(types.contains("text"))
        assertTrue(types.contains("image_url"))

        val imagePart = userContent.first { it.jsonObject["type"]!!.jsonPrimitive.content == "image_url" }
        val url = imagePart.jsonObject["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content
        assertTrue(url.startsWith("data:image/jpeg;base64,QUJD"))
    }

    @Test
    fun build_promptMentionsStyleAndRuleSummary() {
        val bodyStr = OpenAiRequestBuilder.build("QUJD", context)
        val body = json.parseToJsonElement(bodyStr).jsonObject
        val text = body["messages"]!!.jsonArray[1].jsonObject["content"]!!
            .jsonArray.first { it.jsonObject["type"]!!.jsonPrimitive.content == "text" }
            .jsonObject["text"]!!.jsonPrimitive.content
        assertTrue(text.contains("Aesthetic Full-Body"))
        assertTrue(text.contains("rating FAIR, score 60"))
    }

    @Test
    fun build_requestsJsonResponseFormat() {
        val body = json.parseToJsonElement(OpenAiRequestBuilder.build("X", context)).jsonObject
        assertEquals("json_object", body["response_format"]!!.jsonObject["type"]!!.jsonPrimitive.content)
    }
}

class OpenAiResponseParserTest {

    private val parser = OpenAiResponseParser()

    @Test
    fun parse_structuredJsonContent() {
        val response = """
            {"choices":[{"message":{"content":"{\"cameraMovement\":\"Step back\",\"lighting\":\"Turn toward the window\",\"background\":\"Shift left for the doorway\",\"overall\":\"Nice full-body setup\"}"}}]}
        """.trimIndent()
        val advice = parser.parse(response)!!
        assertEquals("Step back", advice.cameraMovement)
        assertEquals("Turn toward the window", advice.lighting)
        assertEquals("Shift left for the doorway", advice.background)
        assertEquals("Nice full-body setup", advice.overall)
    }

    @Test
    fun parse_plainTextContent_fallsBackToOverall() {
        val response = """
            {"choices":[{"message":{"content":"Just step back a little."}}]}
        """.trimIndent()
        val advice = parser.parse(response)!!
        assertNull(advice.cameraMovement)
        assertEquals("Just step back a little.", advice.overall)
    }

    @Test
    fun parse_missingChoices_returnsNull() {
        assertNull(parser.parse("""{"error":"bad"}"""))
    }

    @Test
    fun parse_garbage_returnsNull() {
        assertNull(parser.parse("not json at all"))
    }

    @Test
    fun parse_nullStringFieldsTreatedAsAbsent() {
        val response = """
            {"choices":[{"message":{"content":"{\"cameraMovement\":\"\",\"overall\":\"Good\"}"}}]}
        """.trimIndent()
        val advice = parser.parse(response)!!
        assertNull(advice.cameraMovement) // blank -> null
        assertEquals("Good", advice.overall)
    }
}
