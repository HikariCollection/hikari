package app.hikari.data.track.shikimori

import app.hikari.data.database.models.Track
import app.hikari.data.track.model.TrackSearch
import app.hikari.graphql.shikimori.ShikimoriGetLibMangaQuery
import app.hikari.graphql.shikimori.ShikimoriGetMangaDetailsQuery
import app.hikari.graphql.shikimori.ShikimoriSearchMangaQuery
import app.hikari.graphql.shikimori.fragment.MangaFragment
import app.hikari.graphql.shikimori.type.MangaKindEnum
import app.hikari.graphql.shikimori.type.MangaStatusEnum
import app.hikari.graphql.shikimori.type.UserRateStatusEnum

internal fun ShikimoriGetLibMangaQuery.Manga.toTrack(trackId: Long): Track {
    val sManga = this
    return Track.create(trackId).apply {
        remote_id = sManga.id.toLong()
        title = sManga.name
        total_chapters = sManga.chapters.toLong()
        tracking_url = sManga.url

        if (sManga.userRate != null) {
            // null if not in user's list, must not throw here because it'd break adding titles
            // throws in the findLibManga method of ShikimoriApi if null and shouldn't be
            library_id = sManga.userRate.id.toLong()
            last_chapter_read = sManga.userRate.chapters.toDouble()
            score = sManga.userRate.score.toDouble()
            status = sManga.userRate.status.toLocalStatus()
        }
    }
}

internal fun ShikimoriGetMangaDetailsQuery.Manga.toTrackSearch(trackId: Long): TrackSearch {
    return mangaFragment.toTrackSearch(trackId)
}

internal fun ShikimoriSearchMangaQuery.Manga.toTrackSearch(trackId: Long): TrackSearch {
    return mangaFragment.toTrackSearch(trackId)
}

private fun MangaFragment.toTrackSearch(trackId: Long): TrackSearch {
    val sManga = this
    return TrackSearch.create(trackId).apply {
        remote_id = sManga.id.toLong()
        title = sManga.name
        total_chapters = sManga.chapters.toLong()
        cover_url = sManga.poster?.mainUrl.orEmpty()
        summary = sManga.description.orEmpty()
        score = sManga.score?.takeIf { it > 0.0 } ?: -1.0
        tracking_url = sManga.url
        publishing_status = sManga.status.toLocalString()
        publishing_type = sManga.kind.toLocalString()
        start_date = sManga.airedOn?.date.orEmpty()
        authors = sManga.personRoles?.filter { it.rolesEn.contains("Story") }?.map { it.person.name } ?: emptyList()
        artists = sManga.personRoles?.filter { it.rolesEn.contains("Art") }?.map { it.person.name } ?: emptyList()
    }
}

private fun MangaKindEnum?.toLocalString() = when (this) {
    MangaKindEnum.manga -> "Manga"
    MangaKindEnum.manhwa -> "Manhwa"
    MangaKindEnum.manhua -> "Manhua"
    MangaKindEnum.one_shot -> "Oneshot"
    MangaKindEnum.doujin -> "Doujin"
    // light novel & novel omitted by query filter, fall back to empty if included
    else -> ""
}

private fun MangaStatusEnum?.toLocalString() = when (this) {
    MangaStatusEnum.anons -> "Planned"
    MangaStatusEnum.ongoing -> "Ongoing"
    MangaStatusEnum.released -> "Released"
    MangaStatusEnum.paused -> "Paused"
    MangaStatusEnum.discontinued -> "Discontinued"
    else -> ""
}

private fun UserRateStatusEnum.toLocalStatus() = when (this) {
    UserRateStatusEnum.planned -> Shikimori.PLAN_TO_READ
    UserRateStatusEnum.watching -> Shikimori.READING
    UserRateStatusEnum.rewatching -> Shikimori.REREADING
    UserRateStatusEnum.completed -> Shikimori.COMPLETED
    UserRateStatusEnum.on_hold -> Shikimori.ON_HOLD
    UserRateStatusEnum.dropped -> Shikimori.DROPPED
    else -> throw NotImplementedError("Unknown status: $this")
}

internal fun Track.toShikimoriStatus() = when (status) {
    // rawValues because Shikimori doesn't have GraphQL mutations and this goes through the v2 API
    // if/when Shikimori adds GraphQL mutations, this should return UserRateStatusEnum members
    Shikimori.READING -> UserRateStatusEnum.watching.rawValue
    Shikimori.COMPLETED -> UserRateStatusEnum.completed.rawValue
    Shikimori.ON_HOLD -> UserRateStatusEnum.on_hold.rawValue
    Shikimori.DROPPED -> UserRateStatusEnum.dropped.rawValue
    Shikimori.PLAN_TO_READ -> UserRateStatusEnum.planned.rawValue
    Shikimori.REREADING -> UserRateStatusEnum.rewatching.rawValue
    else -> throw NotImplementedError("Unknown status: $status")
}
