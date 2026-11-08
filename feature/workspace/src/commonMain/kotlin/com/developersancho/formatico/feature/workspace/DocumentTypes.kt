package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Immutable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.format.types.TypeNote
import com.developersancho.formatico.core.format.types.generateTypes
import com.developersancho.formatico.core.format.types.rootTypeName
import com.developersancho.formatico.core.format.types.typeNameOf
import com.developersancho.formatico.core.format.types.typesFileName

/**
 * How a document's types are written, as its view keeps it.
 *
 * @property language the language they are written in.
 * @property root the root type's name as the reader typed it, or null for the one the document's name gives.
 */
data class TypesChoice(val language: TypeLanguage = TypeLanguage.Kotlin, val root: String? = null)

/** What writing a document's types came to. */
internal sealed interface TypesOutcome {
    /** It could not be read: it is plain text, or nothing of it could be read. */
    data object Unread : TypesOutcome

    /** It has [errors]: an invalid document is repaired before its types are written. */
    data class Invalid(val format: Format, val errors: Int) : TypesOutcome

    /** It holds no object to type: a single value, or a list of values. */
    data class Untyped(val format: Format) : TypesOutcome

    /**
     * Its types, read [from] its format, written in [language]: the root one named [root], the file [fileName], their
     * [text], [bytes] long in UTF-8 and [lines] lines, how many [types] it declares, and what could not be told, [notes].
     */
    @Immutable
    data class Written(
        val from: Format,
        val language: TypeLanguage,
        val root: String,
        val fileName: String,
        val text: String,
        val bytes: Long,
        val lines: Int,
        val types: Int,
        val notes: List<TypeNote>
    ) : TypesOutcome
}

/** The types written, where [outcome] is so. */
internal val TypesOutcome?.written: TypesOutcome.Written? get() = this as? TypesOutcome.Written

/** The root type's name of [document] as [choice] has it: the reader's, made a type's name, or its own name's. */
internal fun rootNameOf(document: OpenDocument, choice: TypesChoice): String =
    choice.root?.takeIf { it.isNotBlank() }?.let(::typeNameOf) ?: rootTypeName(document.name)

/** [document], read as [reading], its types written as [choice] says. */
internal fun typesOf(document: OpenDocument, reading: Reading, choice: TypesChoice): TypesOutcome {
    val read = reading as? Reading.Read ?: return TypesOutcome.Unread
    val root = read.root ?: return TypesOutcome.Unread
    if (read.errors > 0) return TypesOutcome.Invalid(read.format, read.errors)
    val name = rootNameOf(document, choice)
    val generated = generateTypes(dataValueOf(root, read.format), choice.language, name) ?: return TypesOutcome.Untyped(read.format)
    return TypesOutcome.Written(
        from = read.format,
        language = choice.language,
        root = name,
        fileName = typesFileName(name, choice.language),
        text = generated.text,
        bytes = generated.text.encodeToByteArray().size.toLong(),
        lines = generated.text.trimEnd('\n').lines().size,
        types = generated.types,
        notes = generated.notes
    )
}
