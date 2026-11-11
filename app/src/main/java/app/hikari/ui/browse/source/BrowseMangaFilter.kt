package app.hikari.ui.browse.source

internal fun shouldShowBrowseManga(
    isFavorite: Boolean,
    url: String,
    hideInLibraryItems: Boolean,
    hiddenUrls: Set<String>,
    showHiddenItems: Boolean,
): Boolean {
    if (hideInLibraryItems && isFavorite) return false
    if (!showHiddenItems && url in hiddenUrls) return false
    return true
}

internal fun shouldShowGlobalSearchManga(
    sourceId: Long,
    url: String,
    hiddenKeys: Set<Pair<Long, String>>,
    showHiddenItems: Boolean,
): Boolean {
    return showHiddenItems || (sourceId to url) !in hiddenKeys
}
