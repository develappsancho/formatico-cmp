package com.developersancho.formatico.core.format.query

/** Reads a JSONPath's segments and selectors, as RFC 9535's grammar writes them; [JsonPathFilterParser] reads its filters. */
internal class JsonPathParser(private val cursor: JsonPathCursor) {
    private val filters = JsonPathFilterParser(cursor, this)

    /** The whole text, as a query from the document's root. */
    fun query(): PathQuery {
        val text = cursor.text
        if (text.all(::isPathWhitespace)) queryError(QueryErrorCode.Empty, 0)
        if (!cursor.accept('$')) queryError(QueryErrorCode.MissingRoot, 0)
        val query = PathQuery(relative = false, segments())
        if (!cursor.atEnd) {
            // Whitespace at the end is reported where it starts; anything else, where it stands.
            val trailing = cursor.at
            cursor.skipWhitespace()
            queryError(QueryErrorCode.UnexpectedCharacter, if (cursor.atEnd) trailing else cursor.at)
        }
        return query
    }

    /** The segments after a `$` or an `@`, as many as follow it; whitespace may stand before each. */
    fun segments(): List<PathSegment> {
        val segments = ArrayList<PathSegment>()
        while (true) {
            val before = cursor.at
            cursor.skipWhitespace()
            val segment = when (cursor.peek()) {
                '[' -> bracketed(descendant = false)
                '.' -> dotted()
                else -> null
            }
            if (segment == null) {
                cursor.at = before
                return segments
            }
            segments += segment
        }
    }

    /** The segment whose `.` or `..` is next: `.name`, `.*`, `..name`, `..*` or `..[…]`. */
    private fun dotted(): PathSegment {
        cursor.expect('.')
        val descendant = cursor.accept('.')
        if (descendant && cursor.peek() == '[') return bracketed(descendant = true)
        val selector = if (cursor.accept('*')) Selector.Wildcard else Selector.Name(cursor.readMemberName())
        return PathSegment(listOf(selector), descendant)
    }

    /** The segment whose `[` is next: its selectors, between commas, up to its `]`. */
    private fun bracketed(descendant: Boolean): PathSegment {
        cursor.enter()
        cursor.expect('[')
        val selectors = ArrayList<Selector>()
        do {
            cursor.skipWhitespace()
            selectors += selector()
            cursor.skipWhitespace()
        } while (cursor.accept(','))
        cursor.expect(']')
        cursor.leave()
        return PathSegment(selectors, descendant)
    }

    /** The selector next, in brackets: a quoted name, `*`, an index, a slice or a filter. */
    private fun selector(): Selector = when (val char = cursor.peek()) {
        '\'', '"' -> Selector.Name(cursor.readString())

        '*' -> {
            cursor.at++
            Selector.Wildcard
        }

        '?' -> {
            cursor.at++
            cursor.skipWhitespace()
            Selector.Filter(filters.filter())
        }

        else -> if (char == '-' || char == ':' || char in '0'..'9') indexOrSlice() else cursor.unexpected()
    }

    /** The index next, `1` or `-1`, or the slice, `start:end:step`, each of its parts left out or not. */
    private fun indexOrSlice(): Selector {
        val start = cursor.readInteger()
        cursor.skipWhitespace()
        if (!cursor.accept(':')) return Selector.Index(start ?: cursor.unexpected())
        cursor.skipWhitespace()
        val end = cursor.readInteger()
        cursor.skipWhitespace()
        var step: Long? = null
        if (cursor.accept(':')) {
            cursor.skipWhitespace()
            step = cursor.readInteger()
        }
        return Selector.Slice(start, end, step)
    }
}
