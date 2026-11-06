package app.hikari.source.builtin.en.arenascans

import app.hikari.source.builtin.core.MangaThemesia

class ArenaScans : MangaThemesia() {
    override val name: String = "Arena Scans"
    override val lang: String = "en"
    override val baseUrl: String = "https://arenascan.com"

    override val supportsLatest: Boolean = true
}
