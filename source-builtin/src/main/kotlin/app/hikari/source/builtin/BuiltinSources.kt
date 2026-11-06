package app.hikari.source.builtin

import app.hikari.domain.extension.model.ContentWarning
import app.hikari.extension.model.Extension
import app.hikari.source.CatalogueSource
import app.hikari.source.builtin.en.arenascans.ArenaScans
import app.hikari.source.builtin.id.mgkomik.MGKomik
import app.hikari.source.builtin.id.shinigami.Shinigami
import app.hikari.source.builtin.id.wurmz.Wurmz

object BuiltinSources {
    val shinigami by lazy { Shinigami() }
    val wurmz by lazy { Wurmz() }
    val mgKomik by lazy { MGKomik() }
    val arenaScans by lazy { ArenaScans() }

    val all: List<CatalogueSource> by lazy {
        listOf(
            shinigami,
            wurmz,
            mgKomik,
            arenaScans,
        )
    }

    val extensions: List<Extension.Loaded> by lazy {
        listOf(
            Extension.Loaded(
                name = shinigami.name,
                pkgName = "app.hikari.extension.id.shinigami",
                versionName = "1.1.1",
                versionCode = 1L,
                libVersion = 1.6,
                lang = shinigami.lang,
                contentWarning = ContentWarning.SAFE,
                isShared = false,
                signatures = emptyList(),
                pkgFactory = null,
                sources = listOf(shinigami),
            ),
            Extension.Loaded(
                name = wurmz.name,
                pkgName = "app.hikari.extension.id.wurmz",
                versionName = "1.1.1",
                versionCode = 1L,
                libVersion = 1.6,
                lang = wurmz.lang,
                contentWarning = ContentWarning.MIXED,
                isShared = false,
                signatures = emptyList(),
                pkgFactory = null,
                sources = listOf(wurmz),
            ),
            Extension.Loaded(
                name = mgKomik.name,
                pkgName = "app.hikari.extension.id.mgkomik",
                versionName = "1.1.1",
                versionCode = 1L,
                libVersion = 1.6,
                lang = mgKomik.lang,
                contentWarning = ContentWarning.SAFE,
                isShared = false,
                signatures = emptyList(),
                pkgFactory = null,
                sources = listOf(mgKomik),
            ),
            Extension.Loaded(
                name = arenaScans.name,
                pkgName = "app.hikari.extension.en.arenascans",
                versionName = "1.1.1",
                versionCode = 1L,
                libVersion = 1.6,
                lang = arenaScans.lang,
                contentWarning = ContentWarning.MIXED,
                isShared = false,
                signatures = emptyList(),
                pkgFactory = null,
                sources = listOf(arenaScans),
            ),
        )
    }
}
