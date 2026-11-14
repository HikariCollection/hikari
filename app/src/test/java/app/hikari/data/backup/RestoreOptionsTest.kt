package app.hikari.data.backup

import app.hikari.data.backup.restore.RestoreOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RestoreOptionsTest {

    @Test
    fun `defaults updateLibraryAfterRestore to false and round-trips boolean array`() {
        val defaultOptions = RestoreOptions()
        assertFalse(defaultOptions.updateLibraryAfterRestore)

        val enabled = defaultOptions.copy(updateLibraryAfterRestore = true)
        val array = enabled.asBooleanArray()
        assertEquals(7, array.size)

        val restored = RestoreOptions.fromBooleanArray(array)
        assertTrue(restored.updateLibraryAfterRestore)
        assertEquals(enabled, restored)
    }

    @Test
    fun `fromBooleanArray defaults updateLibraryAfterRestore to false for legacy 6-element array`() {
        val legacyArray = BooleanArray(6) { true }
        val restored = RestoreOptions.fromBooleanArray(legacyArray)

        assertTrue(restored.libraryEntries)
        assertTrue(restored.hiddenManga)
        assertFalse(restored.updateLibraryAfterRestore)
    }

    @Test
    fun `canRestore returns false when only updateLibraryAfterRestore is true`() {
        val onlyUpdate = RestoreOptions(
            libraryEntries = false,
            categories = false,
            appSettings = false,
            extensionStores = false,
            sourceSettings = false,
            hiddenManga = false,
            updateLibraryAfterRestore = true,
        )

        assertFalse(onlyUpdate.canRestore())
    }
}
