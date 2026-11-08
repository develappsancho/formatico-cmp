package com.developersancho.formatico.core.schema

/**
 * The kind of a value, as JSON Schema's `type` names it.
 *
 * @property keyword its name in a schema: `string`, `integer`.
 */
enum class JsonType(val keyword: String) {
    Null("null"),
    Boolean("boolean"),
    Integer("integer"),
    Number("number"),
    String("string"),
    Array("array"),
    Object("object");

    companion object {
        /** The kind a schema's `type` names as [keyword], or null for a name JSON Schema has not. */
        fun named(keyword: kotlin.String): JsonType? = entries.firstOrNull { it.keyword == keyword }
    }
}
