package app.hikari.domain.track.store

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DelayedTrackingStoreTest {

    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val context = mockk<Context>()

    private lateinit var store: DelayedTrackingStore

    @BeforeEach
    fun setUp() {
        every { context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        store = DelayedTrackingStore(context)
    }

    @Test
    fun `add puts float when lastChapterRead is greater than previous`() {
        every { sharedPreferences.getFloat("10", 0f) } returns 3.0f

        store.add(10L, 5.0)

        verify(exactly = 1) { editor.putFloat("10", 5.0f) }
    }

    @Test
    fun `add ignores update when lastChapterRead is less than or equal to previous`() {
        every { sharedPreferences.getFloat("10", 0f) } returns 5.0f

        store.add(10L, 4.0)

        verify(exactly = 0) { editor.putFloat(any(), any()) }
    }

    @Test
    fun `remove removes key from preferences`() {
        store.remove(10L)

        verify(exactly = 1) { editor.remove("10") }
    }

    @Test
    fun `getItems resiliently parses valid items and ignores malformed or non-numeric entries`() {
        every { sharedPreferences.all } returns mapOf(
            "10" to 5.5f,
            "20" to "12.0",
            "not_a_number" to 1.0f,
            "30" to "invalid_float",
        )

        val items = store.getItems()

        assertEquals(2, items.size)
        assertEquals(10L, items[0].trackId)
        assertEquals(5.5f, items[0].lastChapterRead)
        assertEquals(20L, items[1].trackId)
        assertEquals(12.0f, items[1].lastChapterRead)
    }
}
