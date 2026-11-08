package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.format.types.TypeNote
import com.developersancho.formatico.feature.workspace.strings.TypesStrings
import com.developersancho.formatico.feature.workspace.strings.typesStrings
import com.pantrixui.components.container.alert.AlertTone
import com.pantrixui.components.container.alert.PantrixAlert
import com.pantrixui.foundation.l10n.PantrixStrings
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * What the types are and what of the document could not be told: how many types, from which document; then, where
 * [withLibrary] says so, what they are written for, and a sentence for each [TypeNote]. A phone leaves the library out.
 */
@Composable
internal fun TypesAlert(written: TypesOutcome.Written, documentName: String, withLibrary: Boolean, modifier: Modifier = Modifier) {
    val strings = typesStrings
    val sentences = buildList {
        if (withLibrary) add(strings.libraryOf(written.language))
        written.notes.forEach { note -> add(strings.sentenceOf(note, written.language, written.root, PantrixTheme.strings)) }
    }
    PantrixAlert(
        message = sentences.joinToString(" "),
        modifier = modifier,
        tone = AlertTone.Info,
        title = strings.written(written.types, written.types, documentName),
        tagKey = "types_notes"
    )
}

/** What the types are written for in [language]: "Kotlin data classes for kotlinx.serialization." */
private fun TypesStrings.libraryOf(language: TypeLanguage): String = when (language) {
    TypeLanguage.Kotlin -> kotlinLibrary
    TypeLanguage.TypeScript -> typeScriptLibrary
    TypeLanguage.Swift -> swiftLibrary
    TypeLanguage.Go -> goLibrary
    TypeLanguage.JsonSchema -> jsonSchemaLibrary
}

/** The sentence of [note], in types written in [language], the root one [root], its names joined as [words] join them. */
private fun TypesStrings.sentenceOf(note: TypeNote, language: TypeLanguage, root: String, words: PantrixStrings): String = when (note) {
    is TypeNote.OnlyNull -> {
        val unknown = unknownOf(language, onlyNull = true)
        if (note.names.size == 1) onlyNullOne(namesOf(note.names, words), unknown) else onlyNullMany(namesOf(note.names, words), unknown)
    }

    is TypeNote.Mixed -> {
        val unknown = unknownOf(language, onlyNull = false)
        if (note.names.size == 1) mixedOne(namesOf(note.names, words), unknown) else mixedMany(namesOf(note.names, words), unknown)
    }

    is TypeNote.EmptyList -> {
        val unknown = unknownOf(language, onlyNull = false)
        if (note.names.size == 1) emptyOne(namesOf(note.names, words), unknown) else emptyMany(namesOf(note.names, words), unknown)
    }

    is TypeNote.Dates -> if (note.names.size == 1) datesOne(namesOf(note.names, words)) else datesMany(namesOf(note.names, words))

    TypeNote.RootList -> rootList(root)
}

/** [names] in a sentence as the language joins them: one or two, or the first and how many others. */
private fun namesOf(names: List<String>, words: PantrixStrings): String =
    if (names.size <= 2) words.list(names) else words.listWithMore(names.take(1), more = names.size - 1)

/**
 * What a value whose type cannot be told is written as in [language]: Kotlin's `JsonElement`, nullable where it is
 * [onlyNull], TypeScript's `unknown`, Go's `any`; Swift has none in Codable, so a value only ever null is a `String?`
 * marked as a guess and any other `JSONValue`, which the types bring along; a JSON Schema's is `{}`, which allows
 * anything.
 */
private fun TypesStrings.unknownOf(language: TypeLanguage, onlyNull: Boolean): String = when (language) {
    TypeLanguage.Kotlin -> if (onlyNull) "JsonElement?" else "JsonElement"
    TypeLanguage.TypeScript -> "unknown"
    TypeLanguage.Swift -> if (onlyNull) swiftGuess else "JSONValue"
    TypeLanguage.Go -> "any"
    TypeLanguage.JsonSchema -> "{}"
}
