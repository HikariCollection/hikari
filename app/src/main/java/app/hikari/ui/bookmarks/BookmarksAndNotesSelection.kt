package app.hikari.ui.bookmarks

import app.hikari.domain.bookmark.model.BookmarksAndNotesGroup
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.chapter.model.bookmarkNote

internal fun pruneSelection(
    selectedIds: Set<Long>,
    groups: List<BookmarksAndNotesGroup>,
): Set<Long> {
    if (selectedIds.isEmpty()) return emptySet()
    val visibleIds = groups
        .asSequence()
        .flatMap { it.chapters.asSequence() }
        .map { it.chapter.id }
        .toHashSet()
    return selectedIds.filterTo(HashSet()) { it in visibleIds }
}

internal fun selectAllVisible(groups: List<BookmarksAndNotesGroup>): Set<Long> {
    return groups
        .asSequence()
        .flatMap { it.chapters.asSequence() }
        .mapTo(HashSet()) { it.chapter.id }
}

internal fun invertVisibleSelection(
    selectedIds: Set<Long>,
    groups: List<BookmarksAndNotesGroup>,
): Set<Long> {
    val visibleIds = selectAllVisible(groups)
    return visibleIds.filterNotTo(HashSet()) { it in selectedIds }
}

internal fun requiresNoteClearPrompt(chapters: List<Chapter>): Boolean {
    return chapters.any { !it.bookmarkNote.isNullOrBlank() }
}
