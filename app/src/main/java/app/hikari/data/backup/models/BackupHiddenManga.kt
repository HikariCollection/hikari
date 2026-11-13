package app.hikari.data.backup.models

import app.hikari.domain.manga.model.HiddenManga
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
class BackupHiddenManga(
    @ProtoNumber(1) var source: Long = 0,
    @ProtoNumber(2) var url: String = "",
    @ProtoNumber(3) var title: String = "",
    @ProtoNumber(4) var thumbnailUrl: String? = null,
    @ProtoNumber(5) var createdAt: Long = 0,
)

val backupHiddenMangaMapper = { hidden: HiddenManga ->
    BackupHiddenManga(
        source = hidden.sourceId,
        url = hidden.url,
        title = hidden.title,
        thumbnailUrl = hidden.thumbnailUrl,
        createdAt = hidden.createdAt.toEpochMilliseconds(),
    )
}
