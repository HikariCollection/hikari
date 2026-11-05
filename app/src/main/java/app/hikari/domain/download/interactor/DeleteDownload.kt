package app.hikari.domain.download.interactor

import dev.zacsweers.metro.Inject
import app.hikari.data.download.DownloadManager
import app.hikari.domain.chapter.model.Chapter
import app.hikari.domain.manga.model.Manga
import app.hikari.domain.source.service.SourceManager

@Inject
class DeleteDownload(
    private val sourceManager: SourceManager,
    private val downloadManager: DownloadManager,
) {

    suspend fun awaitAll(manga: Manga, vararg chapters: Chapter) {
        sourceManager.get(manga.source)?.let { source ->
            downloadManager.deleteChapters(chapters.toList(), manga, source)
        }
    }
}
