package app.hikari.ui.reader.viewer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.hikari.data.download.DownloadManager
import app.hikari.domain.manga.model.Manga
import app.hikari.source.Source
import app.hikari.source.local.isLocal
import app.hikari.ui.reader.model.ChapterTransition

class TransitionDownloadState(
    val currChapterDownloaded: Boolean,
    goingToChapterDownloaded: Boolean,
    private val isGoingToChapterOnDisk: (suspend () -> Boolean)?,
) {
    var goingToChapterDownloaded by mutableStateOf(goingToChapterDownloaded)
        private set

    suspend fun checkDisk() {
        isGoingToChapterOnDisk?.let { goingToChapterDownloaded = it() }
    }
}

fun DownloadManager.transitionDownloadState(
    transition: ChapterTransition,
    manga: Manga?,
    source: Source?,
): TransitionDownloadState? {
    if (manga == null || source == null) return null
    val goingToChapter = transition.to?.chapter.takeUnless { manga.isLocal() }
    return TransitionDownloadState(
        currChapterDownloaded = transition.from.pageLoader?.isLocal == true,
        goingToChapterDownloaded = manga.isLocal() ||
            goingToChapter?.let {
                isChapterDownloaded(
                    chapterName = it.name,
                    chapterScanlator = it.scanlator,
                    chapterUrl = it.url,
                    mangaTitle = manga.title,
                    sourceId = manga.source,
                )
            } ?: false,
        isGoingToChapterOnDisk = goingToChapter?.let {
            {
                isChapterDownloadedOnDisk(
                    chapterName = it.name,
                    chapterScanlator = it.scanlator,
                    chapterUrl = it.url,
                    mangaTitle = manga.title,
                    source = source,
                )
            }
        },
    )
}
