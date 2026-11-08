package com.developersancho.formatico.core.schema

import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/** [error] of a value of [instance], as Formatico says it: where the value is, what the schema asks, what it is. */
internal fun SchemaDocuments.problemOf(error: ValidationError, instance: JsonElement): SchemaProblem {
    val tokens = pointerTokens(error.objectPath.toString())
    val value = instance.at(tokens) ?: JsonNull
    return SchemaProblem(stepsOf(tokens, instance), ruleOf(error, value), typeOf(value), scalarOf(value))
}

/** [value] written as JSON where it is a single value; null for an object or an array. */
internal fun scalarOf(value: JsonElement): String? = value.takeIf { it !is JsonObject && it !is JsonArray }?.let(::written)

/** What the keyword [error] reports asks of [value]. */
private fun SchemaDocuments.ruleOf(error: ValidationError, value: JsonElement): SchemaRule {
    val tokens = pointerTokens(error.schemaPath.toString())
    val keyword = tokens.lastOrNull().orEmpty()
    val held = keywordOf(error)
    if (held == FalseSchema) return SchemaRule.NotAllowed
    val parent = tokens.getOrNull(tokens.size - 2)
    if (parent == "dependentRequired" || parent == "dependencies") return SchemaRule.DependentRequired(keyword, missingOf(held, value))
    return when (keyword) {
        "type" -> SchemaRule.Type(typesOf(held))
        "required" -> SchemaRule.Required(missingOf(held, value))
        "enum" -> SchemaRule.Enum(allowedOf(held))
        "const" -> SchemaRule.Const(held?.let(::written).orEmpty())
        "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum" -> boundOf(error, keyword, held)
        "multipleOf", "divisibleBy" -> SchemaRule.MultipleOf(held?.let(::written).orEmpty())
        "minLength", "maxLength" -> SchemaRule.Length(held.count(), lower = keyword == "minLength")
        "pattern" -> SchemaRule.Pattern(held.text())
        "format" -> SchemaRule.Format(held.text())
        "minItems", "maxItems" -> SchemaRule.ItemCount(held.count(), lower = keyword == "minItems")
        "uniqueItems" -> SchemaRule.UniqueItems
        "contains", "minContains", "maxContains" -> SchemaRule.Contains
        "minProperties", "maxProperties" -> SchemaRule.KeyCount(held.count(), lower = keyword == "minProperties")
        "not" -> SchemaRule.Not
        "oneOf" -> SchemaRule.ManyChoices
        else -> SchemaRule.Other(keyword)
    }
}

/**
 * A bound: `minimum` or `maximum`, past it where draft 4's `exclusiveMinimum` or `exclusiveMaximum` beside it is
 * `true`; or a later draft's `exclusiveMinimum` or `exclusiveMaximum`, a bound of its own.
 */
private fun SchemaDocuments.boundOf(error: ValidationError, keyword: String, held: JsonElement?): SchemaRule.Bound {
    val lower = keyword == "minimum" || keyword == "exclusiveMinimum"
    val inclusive = if (lower) "minimum" else "maximum"
    val exclusive = if (lower) "exclusiveMinimum" else "exclusiveMaximum"
    return when {
        keyword == inclusive -> SchemaRule.Bound(
            limit = held?.let(::written).orEmpty(),
            lower = lower,
            exclusive = (besideKeyword(error, exclusive) as? JsonPrimitive)?.booleanOrNull == true
        )

        (held as? JsonPrimitive)?.booleanOrNull != null -> SchemaRule.Bound(
            limit = besideKeyword(error, inclusive)?.let(::written).orEmpty(),
            lower = lower,
            exclusive = true
        )

        else -> SchemaRule.Bound(limit = held?.let(::written).orEmpty(), lower = lower, exclusive = true)
    }
}

/** The kinds a `type` names: one, or a list. */
internal fun typesOf(held: JsonElement?): List<JsonType> = when (held) {
    is JsonArray -> held.mapNotNull { (it as? JsonPrimitive)?.content?.let(JsonType::named) }
    is JsonPrimitive -> listOfNotNull(JsonType.named(held.content))
    else -> emptyList()
}

/** The values an `enum` allows, each as JSON writes it. */
internal fun allowedOf(held: JsonElement?): List<String> = (held as? JsonArray)?.map(::written).orEmpty()

/** The keys the list [held] names that the object [value] has not. */
private fun missingOf(held: JsonElement?, value: JsonElement): List<String> {
    val has = (value as? JsonObject)?.keys.orEmpty()
    return (held as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }.filter { it !in has }
}

private fun JsonElement?.count(): Long =
    (this as? JsonPrimitive)?.longOrNull ?: (this as? JsonPrimitive)?.content?.toDoubleOrNull()?.toLong() ?: 0

private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.content.orEmpty()

private val FalseSchema = JsonPrimitive(false)
