package app.hikari.domain.library.model.search

import app.hikari.domain.library.model.search.LibrarySearchLexer.Token
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class LibrarySearchLexerTest {

    @Test
    fun `tokenize returns empty list for empty or blank input`() {
        LibrarySearchLexer.tokenize("").shouldBeEmpty()
        LibrarySearchLexer.tokenize("   ").shouldBeEmpty()
        LibrarySearchLexer.tokenize("  ,  ,  ").shouldBeEmpty()
    }

    @Test
    fun `tokenize handles general words and quotes`() {
        LibrarySearchLexer.tokenize("one piece") shouldBe listOf(
            Token.General("one"),
            Token.General("piece"),
        )

        LibrarySearchLexer.tokenize("\"one piece\"") shouldBe listOf(
            Token.General("one piece"),
        )

        LibrarySearchLexer.tokenize("'one piece'") shouldBe listOf(
            Token.General("one piece"),
        )
    }

    @Test
    fun `tokenize handles operators`() {
        LibrarySearchLexer.tokenize("&& || ( ) -term") shouldBe listOf(
            Token.And,
            Token.Or,
            Token.LParen,
            Token.RParen,
            Token.Not,
            Token.General("term"),
        )
    }

    @Test
    fun `tokenize handles key-value fields with and without whitespace`() {
        LibrarySearchLexer.tokenize("tag:action") shouldBe listOf(
            Token.Field("tag", "action"),
        )

        LibrarySearchLexer.tokenize("tag: action") shouldBe listOf(
            Token.Field("tag", "action"),
        )

        LibrarySearchLexer.tokenize("tag : action") shouldBe listOf(
            Token.Field("tag", "action"),
        )

        LibrarySearchLexer.tokenize("tag: \"space opera\"") shouldBe listOf(
            Token.Field("tag", "space opera"),
        )

        LibrarySearchLexer.tokenize("author:'Eiichiro Oda'") shouldBe listOf(
            Token.Field("author", "Eiichiro Oda"),
        )
    }

    @Test
    fun `tokenize handles comparison fields with and without whitespace`() {
        LibrarySearchLexer.tokenize("unread>=5") shouldBe listOf(
            Token.CompField("unread", ">=", "5"),
        )

        LibrarySearchLexer.tokenize("unread >= 5") shouldBe listOf(
            Token.CompField("unread", ">=", "5"),
        )

        LibrarySearchLexer.tokenize("unread > 0") shouldBe listOf(
            Token.CompField("unread", ">", "0"),
        )

        LibrarySearchLexer.tokenize("read <= 10") shouldBe listOf(
            Token.CompField("read", "<=", "10"),
        )

        LibrarySearchLexer.tokenize("total = 100") shouldBe listOf(
            Token.CompField("total", "=", "100"),
        )

        LibrarySearchLexer.tokenize("added > \"2024-01-01\"") shouldBe listOf(
            Token.CompField("added", ">", "2024-01-01"),
        )
    }

    @Test
    fun `tokenize handles complex combined expressions`() {
        val query = "(tag:action || tag: \"adventure\") && unread >= 1 -author:oda"
        LibrarySearchLexer.tokenize(query) shouldBe listOf(
            Token.LParen,
            Token.Field("tag", "action"),
            Token.Or,
            Token.Field("tag", "adventure"),
            Token.RParen,
            Token.And,
            Token.CompField("unread", ">=", "1"),
            Token.Not,
            Token.Field("author", "oda"),
        )
    }
}
