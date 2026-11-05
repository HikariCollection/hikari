package app.hikari.ui.reader.viewer

import app.hikari.ui.reader.setting.ReaderPreferences

/**
 * Settings every paged viewer uses.
 */
data class PagedSettings(
    val page: Page,
    val navigateToPan: Boolean,
    val navigation: NavigationSettings,
) {

    /**
     * What the pages are made with, so a change remakes them.
     */
    data class Page(
        val imageScaleType: ReaderPreferences.ScaleType,
        val zoomStart: ReaderPreferences.ZoomStartType,
        val cropBorders: Boolean,
        val landscapeZoom: Boolean,
    )
}
