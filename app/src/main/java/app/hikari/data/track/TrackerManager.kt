package app.hikari.data.track

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import app.hikari.data.track.anilist.Anilist
import app.hikari.data.track.bangumi.Bangumi
import app.hikari.data.track.comick.Comick
import app.hikari.data.track.hikka.Hikka
import app.hikari.data.track.kavita.Kavita
import app.hikari.data.track.kitsu.Kitsu
import app.hikari.data.track.komga.Komga
import app.hikari.data.track.mangabaka.MangaBaka
import app.hikari.data.track.mangaupdates.MangaUpdates
import app.hikari.data.track.myanimelist.MyAnimeList
import app.hikari.data.track.shikimori.Shikimori
import app.hikari.data.track.suwayomi.Suwayomi
import kotlinx.coroutines.flow.combine

@Inject
@SingleIn(AppScope::class)
class TrackerManager {

    val myAnimeList = MyAnimeList(1L)
    val aniList = Anilist(2L)
    val kitsu = Kitsu(3L)
    val shikimori = Shikimori(4L)
    val bangumi = Bangumi(5L)
    val komga = Komga(6L)
    val mangaUpdates = MangaUpdates(7L)
    val kavita = Kavita(8L)
    val suwayomi = Suwayomi(9L)
    val hikka = Hikka(10L)
    val mangaBaka = MangaBaka(11L)
    val comick = Comick(12L)

    val trackers = listOf(
        mangaBaka,
        myAnimeList,
        aniList,
        kitsu,
        shikimori,
        bangumi,
        komga,
        mangaUpdates,
        kavita,
        suwayomi,
        hikka,
        comick,
    )

    fun loggedInTrackers() = trackers.filter { it.isLoggedIn }

    fun loggedInTrackersFlow() = combine(trackers.map { it.isLoggedInFlow }) {
        it.mapIndexedNotNull { index, isLoggedIn ->
            if (isLoggedIn) trackers[index] else null
        }
    }

    fun get(id: Long) = trackers.find { it.id == id }

    fun getAll(ids: Set<Long>) = trackers.filter { it.id in ids }
}
