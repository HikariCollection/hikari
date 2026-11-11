package app.hikari.ui.browse.source

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrowseMangaFilterTest {

    @Test
    fun `shouldShowBrowseManga hides hidden urls unless showHiddenItems is enabled`() {
        val hiddenUrls = setOf("/manga/hidden-one", "/manga/hidden-two")

        assertFalse(
            shouldShowBrowseManga(
                isFavorite = false,
                url = "/manga/hidden-one",
                hideInLibraryItems = false,
                hiddenUrls = hiddenUrls,
                showHiddenItems = false,
            ),
        )
        assertTrue(
            shouldShowBrowseManga(
                isFavorite = false,
                url = "/manga/hidden-one",
                hideInLibraryItems = false,
                hiddenUrls = hiddenUrls,
                showHiddenItems = true,
            ),
        )
        assertTrue(
            shouldShowBrowseManga(
                isFavorite = false,
                url = "/manga/visible",
                hideInLibraryItems = false,
                hiddenUrls = hiddenUrls,
                showHiddenItems = false,
            ),
        )
    }

    @Test
    fun `shouldShowBrowseManga still honors hideInLibraryItems even when showHiddenItems is true`() {
        val hiddenUrls = setOf("/manga/hidden-fav")

        assertFalse(
            shouldShowBrowseManga(
                isFavorite = true,
                url = "/manga/hidden-fav",
                hideInLibraryItems = true,
                hiddenUrls = hiddenUrls,
                showHiddenItems = true,
            ),
        )
        assertTrue(
            shouldShowBrowseManga(
                isFavorite = true,
                url = "/manga/hidden-fav",
                hideInLibraryItems = false,
                hiddenUrls = hiddenUrls,
                showHiddenItems = true,
            ),
        )
    }

    @Test
    fun `shouldShowGlobalSearchManga matches sourceId and url pair`() {
        val hiddenKeys = setOf(10L to "/manga/a", 20L to "/manga/b")

        assertFalse(
            shouldShowGlobalSearchManga(
                sourceId = 10L,
                url = "/manga/a",
                hiddenKeys = hiddenKeys,
                showHiddenItems = false,
            ),
        )
        assertTrue(
            shouldShowGlobalSearchManga(
                sourceId = 20L,
                url = "/manga/a",
                hiddenKeys = hiddenKeys,
                showHiddenItems = false,
            ),
        )
        assertTrue(
            shouldShowGlobalSearchManga(
                sourceId = 10L,
                url = "/manga/a",
                hiddenKeys = hiddenKeys,
                showHiddenItems = true,
            ),
        )
    }
}
