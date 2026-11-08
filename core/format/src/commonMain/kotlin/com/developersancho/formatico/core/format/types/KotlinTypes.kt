package com.developersancho.formatico.core.format.types

/** [types] as Kotlin: a data class for each, serializable by kotlinx.serialization, after the imports they use. */
internal fun kotlinTypes(types: TypeSet): String = KotlinWriter(types).document()

/**
 * Writes types as Kotlin data classes, indented by four spaces: a property for each member, in lower camel case,
 * `@SerialName` before it where the JSON name is another, `T? = null` where the member is optional or nullable,
 * `JsonElement` where its type cannot be told. A type of no members is a plain class.
 */
private class KotlinWriter(private val types: TypeSet) {
    private var renamed = false
    private var elements = false

    /** The imports the types use, a blank line, and the types, a blank line between each. */
    fun document(): String {
        val classes = types.declarations.joinToString("\n") { declaration(it) }
        // In the order of their names, as Kotlin sorts imports.
        val imports = listOfNotNull(SERIAL_NAME.takeIf { renamed }, SERIALIZABLE, JSON_ELEMENT.takeIf { elements })
        return imports.joinToString("") { "import $it\n" } + "\n" + classes
    }

    private fun declaration(declaration: Declaration): String = buildString {
        append("@Serializable\n")
        val members = declaration.record.members
        if (members.isEmpty()) {
            append("class ").append(declaration.name).append('\n')
        } else {
            append("data class ").append(declaration.name).append("(\n")
            val names = uniqueNames(members.map { propertyName(it.name) })
            members.forEachIndexed { index, member ->
                append(INDENT).append(property(member, names[index])).append(if (index < members.lastIndex) ",\n" else "\n")
            }
            append(")\n")
        }
    }

    private fun property(member: Member, name: String): String {
        val annotation = if (name == member.name) "" else "@SerialName(${kotlinString(member.name)}) ".also { renamed = true }
        val absent = if (member.optional || member.nullable) "? = null" else ""
        return "${annotation}val ${kotlinIdentifier(name)}: ${typeOf(member.type)}$absent"
    }

    private fun typeOf(type: ValueType): String = when (type) {
        ValueType.Text -> "String"
        ValueType.Whole -> "Int"
        ValueType.LongWhole -> "Long"
        ValueType.Decimal -> "Double"
        ValueType.Truth -> "Boolean"
        ValueType.Anything, ValueType.OnlyNull -> "JsonElement".also { elements = true }
        is ValueType.Record -> types.nameOf(type)
        is ValueType.ListOf -> "List<${typeOf(type.item)}${if (type.nullableItems) "?" else ""}>"
    }

    private companion object {
        const val INDENT = "    "
        const val SERIAL_NAME = "kotlinx.serialization.SerialName"
        const val SERIALIZABLE = "kotlinx.serialization.Serializable"
        const val JSON_ELEMENT = "kotlinx.serialization.json.JsonElement"
    }
}

/** [name] as a Kotlin identifier: in backticks where it is a hard keyword. */
private fun kotlinIdentifier(name: String): String = if (name in KotlinKeywords) "`$name`" else name

/** [text] as a Kotlin string literal, with what Kotlin escapes escaped, a `$` among them. */
private fun kotlinString(text: String): String = buildString {
    append('"')
    text.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '$' -> append("\\\$")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            in '\u0000'..'\u001F' -> append("\\u").append(char.code.toString(HEX).padStart(UNICODE, '0'))
            else -> append(char)
        }
    }
    append('"')
}

private const val HEX = 16
private const val UNICODE = 4

/** Kotlin's hard keywords: no name can be one but in backticks. */
private val KotlinKeywords = setOf(
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while"
)

/** The types the Kotlin written refers to, which no type written may be named. */
internal val KotlinTypeNames = setOf("Boolean", "Double", "Int", "JsonElement", "List", "Long", "SerialName", "Serializable", "String")
