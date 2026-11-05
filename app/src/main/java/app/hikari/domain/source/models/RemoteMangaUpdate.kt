package app.hikari.domain.source.models

import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.manga.model.Manga

data class RemoteMangaUpdate(
    val manga: Manga,
    val newChapters: List<Chapter>,
)
