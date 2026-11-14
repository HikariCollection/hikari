package app.hikari.extension.util

import app.hikari.domain.extension.model.ContentWarning
import app.hikari.extension.model.Extension
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExtensionLoaderValidationTest {

    @Test
    fun `stripExtensionTitlePrefix removes Hikari and legacy Tachiyomi prefixes`() {
        assertEquals("MangaDex", ExtensionLoader.stripExtensionTitlePrefix("Hikari: MangaDex"))
        assertEquals("MangaDex", ExtensionLoader.stripExtensionTitlePrefix("Tachiyomi: MangaDex"))
        assertEquals("Custom Source", ExtensionLoader.stripExtensionTitlePrefix("Custom Source"))
    }

    @Test
    fun `resolveContentWarning maps explicit metadata and legacy nsfw flag`() {
        assertEquals(
            ContentWarning.SAFE,
            ExtensionLoader.resolveContentWarning(
                hasMetadata = false,
                hasExplicitContentWarning = true,
                explicitLevel = 2,
                legacyNsfwFlag = 1,
            ),
        )
        assertEquals(
            ContentWarning.MIXED,
            ExtensionLoader.resolveContentWarning(
                hasMetadata = true,
                hasExplicitContentWarning = true,
                explicitLevel = 1,
                legacyNsfwFlag = 0,
            ),
        )
        assertEquals(
            ContentWarning.NSFW,
            ExtensionLoader.resolveContentWarning(
                hasMetadata = true,
                hasExplicitContentWarning = true,
                explicitLevel = 2,
                legacyNsfwFlag = 0,
            ),
        )
        assertEquals(
            ContentWarning.NSFW,
            ExtensionLoader.resolveContentWarning(
                hasMetadata = true,
                hasExplicitContentWarning = false,
                explicitLevel = null,
                legacyNsfwFlag = 1,
            ),
        )
    }

    @Test
    fun `resolveExtensionLibVersion prefers metadata float and falls back to versionName`() {
        assertEquals(1.1, ExtensionLoader.resolveExtensionLibVersion(1.1f, "2.0.1"))
        assertEquals(1.1, ExtensionLoader.resolveExtensionLibVersion(0.0f, "1.1.4"))
        assertEquals(1.1, ExtensionLoader.resolveExtensionLibVersion(null, "1.1.0"))
        assertNull(ExtensionLoader.resolveExtensionLibVersion(null, "invalid"))
    }

    @Test
    fun `validateExtensionCandidate enforces malformed, libVersion, signature, trust, and content warning order`() {
        assertEquals(
            Extension.NotLoaded.Reason.Malformed,
            ExtensionLoader.validateExtensionCandidate(
                hasAppAndMetadata = false,
                versionName = "1.1.0",
                libVersion = 1.1,
                signatures = listOf("sig"),
                isTrusted = { true },
                contentWarning = ContentWarning.SAFE,
                enabledContentWarnings = setOf(ContentWarning.SAFE),
                applyContentWarningsToInstalled = true,
            ),
        )
        assertEquals(
            Extension.NotLoaded.Reason.UnsupportedLibVersion,
            ExtensionLoader.validateExtensionCandidate(
                hasAppAndMetadata = true,
                versionName = "1.5.0",
                libVersion = 1.5,
                signatures = emptyList(),
                isTrusted = { false },
                contentWarning = ContentWarning.SAFE,
                enabledContentWarnings = setOf(ContentWarning.SAFE),
                applyContentWarningsToInstalled = true,
            ),
        )
        assertEquals(
            Extension.NotLoaded.Reason.Unsigned,
            ExtensionLoader.validateExtensionCandidate(
                hasAppAndMetadata = true,
                versionName = "1.1.0",
                libVersion = 1.1,
                signatures = emptyList(),
                isTrusted = { true },
                contentWarning = ContentWarning.SAFE,
                enabledContentWarnings = setOf(ContentWarning.SAFE),
                applyContentWarningsToInstalled = true,
            ),
        )
        assertEquals(
            Extension.NotLoaded.Reason.Untrusted("sig2"),
            ExtensionLoader.validateExtensionCandidate(
                hasAppAndMetadata = true,
                versionName = "1.1.0",
                libVersion = 1.1,
                signatures = listOf("sig1", "sig2"),
                isTrusted = { false },
                contentWarning = ContentWarning.NSFW,
                enabledContentWarnings = emptySet(),
                applyContentWarningsToInstalled = true,
            ),
        )
        assertEquals(
            Extension.NotLoaded.Reason.Filtered,
            ExtensionLoader.validateExtensionCandidate(
                hasAppAndMetadata = true,
                versionName = "1.1.0",
                libVersion = 1.1,
                signatures = listOf("sig1"),
                isTrusted = { true },
                contentWarning = ContentWarning.NSFW,
                enabledContentWarnings = setOf(ContentWarning.SAFE),
                applyContentWarningsToInstalled = true,
            ),
        )
        assertNull(
            ExtensionLoader.validateExtensionCandidate(
                hasAppAndMetadata = true,
                versionName = "1.1.0",
                libVersion = 1.1,
                signatures = listOf("sig1"),
                isTrusted = { true },
                contentWarning = ContentWarning.NSFW,
                enabledContentWarnings = setOf(ContentWarning.SAFE),
                applyContentWarningsToInstalled = false,
            ),
        )
    }

    @Test
    fun `canReuseAlreadyLoaded requires matching versionCode and isShared flag`() {
        val loaded = Extension.Loaded(
            name = "Sample",
            pkgName = "app.hikari.ext.sample",
            versionName = "1.1.0",
            versionCode = 42L,
            libVersion = 1.1,
            lang = "en",
            contentWarning = ContentWarning.SAFE,
            sources = listOf(mockk()),
            pkgFactory = null,
            isShared = true,
            signatures = listOf("sig"),
            store = null,
        )

        assertTrue(ExtensionLoader.canReuseAlreadyLoaded(loaded, versionCode = 42L, isShared = true))
        assertFalse(ExtensionLoader.canReuseAlreadyLoaded(loaded, versionCode = 43L, isShared = true))
        assertFalse(ExtensionLoader.canReuseAlreadyLoaded(loaded, versionCode = 42L, isShared = false))
        assertFalse(ExtensionLoader.canReuseAlreadyLoaded(null, versionCode = 42L, isShared = true))
    }
}
