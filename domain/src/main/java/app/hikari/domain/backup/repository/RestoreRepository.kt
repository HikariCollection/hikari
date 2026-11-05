package app.hikari.domain.backup.repository

import app.hikari.domain.backup.model.RestoredManga
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.manga.model.MangaUpdate

interface RestoreRepository {

    /**
     * Restores [entries] all together: either every one of them is restored or none is. [update] is what to change on each
     * entry once its chapters and history are in place, and is part of the same all-or-nothing restore.
     */
    suspend fun restoreManga(entries: List<RestoredManga>, update: suspend (Manga) -> MangaUpdate)
}
