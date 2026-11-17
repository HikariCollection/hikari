package app.hikari.core.common.extensions

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

class JsonObjectTest {

    @Test
    fun `empty byte array returns JsonObjectEmpty`() {
        val bytes = byteArrayOf()
        bytes.toJsonObject() shouldBe JsonObjectEmpty
    }

    @Test
    fun `malformed byte array returns JsonObjectEmpty without throwing`() {
        val malformedBytes = "{ invalid json".encodeToByteArray()
        malformedBytes.toJsonObject() shouldBe JsonObjectEmpty

        val truncatedBytes = """{"key":""".encodeToByteArray()
        truncatedBytes.toJsonObject() shouldBe JsonObjectEmpty

        val randomBytes = byteArrayOf(0x00, 0x01, 0x02, 0x7F)
        randomBytes.toJsonObject() shouldBe JsonObjectEmpty
    }

    @Test
    fun `valid JSON bytes decode correctly`() {
        val jsonString = """{"key":"value","count":42}"""
        val jsonObject = jsonString.encodeToByteArray().toJsonObject()

        jsonObject["key"]?.jsonPrimitive?.content shouldBe "value"
        jsonObject["count"]?.jsonPrimitive?.content shouldBe "42"
    }

    @Test
    fun `round trip serialization preserves object`() {
        val original = buildJsonObject {
            put("title", "Hikari")
            put("unread", true)
        }

        val roundTripped = original.toByteArray().toJsonObject()
        roundTripped shouldBe original
    }
}
