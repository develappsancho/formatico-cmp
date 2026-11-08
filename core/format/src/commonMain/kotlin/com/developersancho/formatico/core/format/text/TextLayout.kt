package com.developersancho.formatico.core.format.text

/** How a text ends its lines. */
enum class LineEnding(val characters: String) {
    /** `\n`, as Linux and macOS write. */
    Lf("\n"),

    /** `\r\n`, as Windows writes. */
    Crlf("\r\n"),

    /** `\r` alone, as the classic Mac OS wrote. */
    Cr("\r")
}

/** How a text indents its lines. */
sealed interface Indentation {
    /** What one level of it is written as. */
    val unit: String

    /** [count] spaces to a level. */
    data class Spaces(val count: Int) : Indentation {
        init {
            require(count > 0) { "An indentation has a space at least: $count" }
        }

        override val unit: String get() = " ".repeat(count)
    }

    /** A tab to a level. */
    data object Tabs : Indentation {
        override val unit: String get() = "\t"
    }
}

/**
 * How a text is laid out: how it ends its lines and how it indents them, as the first of them say.
 *
 * @property lineEnding the line ending of the first line, or null for a text of one line.
 * @property indentation one level of the indentation, or null for a text whose lines are not indented.
 */
data class TextLayout(val lineEnding: LineEnding?, val indentation: Indentation?) {
    companion object {
        /** How many lines are looked at for the indentation. */
        private const val LINES_LOOKED_AT = 200

        /** The widest level of spaces taken for one: a wider one is an alignment, not a level. */
        private const val MOST_SPACES = 8

        /** The layout of [text]. */
        fun of(text: CharSequence): TextLayout = TextLayout(lineEndingOf(text), indentationOf(text))

        private fun lineEndingOf(text: CharSequence): LineEnding? {
            val index = text.indexOfFirst { it == '\n' || it == '\r' }
            return when {
                index < 0 -> null
                text[index] == '\n' -> LineEnding.Lf
                index + 1 < text.length && text[index + 1] == '\n' -> LineEnding.Crlf
                else -> LineEnding.Cr
            }
        }

        /** The narrowest indentation of the first lines that are indented: tabs where the first of them is. */
        private fun indentationOf(text: CharSequence): Indentation? {
            var narrowest = Int.MAX_VALUE
            for (line in text.lineSequence().take(LINES_LOOKED_AT)) {
                if (line.isBlank()) continue
                if (line[0] == '\t') return Indentation.Tabs
                val spaces = line.indexOfFirst { it != ' ' }
                if (spaces in 1..MOST_SPACES && spaces < narrowest) narrowest = spaces
            }
            return if (narrowest == Int.MAX_VALUE) null else Indentation.Spaces(narrowest)
        }
    }
}
