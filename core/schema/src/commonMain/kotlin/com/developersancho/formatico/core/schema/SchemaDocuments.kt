package com.developersancho.formatico.core.schema

import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The schemas a validation reads what a keyword holds from: the [main] one, and the [others] it names by their `$id`.
 * A keyword of a schema none of them is, as the drafts' own, holds nothing that can be read.
 */
internal class SchemaDocuments(private val main: JsonElement?, others: List<JsonElement> = emptyList()) {
    private val byId: Map<String, JsonElement> = buildMap {
        others.forEach { other -> idOf(other)?.let { put(it, other) } }
        main?.let { schema -> idOf(schema)?.let { put(it, schema) } }
    }

    /** What the schema holds where [error]'s keyword is: its value, or null where it cannot be read. */
    fun keywordOf(error: ValidationError): JsonElement? {
        error.absoluteLocation?.let { location ->
            val uri = location.uri.toString().substringBefore('#')
            val schema = byId[uri] ?: main?.takeIf { uri.isEmpty() || SchemaDraft.named(uri) == null }
            return schema?.at(pointerTokens(location.path.toString()))
        }
        return main?.at(pointerTokens(error.schemaPath.toString()))
    }

    /** What the schema object [error]'s keyword stands in holds as [name], beside it. */
    fun besideKeyword(error: ValidationError, name: String): JsonElement? {
        val location = error.absoluteLocation
        val (schema, tokens) = if (location != null) {
            val uri = location.uri.toString().substringBefore('#')
            (byId[uri] ?: main?.takeIf { SchemaDraft.named(uri) == null }) to pointerTokens(location.path.toString())
        } else {
            main to pointerTokens(error.schemaPath.toString())
        }
        return schema?.at(tokens.dropLast(1) + name)
    }
}

/** The `$id` of [schema], `id` in draft 4, without its `#`; null where it has none. */
internal fun idOf(schema: JsonElement): String? {
    val holder = schema as? JsonObject ?: return null
    val id = (holder["\$id"] ?: holder["id"]) as? JsonPrimitive ?: return null
    return id.takeIf { it.isString }?.content?.substringBefore('#')?.takeIf { it.isNotEmpty() }
}
