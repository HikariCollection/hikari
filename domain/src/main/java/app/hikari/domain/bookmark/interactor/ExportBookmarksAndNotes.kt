package app.hikari.domain.bookmark.interactor

import app.hikari.domain.bookmark.model.BookmarksAndNotesExportChapter
import app.hikari.domain.bookmark.model.BookmarksAndNotesExportEntry
import app.hikari.domain.bookmark.model.BookmarksAndNotesExportPayload
import app.hikari.domain.bookmark.model.BookmarksAndNotesGroup
import app.hikari.domain.chapter.model.bookmarkNote
import dev.zacsweers.metro.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import kotlin.time.Clock
import kotlin.time.Instant

@Inject
class ExportBookmarksAndNotes {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun toMarkdown(groups: List<BookmarksAndNotesGroup>): String {
        val builder = StringBuilder()
        builder.appendLine("# Hikari Bookmarks & Notes")
        for (group in groups) {
            builder.appendLine()
            builder.appendLine("## ${group.manga.title}")
            val libraryStatus = if (group.manga.favorite) "In library" else "Not in library"
            builder.appendLine("Source: ${group.sourceName} | $libraryStatus")

            val seriesNotes = group.manga.notes.trim()
            if (seriesNotes.isNotEmpty()) {
                builder.appendLine()
                builder.appendLine("### Series Notes")
                builder.appendLine(seriesNotes)
            }

            if (group.chapters.isNotEmpty()) {
                builder.appendLine()
                builder.appendLine("### Chapters")
                for (entry in group.chapters) {
                    val chapter = entry.chapter
                    val chapterPrefix = if (chapter.isRecognizedNumber) {
                        val number = BigDecimal.valueOf(chapter.chapterNumber).stripTrailingZeros().toPlainString()
                        "Ch. $number - ${chapter.name}"
                    } else {
                        chapter.name
                    }
                    val scanlator = chapter.scanlator?.takeIf { it.isNotBlank() }?.let { " [$it]" }.orEmpty()
                    val statusTags = buildList {
                        add(if (chapter.bookmark) "Bookmarked" else "Note only")
                        if (chapter.read) {
                            add("Read")
                        } else if (chapter.lastPageRead > 0) {
                            add("Page ${chapter.lastPageRead + 1}")
                        }
                    }
                    builder.appendLine("- $chapterPrefix$scanlator (${statusTags.joinToString(", ")})")
                    chapter.bookmarkNote?.let { note ->
                        builder.appendLine("  Note: $note")
                    }
                }
            }
        }
        return builder.toString()
    }

    fun toJson(
        groups: List<BookmarksAndNotesGroup>,
        exportedAt: Instant = Clock.System.now(),
    ): String {
        val entries = groups.map { group ->
            BookmarksAndNotesExportEntry(
                mangaTitle = group.manga.title,
                sourceName = group.sourceName,
                inLibrary = group.manga.favorite,
                mangaNotes = group.manga.notes.trim(),
                chapters = group.chapters.map { entry ->
                    val chapter = entry.chapter
                    BookmarksAndNotesExportChapter(
                        chapterName = chapter.name,
                        chapterNumber = chapter.chapterNumber,
                        scanlator = chapter.scanlator,
                        bookmarked = chapter.bookmark,
                        bookmarkNote = chapter.bookmarkNote,
                        lastPageRead = chapter.lastPageRead,
                        read = chapter.read,
                    )
                },
            )
        }
        return json.encodeToString(
            BookmarksAndNotesExportPayload(
                exportedAt = exportedAt,
                entries = entries,
            ),
        )
    }
}
