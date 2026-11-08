package com.developersancho.formatico.core.format.types

import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.DataObject
import com.developersancho.formatico.core.format.convert.DataValue

/** The languages types are written in, each by the name a reader knows it by ([displayName]). */
enum class TypeLanguage(val displayName: String) {
    /** Data classes for kotlinx.serialization. */
    Kotlin("Kotlin"),

    /** Interfaces. */
    TypeScript("TypeScript"),

    /** Codable structs. */
    Swift("Swift"),

    /** Structs for encoding/json. */
    Go("Go"),

    /** A JSON Schema of draft 2020-12, which other documents of the same shape can be validated against (Faz 19). */
    JsonSchema("JSON Schema")
}

/**
 * Types written of a document: their [text], how many [types] it declares, and [notes] on what could not be told of
 * its values. [types] counts the document's own types, the same in every language: Swift's `JSONValue`, written where
 * a value can be anything, is not one of them.
 */
data class GeneratedTypes(val text: String, val types: Int, val notes: List<TypeNote>)

/**
 * What a generation says of the document's values. Each list of names is of JSON member names, in the order first
 * met, each once: a member whose values are arrays is named for what is said of their items.
 */
sealed interface TypeNote {
    /** Members whose every value is null: their type cannot be told. */
    data class OnlyNull(val names: List<String>) : TypeNote

    /**
     * Members whose values are of more than one kind (a string and a number, an object and an array): written as the
     * language's "anything".
     */
    data class Mixed(val names: List<String>) : TypeNote

    /** Arrays empty in every value: their items' type cannot be told. */
    data class EmptyList(val names: List<String>) : TypeNote

    /** String members whose every value is an ISO 8601 date, time or date-time (and TOML's dates): kept as strings. */
    data class Dates(val names: List<String>) : TypeNote

    /** The document is a list (an array at its root, NDJSON): the root type is the type of one of its items. */
    data object RootList : TypeNote
}

/**
 * The types of [value] in [language], the root one named [rootName] as [typeNameOf] makes a name of it; null where
 * the document holds no object to type (a single value, a list of values).
 *
 * Every value met at one place is merged into one shape: a member's values across every object at its place, an
 * array's items across every array. A member missing from some of those objects is optional, one that is null in some
 * is nullable. Whole numbers are 32-bit integers, 64-bit past that, and decimals where whole numbers and decimals
 * meet or a whole number is larger than 64 bits hold; values of more than one other kind are the language's
 * "anything". Every object is a type, named after its member, an array's items after the member singularised
 * ("items" → `Item`, "status" → `StatusItem`); objects of the same members of the same types are one type, the first
 * name kept, and a name met again for another type gets 2, 3, … after it. Types are written breadth first from the
 * root. A document that is an array whose items are objects (NDJSON reads as one) is typed by its items, the
 * [TypeNote.RootList] note saying so.
 */
fun generateTypes(value: DataValue, language: TypeLanguage, rootName: String): GeneratedTypes? {
    val shape = Shape().apply { add(value) }
    val root = when (value) {
        is DataObject -> shape
        is DataArray -> shape.items.takeIf { it.isRecords }
        else -> null
    } ?: return null
    val types = TypeSet.of(root.record(), typeNameOf(rootName), language)
    val text = when (language) {
        TypeLanguage.Kotlin -> kotlinTypes(types)
        TypeLanguage.TypeScript -> typeScriptTypes(types)
        TypeLanguage.Swift -> swiftTypes(types)
        TypeLanguage.Go -> goTypes(types, goPackageName(rootName))
        TypeLanguage.JsonSchema -> jsonSchemaTypes(types)
    }
    return GeneratedTypes(text, types.declarations.size, notesOf(root, rootList = value is DataArray))
}
