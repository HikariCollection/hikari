package app.hikari.data.library

import app.hikari.domain.manga.model.Manga
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SkippedUpdatesFormatterTest {

    private fun testManga(title: String) = Manga.create().copy(title = title)

    @Test
    fun `returns empty string when skipped list is empty`() {
        assertEquals("", formatSkippedUpdates(emptyList()))
    }

    @Test
    fun `groups skipped manga by reason and sorts titles within each reason`() {
        val skipped = listOf(
            testManga("Zeta") to "Completed",
            testManga("Beta") to "Unread",
            testManga("Alpha") to "Completed",
            testManga("Delta") to "Unread",
        )

        val expected = """
            ! Completed
              - Alpha
              - Zeta

            ! Unread
              - Beta
              - Delta
        """.trimIndent() + "\n"

        assertEquals(expected, formatSkippedUpdates(skipped))
    }

    @Test
    fun `handles null reason without throwing`() {
        val skipped = listOf(
            testManga("Gamma") to null,
            testManga("Alpha") to null,
        )

        val expected = """
            !
              - Alpha
              - Gamma
        """.trimIndent() + "\n"

        assertEquals(expected, formatSkippedUpdates(skipped))
    }
}
