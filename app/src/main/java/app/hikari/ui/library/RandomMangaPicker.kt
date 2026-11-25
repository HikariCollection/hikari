package app.hikari.ui.library

import androidx.compose.runtime.Immutable
import kotlin.random.Random

@Immutable
data class RandomMangaFilter(
    val unreadOnly: Boolean = false,
    val startedOnly: Boolean = false,
    val allCategories: Boolean = false,
)

internal fun filterRandomCandidates(
    items: List<LibraryItem>,
    filter: RandomMangaFilter,
): List<LibraryItem> {
    return items
        .asSequence()
        .distinctBy { it.id }
        .filter { item -> !filter.unreadOnly || item.libraryManga.unreadCount > 0L }
        .filter { item -> !filter.startedOnly || item.libraryManga.hasStarted }
        .toList()
}

internal fun pickRandomCandidate(
    candidates: List<LibraryItem>,
    excludeMangaId: Long? = null,
    random: Random = Random.Default,
): LibraryItem? {
    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates.first()

    val pool = if (excludeMangaId != null) {
        candidates.filterNot { it.id == excludeMangaId }.ifEmpty { candidates }
    } else {
        candidates
    }
    return pool.random(random)
}
