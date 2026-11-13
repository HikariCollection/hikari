package app.hikari.data.backup

import app.hikari.data.backup.create.BackupOptions
import app.hikari.data.backup.restore.RestoreOptions
import app.hikari.i18n.MR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackupOptionsTest {

    @Test
    fun `BackupOptions defaults hiddenManga to true and round-trips boolean array`() {
        val defaultOptions = BackupOptions()
        assertTrue(defaultOptions.hiddenManga)

        val disabledHidden = defaultOptions.copy(hiddenManga = false)
        val array = disabledHidden.asBooleanArray()
        val restored = BackupOptions.fromBooleanArray(array)
        assertFalse(restored.hiddenManga)

        // Legacy 10-element array without hiddenManga should default hiddenManga to true
        val legacyArray = BooleanArray(10) { false }
        val restoredFromLegacy = BackupOptions.fromBooleanArray(legacyArray)
        assertTrue(restoredFromLegacy.hiddenManga)

        // canCreate returns true when only hiddenManga is true
        val onlyHidden = BackupOptions(
            libraryEntries = false,
            categories = false,
            chapters = false,
            tracking = false,
            history = false,
            readEntries = false,
            appSettings = false,
            extensionStores = false,
            sourceSettings = false,
            privateSettings = false,
            hiddenManga = true,
        )
        assertTrue(onlyHidden.canCreate())
    }

    @Test
    fun `RestoreOptions defaults hiddenManga to true and round-trips boolean array`() {
        val defaultOptions = RestoreOptions()
        assertTrue(defaultOptions.hiddenManga)

        val disabledHidden = defaultOptions.copy(hiddenManga = false)
        val array = disabledHidden.asBooleanArray()
        val restored = RestoreOptions.fromBooleanArray(array)
        assertFalse(restored.hiddenManga)

        // Legacy 5-element array without hiddenManga should default hiddenManga to true
        val legacyArray = BooleanArray(5) { false }
        val restoredFromLegacy = RestoreOptions.fromBooleanArray(legacyArray)
        assertTrue(restoredFromLegacy.hiddenManga)

        // canRestore returns true when only hiddenManga is true
        val onlyHidden = RestoreOptions(
            libraryEntries = false,
            categories = false,
            appSettings = false,
            extensionStores = false,
            sourceSettings = false,
            hiddenManga = true,
        )
        assertTrue(onlyHidden.canRestore())
    }

    @Test
    fun `library and restore option entries include pref_hidden_manga`() {
        val backupEntry = BackupOptions.libraryOptions.firstOrNull { it.label == MR.strings.pref_hidden_manga }
        assertTrue(backupEntry != null)
        val optionsWithHiddenDisabled = backupEntry!!.setter(BackupOptions(), false)
        assertFalse(backupEntry.getter(optionsWithHiddenDisabled))

        val restoreEntry = RestoreOptions.options.firstOrNull { it.label == MR.strings.pref_hidden_manga }
        assertTrue(restoreEntry != null)
        val restoreWithHiddenDisabled = restoreEntry!!.setter(RestoreOptions(), false)
        assertFalse(restoreEntry.getter(restoreWithHiddenDisabled))
    }
}
