package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.core.format.text.TextRange

/**
 * A TOML text as TOML 1.0 reads it: its tables, which keep the order of keys and the spelling of every value, its
 * comments, and its problems, each with its line and column.
 *
 * Reading goes on past a mistake: a mistake ends at most its line, and what can be read of the rest is read, so that
 * every problem of the text is found. A key given twice and a table defined twice are errors, as TOML has them.
 *
 * @property text the text read.
 * @property root the document's table, over the whole text: as much of it as could be read where there are errors.
 * @property comments where each comment is, from its `#` to the end of its line, in order.
 * @property problems what is wrong in it, in the order of the text.
 * @property statements its key/values and headers, in order, as they are written: what it is written out again from.
 */
class TomlDocument private constructor(
    val text: String,
    val root: TomlTable,
    val comments: List<TextRange>,
    val problems: List<Problem>,
    internal val statements: List<TomlStatement>
) {
    /** Whether the document has no error; a warning leaves it valid. */
    val isValid: Boolean get() = problems.none { it.severity == Severity.Error }

    /** The text the document was read from, character for character: nothing of it is lost. */
    fun print(): String = text

    companion object {
        /** Reads [text] as TOML. */
        fun parse(text: String): TomlDocument {
            val scanner = TomlScanner(text)
            val statements = TomlStatementReader(scanner).read()
            val root = TomlTreeBuilder(text.length) { code, range -> scanner.report(code, range.start, range.end) }
                .build(statements)
            val lines = LineMap(text)
            // A reader that went back over what it had read found some problems and comments twice.
            val problems = scanner.found
                .distinctBy { Triple(it.code, it.start, it.end) }
                .sortedBy { it.start }
                .map {
                    Problem(
                        code = it.code,
                        severity = Severity.Error,
                        range = TextRange(it.start, it.end),
                        start = lines.position(it.start),
                        end = lines.position(it.end)
                    )
                }
            val comments = scanner.comments.distinctBy { it.start }.sortedBy { it.start }
            return TomlDocument(text, root, comments, problems, statements)
        }
    }
}
