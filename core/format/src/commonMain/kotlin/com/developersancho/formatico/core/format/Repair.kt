package com.developersancho.formatico.core.format

import com.developersancho.formatico.core.format.text.TextRange

/** What a repair does: what its button says. */
enum class RepairKind {
    /** Takes a comment out. */
    RemoveComment,

    /** Puts a comma after the value before. */
    AddComma,

    /** Puts a key in double quotes. */
    QuoteKey,

    /** Takes a comma out. */
    RemoveComma,

    /** Writes a string in double quotes. */
    UseDoubleQuotes,

    /** Closes a string, a comment, an object or an array. */
    Close,

    /** Puts a colon after a key. */
    AddColon,

    /** Takes out what should not be there: a character, or text after the value. */
    Remove,

    /** Writes a character as an escape. */
    Escape,

    /** Writes a number as the format spells it. */
    FixNumber,

    /** Writes a quote inside a CSV field as two. */
    DoubleTheQuote
}

/** One change to a text: what stands in [range] replaced by [text]. */
data class TextEdit(val range: TextRange, val text: String)

/** How a problem is repaired: what the repair does, and the changes it makes to the text. */
data class Repair(val kind: RepairKind, val edits: List<TextEdit>)

/** How many times [Repairs.all] reads the text again at most: a repair can bring out a problem it hid. */
private const val ROUNDS = 20

/** The repair of each problem Formatico knows how to repair. */
object Repairs {
    /** How [problem] in [text] is repaired, or null where it is not one Formatico repairs. */
    fun of(problem: Problem, text: String): Repair? {
        val range = problem.range
        val at = text.substring(range.start, range.end)
        return when (problem.code) {
            ProblemCode.CommentNotAllowed -> Repair(RepairKind.RemoveComment, listOf(removeWithBlankLine(text, range)))
            ProblemCode.ExpectedCommaOrEnd -> Repair(RepairKind.AddComma, listOf(insert(afterValue(text, range.start), ",")))
            ProblemCode.UnquotedPropertyName -> Repair(RepairKind.QuoteKey, listOf(TextEdit(range, quoted(at))))
            ProblemCode.TrailingComma -> Repair(RepairKind.RemoveComma, listOf(TextEdit(range, "")))
            ProblemCode.SingleQuotedString -> Repair(RepairKind.UseDoubleQuotes, listOf(TextEdit(range, doubleQuoted(at))))
            ProblemCode.UnterminatedString -> Repair(RepairKind.Close, listOf(insert(range.end, "\"")))
            ProblemCode.UnterminatedComment -> Repair(RepairKind.Close, listOf(insert(text.length, " */")))
            ProblemCode.Unclosed -> closer(at)?.let { Repair(RepairKind.Close, listOf(insert(text.trimEnd().length, it))) }
            ProblemCode.ExpectedColon -> Repair(RepairKind.AddColon, listOf(insert(afterValue(text, range.start), ":")))
            ProblemCode.TrailingContent, ProblemCode.UnexpectedCharacter -> Repair(RepairKind.Remove, listOf(TextEdit(range, "")))
            ProblemCode.ControlCharacterInString -> Repair(RepairKind.Escape, listOf(TextEdit(range, escaped(at))))
            ProblemCode.InvalidEscape -> Repair(RepairKind.Escape, listOf(TextEdit(TextRange(range.start, range.start + 1), "\\\\")))
            ProblemCode.InvalidNumber -> fixedNumber(at)?.let { Repair(RepairKind.FixNumber, listOf(TextEdit(range, it))) }
            ProblemCode.UnclosedQuote -> Repair(RepairKind.Close, listOf(insert(range.end, "\"")))
            ProblemCode.StrayQuote -> Repair(RepairKind.DoubleTheQuote, listOf(insert(range.start, "\"")))
            else -> null
        }
    }

    /**
     * [text] with every problem [read] finds in it that Formatico repairs, repaired: the repairs of a reading made at
     * once, the later ones first so that the earlier ones' places hold, and the text read again, until no repair is
     * left.
     */
    fun all(text: String, read: (String) -> List<Problem>): String {
        var repaired = text
        repeat(ROUNDS) {
            val edits = read(repaired).mapNotNull { of(it, repaired) }.flatMap { it.edits }
            if (edits.isEmpty()) return repaired
            repaired = applied(repaired, edits)
        }
        return repaired
    }

    /** [text] with [edits] made, the later first; an edit that overlaps one made already is left for the next round. */
    fun applied(text: String, edits: List<TextEdit>): String {
        val builder = StringBuilder(text)
        var limit = Int.MAX_VALUE
        edits.sortedWith(compareByDescending<TextEdit> { it.range.start }.thenByDescending { it.range.end }).forEach { edit ->
            if (edit.range.end <= limit) {
                builder.setRange(edit.range.start, edit.range.end, edit.text)
                limit = edit.range.start
            }
        }
        return builder.toString()
    }

    private fun insert(at: Int, text: String) = TextEdit(TextRange(at, at), text)

    /** Where the value before [offset] ends: past the room and the line breaks before it. */
    private fun afterValue(text: String, offset: Int): Int {
        var end = offset
        while (end > 0 && text[end - 1].isWhitespace()) end--
        return end
    }

    /** [range] taken out, and its line with it where nothing but room is left on it. */
    private fun removeWithBlankLine(text: String, range: TextRange): TextEdit {
        val lineStart = text.lastIndexOf('\n', range.start - 1) + 1
        val lineEnd = text.indexOf('\n', range.end).let { if (it < 0) text.length else it }
        val rest = text.substring(lineStart, range.start) + text.substring(range.end, lineEnd)
        return if (rest.isBlank()) {
            TextEdit(TextRange(lineStart, minOf(lineEnd + 1, text.length)), "")
        } else {
            // A comment after a value: it goes with the room before it.
            TextEdit(TextRange(lineStart + text.substring(lineStart, range.start).trimEnd().length, range.end), "")
        }
    }
}
