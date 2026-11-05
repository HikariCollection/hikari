package app.hikari.domain.manga.interactor

import dev.zacsweers.metro.Inject
import app.hikari.ui.reader.setting.ReaderOrientation
import app.hikari.ui.reader.setting.ReadingMode
import app.hikari.domain.manga.model.MangaUpdate
import app.hikari.domain.manga.repository.MangaRepository

@Inject
class SetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun awaitSetReadingMode(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaUpdate(id) {
                viewerFlags = manga.viewerFlags.setFlag(flag, ReadingMode.MASK.toLong())
            },
        )
    }

    suspend fun awaitSetOrientation(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaUpdate(id) {
                viewerFlags = manga.viewerFlags.setFlag(flag, ReaderOrientation.MASK.toLong())
            },
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
