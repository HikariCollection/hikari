package app.hikari.data.backup.restore

import android.content.Context
import app.hikari.core.common.preference.Preference
import app.hikari.core.common.preference.PreferenceStore
import app.hikari.data.backup.models.BackupCategory
import app.hikari.data.backup.models.BackupPreference
import app.hikari.data.backup.models.BooleanPreferenceValue
import app.hikari.data.backup.models.FloatPreferenceValue
import app.hikari.data.backup.models.IntPreferenceValue
import app.hikari.data.backup.models.LongPreferenceValue
import app.hikari.data.backup.models.StringPreferenceValue
import app.hikari.data.backup.models.StringSetPreferenceValue
import app.hikari.data.backup.restore.restorers.PreferenceRestorer
import app.hikari.domain.category.interactor.GetCategories
import app.hikari.domain.category.model.Category
import app.hikari.domain.download.service.DownloadPreferences
import app.hikari.domain.library.service.LibraryPreferences
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PreferenceRestorerTest {

    private val context = mockk<Context>(relaxed = true)
    private val getCategories = mockk<GetCategories>()
    private val preferenceStore = TestPreferenceStore()

    private lateinit var restorer: PreferenceRestorer
    private var scheduledTasksCalled = false

    @BeforeEach
    fun setUp() {
        restorer = PreferenceRestorer(
            context = context,
            getCategories = getCategories,
            preferenceStore = preferenceStore,
        ).apply {
            scheduleTasks = {
                scheduledTasksCalled = true
            }
        }
    }

    @Test
    fun `restores primitive preference types into preferenceStore`() = runTest {
        val preferences = listOf(
            BackupPreference("int_key", IntPreferenceValue(42)),
            BackupPreference("long_key", LongPreferenceValue(123456789L)),
            BackupPreference("float_key", FloatPreferenceValue(3.14f)),
            BackupPreference("string_key", StringPreferenceValue("hikari")),
            BackupPreference("boolean_key", BooleanPreferenceValue(true)),
        )

        restorer.restoreApp(preferences, null)

        preferenceStore.getInt("int_key").get() shouldBe 42
        preferenceStore.getLong("long_key").get() shouldBe 123456789L
        preferenceStore.getFloat("float_key").get() shouldBe 3.14f
        preferenceStore.getString("string_key").get() shouldBe "hikari"
        preferenceStore.getBoolean("boolean_key").get() shouldBe true
        scheduledTasksCalled shouldBe true
    }

    @Test
    fun `safely ignores preference with incompatible type`() = runTest {
        // Pre-populate with String
        preferenceStore.getString("mismatched_key").set("existing_string")

        // Try to restore an Int into it
        val preferences = listOf(
            BackupPreference("mismatched_key", IntPreferenceValue(99)),
        )

        restorer.restoreApp(preferences, null)

        // String value should not have been overwritten or corrupted
        preferenceStore.getString("mismatched_key").get() shouldBe "existing_string"
    }

    @Test
    fun `remaps default category preference from backup id to db category id`() = runTest {
        val dbCategories = listOf(
            Category(id = 15L, name = "Manga", order = 1, flags = 0),
        )
        coEvery { getCategories.await() } returns dbCategories

        val backupCategories = listOf(
            BackupCategory(id = 101L, name = "Manga", order = 1),
        )

        val preferences = listOf(
            BackupPreference(LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY, IntPreferenceValue(101)),
        )

        restorer.restoreApp(preferences, backupCategories)

        preferenceStore.getInt(LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY).get() shouldBe 15
    }

    @Test
    fun `remaps category string set preference keys`() = runTest {
        val dbCategories = listOf(
            Category(id = 25L, name = "Downloads", order = 1, flags = 0),
        )
        coEvery { getCategories.await() } returns dbCategories

        val backupCategories = listOf(
            BackupCategory(id = 202L, name = "Downloads", order = 1),
        )

        val prefKey = DownloadPreferences.categoryPreferenceKeys.first()
        val preferences = listOf(
            BackupPreference(prefKey, StringSetPreferenceValue(setOf("202"))),
        )

        restorer.restoreApp(preferences, backupCategories)

        preferenceStore.getStringSet(prefKey).get() shouldBe setOf("25")
    }

    private class TestPreferenceStore : PreferenceStore {
        val values = mutableMapOf<String, Any?>()

        override fun getString(key: String, defaultValue: String): Preference<String> =
            createPreference(key, defaultValue)

        override fun getLong(key: String, defaultValue: Long): Preference<Long> =
            createPreference(key, defaultValue)

        override fun getInt(key: String, defaultValue: Int): Preference<Int> =
            createPreference(key, defaultValue)

        override fun getFloat(key: String, defaultValue: Float): Preference<Float> =
            createPreference(key, defaultValue)

        override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
            createPreference(key, defaultValue)

        override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
            createPreference(key, defaultValue)

        override fun <T> getObjectFromString(
            key: String,
            defaultValue: T,
            serializer: (T) -> String,
            deserializer: (String) -> T,
        ): Preference<T> = throw UnsupportedOperationException()

        override fun <T> getObjectFromInt(
            key: String,
            defaultValue: T,
            serializer: (T) -> Int,
            deserializer: (Int) -> T,
        ): Preference<T> = throw UnsupportedOperationException()

        override fun <T> getObjectSetFromStringSet(
            key: String,
            defaultValue: Set<T>,
            serializer: (T) -> String,
            deserializer: (String) -> T?,
        ): Preference<Set<T>> = throw UnsupportedOperationException()

        override fun getAll(): Map<String, *> = values

        @Suppress("UNCHECKED_CAST")
        private fun <T> createPreference(key: String, defaultValue: T): Preference<T> {
            return object : Preference<T> {
                override fun key(): String = key
                override fun get(): T = (values[key] as? T) ?: defaultValue
                override fun isSet(): Boolean = key in values
                override fun delete() {
                    values.remove(key)
                }
                override fun defaultValue(): T = defaultValue
                override fun changes(): Flow<T> = flow { emit(get()) }
                override fun stateIn(scope: CoroutineScope): StateFlow<T> =
                    changes().stateIn(scope, SharingStarted.Eagerly, get())
                override fun set(value: T) {
                    values[key] = value
                }
            }
        }
    }
}
