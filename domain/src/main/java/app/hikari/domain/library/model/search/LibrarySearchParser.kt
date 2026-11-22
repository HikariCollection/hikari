package app.hikari.domain.library.model.search

class LibrarySearchParser(private val tokens: List<LibrarySearchLexer.Token>) {
    private var index = 0
    private var parenDepth = 0

    private fun peek(): LibrarySearchLexer.Token? = tokens.getOrNull(index)
    private fun advance(): LibrarySearchLexer.Token? = tokens.getOrNull(index++)

    fun parse(): QueryNode {
        if (tokens.isEmpty()) return EmptyQueryNode
        return parseOr()
    }

    private fun parseOr(): QueryNode {
        val nodes = mutableListOf<QueryNode>()
        nodes.add(parseAnd())
        while (peek() is LibrarySearchLexer.Token.Or) {
            advance()
            nodes.add(parseAnd())
        }
        val filteredNodes = nodes.filter { it !is EmptyQueryNode }
        return when (filteredNodes.size) {
            0 -> EmptyQueryNode
            1 -> filteredNodes.first()
            else -> OrNode(filteredNodes)
        }
    }

    private fun parseAnd(): QueryNode {
        val nodes = mutableListOf<QueryNode>()

        while (index < tokens.size &&
            peek() !is LibrarySearchLexer.Token.Or &&
            (peek() !is LibrarySearchLexer.Token.RParen || parenDepth == 0)
        ) {
            if (peek() is LibrarySearchLexer.Token.RParen) {
                advance()
                continue
            }
            if (peek() is LibrarySearchLexer.Token.And) {
                advance()
            }
            nodes.add(parseTerm())
        }

        val filteredNodes = nodes.filter { it !is EmptyQueryNode }
        if (filteredNodes.isEmpty()) return EmptyQueryNode
        return if (filteredNodes.size == 1) filteredNodes.first() else AndNode(filteredNodes)
    }

    private fun parseTerm(): QueryNode {
        var negated = false

        while (peek() is LibrarySearchLexer.Token.Not) {
            advance()
            negated = !negated
        }

        if (peek() is LibrarySearchLexer.Token.LParen) {
            advance()
            parenDepth++

            val subTree = parseOr()

            if (peek() is LibrarySearchLexer.Token.RParen) {
                advance()
                parenDepth--
            }

            if (negated) {
                return NotNode(subTree)
            }
            return subTree
        }

        return when (val nextToken = advance()) {
            is LibrarySearchLexer.Token.General -> GeneralQueryNode(nextToken.value, negated)
            is LibrarySearchLexer.Token.Field -> {
                MangaField.fromString(nextToken.field)?.let {
                    FieldQueryNode(it, nextToken.value, negated)
                } ?: GeneralQueryNode("${nextToken.field}:${nextToken.value}", negated)
            }
            is LibrarySearchLexer.Token.CompField -> {
                val comp = Comparator.fromString(nextToken.comparator)
                val field = ComparisonField.fromString(nextToken.field)
                if (comp != null && field != null) {
                    ComparisonQueryNode(field, nextToken.value, comp, negated)
                } else {
                    GeneralQueryNode("${nextToken.field}${nextToken.comparator}${nextToken.value}", negated)
                }
            }
            else -> EmptyQueryNode
        }
    }
}
