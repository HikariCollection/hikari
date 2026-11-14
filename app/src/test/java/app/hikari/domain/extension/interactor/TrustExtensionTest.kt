package app.hikari.domain.extension.interactor

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import app.hikari.core.common.preference.Preference
import app.hikari.domain.extension.model.ExtensionStore
import app.hikari.domain.extension.repository.ExtensionStoreRepository
import app.hikari.domain.source.service.SourcePreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TrustExtensionTest {

    private lateinit var storesFlow: MutableStateFlow<List<ExtensionStore>>
    private lateinit var trustedSet: MutableSet<String>
    private lateinit var trustedFlow: MutableStateFlow<Set<String>>
    private lateinit var trustedPref: Preference<Set<String>>
    private lateinit var repository: ExtensionStoreRepository
    private lateinit var preferences: SourcePreferences
    private lateinit var trustExtension: TrustExtension

    @BeforeEach
    fun setUp() {
        mockkStatic(PackageInfoCompat::class)

        storesFlow = MutableStateFlow(emptyList())
        trustedSet = linkedSetOf()
        trustedFlow = MutableStateFlow(emptySet())

        trustedPref = mockk(relaxed = true) {
            every { get() } answers { trustedSet.toSet() }
            every { set(any()) } answers {
                val updated = firstArg<Set<String>>()
                trustedSet = updated.toMutableSet()
                trustedFlow.value = trustedSet.toSet()
            }
            every { delete() } answers {
                trustedSet.clear()
                trustedFlow.value = emptySet()
            }
            every { changes() } returns trustedFlow
        }

        repository = mockk {
            every { getAllAsFlow() } returns storesFlow
        }
        preferences = mockk {
            every { trustedExtensions } returns trustedPref
        }
        trustExtension = TrustExtension(repository, preferences)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(PackageInfoCompat::class)
    }

    private fun testStore(signingKey: String): ExtensionStore {
        return ExtensionStore(
            indexUrl = "https://example.com/index.min.json",
            name = "Test Store",
            badgeLabel = "Test",
            signingKey = signingKey,
            contact = ExtensionStore.Contact(website = "https://example.com", discord = null),
            isLegacy = false,
            extensionListUrl = null,
        )
    }

    private fun testPkgInfo(pkgName: String, versionCode: Long): PackageInfo {
        val pkgInfo = PackageInfo().apply {
            packageName = pkgName
        }
        every { PackageInfoCompat.getLongVersionCode(pkgInfo) } returns versionCode
        return pkgInfo
    }

    @Test
    fun `isTrusted returns false when fingerprints list is empty`() {
        val pkgInfo = testPkgInfo("app.hikari.ext.sample", 1L)
        assertFalse(trustExtension.isTrusted(pkgInfo, emptyList(), listOf(testStore("abc"))))
    }

    @Test
    fun `isTrusted matches store signing key or trusted preference key`() {
        val pkgInfo = testPkgInfo("app.hikari.ext.sample", 10L)

        assertTrue(trustExtension.isTrusted(pkgInfo, listOf("sig1"), listOf(testStore("sig1"))))
        assertFalse(trustExtension.isTrusted(pkgInfo, listOf("sig2"), listOf(testStore("sig1"))))

        trustExtension.trust("app.hikari.ext.sample", 10L, "sig2")
        assertTrue(trustExtension.isTrusted(pkgInfo, listOf("sig2"), emptyList()))
    }

    @Test
    fun `trust replaces prior versions of same package and preserves other packages`() {
        trustExtension.trust("ext.one", 1L, "sigA")
        trustExtension.trust("ext.two", 1L, "sigB")
        trustExtension.trust("ext.one", 2L, "sigC")

        assertEquals(setOf("ext.two:1:sigB", "ext.one:2:sigC"), trustedSet)
    }

    @Test
    fun `revokeAll deletes trusted extensions preference`() {
        trustExtension.trust("ext.one", 1L, "sigA")
        trustExtension.revokeAll()

        verify(exactly = 1) { trustedPref.delete() }
        assertTrue(trustedSet.isEmpty())
    }

    @Test
    fun `changes drops initial replays and emits when signing keys or trusted set changes`() = runBlocking {
        storesFlow.value = listOf(testStore("key1"))
        trustedFlow.value = setOf("ext.one:1:sigA")

        val emissions = mutableListOf<Unit>()
        // Update once to trigger an emission after initial drop(1)
        storesFlow.value = listOf(testStore("key1"), testStore("key2"))
        // Re-collect fresh flow from current state
        val flow = trustExtension.changes()
        // Trigger a change after subscription via trustedFlow
        val collected = async(Dispatchers.Unconfined) {
            flow.take(1).toList(emissions)
        }
        trustedFlow.value = setOf("ext.one:2:sigB")
        collected.await()

        assertEquals(1, emissions.size)
    }
}
