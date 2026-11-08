package com.developersancho.formatico.core.format.types

/** [types] as Swift: a Codable struct for each, after `import Foundation`, and `JSONValue` at the end where it is used. */
internal fun swiftTypes(types: TypeSet): String = SwiftWriter(types).document()

/**
 * Writes types as Swift structs, indented by four spaces: a `let` for each member, in lower camel case, `T?` where
 * the member is optional or nullable, and `CodingKeys` after them where a JSON name is another than its property's.
 * A member only ever null is a `String?`, a comment saying so; one whose type cannot be told otherwise is a
 * `JSONValue`, written once after the types.
 */
private class SwiftWriter(private val types: TypeSet) {
    private var anything = false

    /** The import, and the types after it, a blank line before each. */
    fun document(): String = buildString {
        append("import Foundation\n")
        types.declarations.forEach { append('\n').append(struct(it)) }
        if (anything) append('\n').append(JsonValueType)
    }

    private fun struct(declaration: Declaration): String = buildString {
        append("struct ").append(declaration.name).append(": Codable {")
        val members = declaration.record.members
        if (members.isEmpty()) {
            append("}\n")
        } else {
            append('\n')
            val names = uniqueNames(members.map { propertyName(it.name) })
            members.forEachIndexed { index, member -> append(INDENT).append(property(member, names[index])).append('\n') }
            if (members.indices.any { names[it] != members[it].name }) append(codingKeys(members, names))
            append("}\n")
        }
    }

    private fun property(member: Member, name: String): String {
        val optional = if (member.optional || member.nullable) "?" else ""
        val guess = if (member.type.isGuess) GUESS else ""
        return "let ${swiftIdentifier(name)}: ${typeOf(member.type)}$optional$guess"
    }

    /** The `CodingKeys` of [members], whose properties are named [names]: a JSON name another than its property's after it. */
    private fun codingKeys(members: List<Member>, names: List<String>): String = buildString {
        append('\n').append(INDENT).append("enum CodingKeys: String, CodingKey {\n")
        members.forEachIndexed { index, member ->
            append(INDENT).append(INDENT).append("case ").append(swiftIdentifier(names[index]))
            if (names[index] != member.name) append(" = ").append(swiftString(member.name))
            append('\n')
        }
        append(INDENT).append("}\n")
    }

    private fun typeOf(type: ValueType): String = when (type) {
        ValueType.Text, ValueType.OnlyNull -> "String"
        ValueType.Whole, ValueType.LongWhole -> "Int"
        ValueType.Decimal -> "Double"
        ValueType.Truth -> "Bool"
        ValueType.Anything -> JSON_VALUE.also { anything = true }
        is ValueType.Record -> types.nameOf(type)
        is ValueType.ListOf -> "[${typeOf(type.item)}${if (type.nullableItems) "?" else ""}]"
    }

    private companion object {
        const val INDENT = "    "
        const val JSON_VALUE = "JSONValue"
        const val GUESS = " // Only ever null here: its type is a guess"
    }
}

/** Whether the type is a guess: only ever null, or a list of such. */
private val ValueType.isGuess: Boolean get() = this == ValueType.OnlyNull || (this is ValueType.ListOf && item.isGuess)

/** [name] as a Swift identifier: in backticks where it is a keyword. */
private fun swiftIdentifier(name: String): String = if (name in SwiftKeywords) "`$name`" else name

/** [text] as a Swift string literal, with what Swift escapes escaped. */
private fun swiftString(text: String): String = buildString {
    append('"')
    text.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            in '\u0000'..'\u001F', '\u007F' -> append("\\u{").append(char.code.toString(HEX)).append('}')
            else -> append(char)
        }
    }
    append('"')
}

private const val HEX = 16

/** Swift's keywords, and the names it gives a meaning of their own: no name can be one but in backticks. */
private val SwiftKeywords = setOf(
    "class",
    "struct",
    "let",
    "var",
    "func",
    "import",
    "in",
    "is",
    "return",
    "self",
    "Self",
    "Type",
    "protocol",
    "extension",
    "enum",
    "case",
    "switch",
    "default",
    "where",
    "while",
    "for",
    "if",
    "else",
    "do",
    "try",
    "throw",
    "throws",
    "catch",
    "true",
    "false",
    "nil",
    "operator",
    "static",
    "subscript",
    "super",
    "init",
    "deinit",
    "internal",
    "private",
    "public",
    "fileprivate",
    "open",
    "repeat",
    "guard",
    "defer",
    "break",
    "continue",
    "fallthrough",
    "inout",
    "associatedtype",
    "typealias",
    "Any"
)

/** The types the Swift written refers to, and the keywords no type can be named even in backticks: `Any`, `Self`. */
internal val SwiftTypeNames =
    setOf("Any", "Bool", "Codable", "CodingKey", "CodingKeys", "Decoder", "Double", "Encoder", "Int", "JSONValue", "Self", "String")

/** A JSON value of any kind, for a member whose type cannot be told: it reads and writes itself as it was. */
private val JsonValueType =
    """
    enum JSONValue: Codable {
        case string(String)
        case number(Double)
        case bool(Bool)
        case object([String: JSONValue])
        case array([JSONValue])
        case null

        init(from decoder: Decoder) throws {
            let container = try decoder.singleValueContainer()
            if container.decodeNil() {
                self = .null
            } else if let value = try? container.decode(Bool.self) {
                self = .bool(value)
            } else if let value = try? container.decode(Double.self) {
                self = .number(value)
            } else if let value = try? container.decode(String.self) {
                self = .string(value)
            } else if let value = try? container.decode([JSONValue].self) {
                self = .array(value)
            } else {
                self = .object(try container.decode([String: JSONValue].self))
            }
        }

        func encode(to encoder: Encoder) throws {
            var container = encoder.singleValueContainer()
            switch self {
            case .string(let value):
                try container.encode(value)
            case .number(let value):
                try container.encode(value)
            case .bool(let value):
                try container.encode(value)
            case .object(let value):
                try container.encode(value)
            case .array(let value):
                try container.encode(value)
            case .null:
                try container.encodeNil()
            }
        }
    }
    """.trimIndent() + "\n"
