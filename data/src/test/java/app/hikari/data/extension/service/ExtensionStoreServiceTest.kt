package app.hikari.data.extension.service

import app.hikari.data.extension.model.NetworkExtensionStore
import app.hikari.data.extension.model.NetworkLegacyExtensionRepo
import app.hikari.domain.extension.model.ExtensionStore
import app.hikari.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class ExtensionStoreServiceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val protoBuf = ProtoBuf

    private fun createService(routes: Map<String, () -> ByteArray>): ExtensionStoreService {
        val client = mockk<OkHttpClient>()
        every { client.newCall(any()) } answers {
            val request = firstArg<Request>()
            val url = request.url.toString()
            mockk<Call> {
                every { enqueue(any()) } answers {
                    val callback = firstArg<Callback>()
                    val responder = routes[url]
                    if (responder == null) {
                        callback.onFailure(this@mockk, IOException("Unexpected URL: $url"))
                    } else {
                        val bytes = responder()
                        val response = Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(bytes.toResponseBody("application/octet-stream".toMediaType()))
                            .build()
                        callback.onResponse(this@mockk, response)
                    }
                }
                every { cancel() } returns Unit
            }
        }
        val network = mockk<NetworkHelper> {
            every { this@mockk.client } returns client
        }
        return ExtensionStoreService(network, json, protoBuf)
    }

    private fun sampleNetworkStore(): NetworkExtensionStore {
        return NetworkExtensionStore(
            name = "Hikari Official",
            badgeLabel = "Hikari",
            signingKey = "deadbeef",
            contact = NetworkExtensionStore.Contact(website = "https://hikari.app", discord = null),
            extensionList = NetworkExtensionStore.ExtensionList(
                extensions = listOf(
                    NetworkExtensionStore.Extension(
                        name = "Sample",
                        packageName = "app.hikari.ext.sample",
                        resources = NetworkExtensionStore.Resources(
                            apkUrl = "https://hikari.app/ext.apk",
                            iconUrl = "https://hikari.app/ext.png",
                        ),
                        extensionLib = "1.1",
                        versionCode = 1L,
                        versionName = "1.1.1",
                        contentWarning = NetworkExtensionStore.ContentWarning.SAFE,
                        sources = listOf(
                            NetworkExtensionStore.Source(id = 10L, name = "SampleSrc", language = "en"),
                        ),
                    ),
                ),
            ),
            extensionListUrl = null,
        )
    }

    @Test
    fun `fetch parses gzipped protobuf v2 store`() = runBlocking {
        val storePayload = protoBuf.encodeToByteArray(sampleNetworkStore())
        val gzipped = Buffer().apply {
            GzipSink(this).buffer().use { it.write(storePayload) }
        }.readByteArray()

        val service = createService(
            mapOf("https://hikari.app/index.proto.gz" to { gzipped }),
        )

        val result = service.fetch("https://hikari.app/index.proto.gz")
        assertTrue(result.isSuccess)
        val store = result.getOrThrow()
        assertEquals("Hikari Official", store.name)
        assertEquals("deadbeef", store.signingKey)
        assertFalse(store.isLegacy)
    }

    @Test
    fun `fetch rewrites legacy index_min_json to repo_json and follows indexV2 redirect`() = runBlocking {
        val legacyMeta = json.encodeToString(
            NetworkLegacyExtensionRepo.Meta(
                name = "Legacy Repo",
                shortName = null,
                website = "https://legacy.example",
                signingKeyFingerprint = "legacykey",
            ),
        )
        val legacyRepoJson = """{"meta":$legacyMeta,"index_v2":"https://hikari.app/v2/index.json"}"""
        val v2Json = json.encodeToString(sampleNetworkStore())

        val service = createService(
            mapOf(
                "https://legacy.example/index.min.json" to { "[]".toByteArray() },
                "https://legacy.example/repo.json" to { legacyRepoJson.toByteArray() },
                "https://hikari.app/v2/index.json" to { v2Json.toByteArray() },
            ),
        )

        val result = service.fetch("https://legacy.example/index.min.json")
        assertTrue(result.isSuccess)
        val store = result.getOrThrow()
        assertEquals("https://hikari.app/v2/index.json", store.indexUrl)
        assertEquals("Hikari Official", store.name)
    }

    @Test
    fun `fetch fails cleanly when indexV2 forms a redirect cycle`() = runBlocking {
        val loopA = """
            {"meta":{"name":"A","shortName":null,"website":"https://a.example","signingKeyFingerprint":"k1"},
            "index_v2":"https://b.example/repo.json"}
        """.trimIndent()
        val loopB = """
            {"meta":{"name":"B","shortName":null,"website":"https://b.example","signingKeyFingerprint":"k2"},
            "index_v2":"https://a.example/repo.json"}
        """.trimIndent()

        val service = createService(
            mapOf(
                "https://a.example/repo.json" to { loopA.toByteArray() },
                "https://b.example/repo.json" to { loopB.toByteArray() },
            ),
        )

        val result = service.fetch("https://a.example/repo.json")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `getExtensions fails with IllegalStateException when v2 store omits extensionList and extensionListUrl`() =
        runBlocking {
            val incompleteStore = sampleNetworkStore().copy(extensionList = null, extensionListUrl = null)
            val service = createService(
                mapOf("https://hikari.app/index.json" to { json.encodeToString(incompleteStore).toByteArray() }),
            )

            val result = service.getExtensions(
                ExtensionStore(
                    indexUrl = "https://hikari.app/index.json",
                    name = "Hikari Official",
                    badgeLabel = "Hikari",
                    signingKey = "deadbeef",
                    contact = ExtensionStore.Contact(website = "https://hikari.app", discord = null),
                    isLegacy = false,
                    extensionListUrl = null,
                ),
            )
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalStateException)
        }
}
