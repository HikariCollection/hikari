package app.hikari.domain.library.model.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class LibrarySearchParserTest {

    @Test
    fun `parse empty or blank query returns EmptyQueryNode`() {
        QueryNode.from("") shouldBe EmptyQueryNode
        QueryNode.from("   ") shouldBe EmptyQueryNode
        QueryNode.from(" , , ") shouldBe EmptyQueryNode
    }

    @Test
    fun `parse single general term`() {
        QueryNode.from("naruto") shouldBe GeneralQueryNode("naruto", negated = false)
        QueryNode.from("-naruto") shouldBe GeneralQueryNode("naruto", negated = true)
        QueryNode.from("--naruto") shouldBe GeneralQueryNode("naruto", negated = false)
    }

    @Test
    fun `parse implicit and explicit AND`() {
        val expected = AndNode(
            listOf(
                GeneralQueryNode("one", negated = false),
                GeneralQueryNode("piece", negated = false),
            ),
        )

        QueryNode.from("one piece") shouldBe expected
        QueryNode.from("one && piece") shouldBe expected
    }

    @Test
    fun `parse explicit OR`() {
        QueryNode.from("action || romance") shouldBe OrNode(
            listOf(
                GeneralQueryNode("action", negated = false),
                GeneralQueryNode("romance", negated = false),
            ),
        )
    }

    @Test
    fun `parse trailing OR simplifies without empty node`() {
        QueryNode.from("action ||") shouldBe GeneralQueryNode("action", negated = false)
    }

    @Test
    fun `parse operator precedence NOT higher than AND higher than OR`() {
        // "action || comedy && -romance" should parse as: action || (comedy && !romance)
        QueryNode.from("action || comedy && -romance") shouldBe OrNode(
            listOf(
                GeneralQueryNode("action", negated = false),
                AndNode(
                    listOf(
                        GeneralQueryNode("comedy", negated = false),
                        GeneralQueryNode("romance", negated = true),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `parse parenthesized expressions`() {
        // "(action || comedy) && unread>0"
        QueryNode.from("(action || comedy) && unread>0") shouldBe AndNode(
            listOf(
                OrNode(
                    listOf(
                        GeneralQueryNode("action", negated = false),
                        GeneralQueryNode("comedy", negated = false),
                    ),
                ),
                ComparisonQueryNode(
                    field = ComparisonField.UNREAD,
                    value = "0",
                    queryComparator = Comparator.GT,
                    negated = false,
                ),
            ),
        )
    }

    @Test
    fun `parse negated parenthesized expression produces NotNode`() {
        QueryNode.from("-(action || romance)") shouldBe NotNode(
            OrNode(
                listOf(
                    GeneralQueryNode("action", negated = false),
                    GeneralQueryNode("romance", negated = false),
                ),
            ),
        )
    }

    @Test
    fun `parse gracefully recovers from unclosed open parenthesis`() {
        QueryNode.from("(action || romance") shouldBe OrNode(
            listOf(
                GeneralQueryNode("action", negated = false),
                GeneralQueryNode("romance", negated = false),
            ),
        )
    }

    @Test
    fun `parse gracefully skips stray unmatched closing parenthesis`() {
        QueryNode.from("one ) piece") shouldBe AndNode(
            listOf(
                GeneralQueryNode("one", negated = false),
                GeneralQueryNode("piece", negated = false),
            ),
        )

        QueryNode.from(")") shouldBe EmptyQueryNode
        QueryNode.from("))") shouldBe EmptyQueryNode
    }

    @Test
    fun `parse fields and comparison fields with lenient whitespace`() {
        QueryNode.from("tag: shonen && unread >= 5") shouldBe AndNode(
            listOf(
                FieldQueryNode(
                    field = MangaField.GENRE,
                    value = "shonen",
                    negated = false,
                ),
                ComparisonQueryNode(
                    field = ComparisonField.UNREAD,
                    value = "5",
                    queryComparator = Comparator.GTE,
                    negated = false,
                ),
            ),
        )
    }

    @Test
    fun `parse unknown field or comparator falls back to GeneralQueryNode safely`() {
        QueryNode.from("unknownfield:value") shouldBe GeneralQueryNode(
            value = "unknownfield:value",
            negated = false,
        )
    }
}
