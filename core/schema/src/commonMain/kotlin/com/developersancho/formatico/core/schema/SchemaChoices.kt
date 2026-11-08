package com.developersancho.formatico.core.schema

import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * [errors] of a value of [instance] as Formatico's problems, in the order the validator found them. The errors of an
 * `anyOf` or a `oneOf` that matched none of its schemas are one problem, said where it applies: the kinds its choices
 * allow, where each asks for a kind; the values they allow, where some ask for one; that it matches none of them, else.
 */
internal fun SchemaDocuments.problemsOf(errors: List<ValidationError>, instance: JsonElement): List<SchemaProblem> {
    val groups = LinkedHashMap<String, MutableList<ValidationError>>()
    errors.forEachIndexed { index, error ->
        val tokens = pointerTokens(error.schemaPath.toString())
        val branch = branchAt(tokens)
        val key = if (branch < 0) {
            "#$index"
        } else {
            val site = pointerTokens(error.objectPath.toString()).take(depthOf(tokens, branch))
            tokens.take(branch + 1).joinToString("/") + "|" + site.joinToString("/")
        }
        groups.getOrPut(key) { mutableListOf() } += error
    }
    return groups.values.map { group ->
        val tokens = pointerTokens(group.first().schemaPath.toString())
        val branch = branchAt(tokens)
        if (branch < 0) problemOf(group.single(), instance) else choicesProblemOf(group, tokens, branch, instance)
    }
}

/** The problem of an `anyOf` or a `oneOf`, standing at [branch] in [tokens], none of whose schemas [group] matched. */
private fun SchemaDocuments.choicesProblemOf(
    group: List<ValidationError>,
    tokens: List<String>,
    branch: Int,
    instance: JsonElement
): SchemaProblem {
    val site = pointerTokens(group.first().objectPath.toString()).take(depthOf(tokens, branch))
    val value = instance.at(site) ?: JsonNull
    val atSite = group.filter { pointerTokens(it.objectPath.toString()) == site }
    val keywords = atSite.map { pointerTokens(it.schemaPath.toString()).lastOrNull() }
    val allowing = atSite.filterIndexed { index, _ -> keywords[index] == "enum" || keywords[index] == "const" }
    val rule = when {
        atSite.size == group.size && keywords.all { it == "type" } -> SchemaRule.Type(atSite.flatMap { typesOf(keywordOf(it)) }.distinct())

        allowing.isNotEmpty() -> SchemaRule.Enum(
            allowing.flatMap { error ->
                val held = keywordOf(error)
                if (pointerTokens(error.schemaPath.toString()).last() == "enum") allowedOf(held) else listOfNotNull(held?.let(::written))
            }.distinct()
        )

        else -> SchemaRule.NoChoice(
            keyword = tokens[branch],
            choices = group.mapNotNull { pointerTokens(it.schemaPath.toString()).getOrNull(branch + 1) }.distinct().size
        )
    }
    return SchemaProblem(stepsOf(site, instance), rule, typeOf(value), scalarOf(value))
}

/** Where in a schema's path [tokens] the outermost `anyOf` or `oneOf` it stands under is, its choice after it; -1 for none. */
private fun branchAt(tokens: List<String>): Int =
    tokens.indices.firstOrNull { tokens[it] in Branches && tokens.getOrNull(it + 1)?.toIntOrNull() != null } ?: -1

/**
 * How many levels into the value a schema's path [tokens] goes before [end]: each keyword that applies a schema to a
 * member or an item, one; a name or a place after a keyword is no keyword of its own.
 */
private fun depthOf(tokens: List<String>, end: Int): Int {
    var depth = 0
    var index = 0
    while (index < end) {
        when (tokens[index]) {
            in Named -> {
                depth++
                index++
            }

            "items" -> {
                depth++
                if (index + 1 < end && tokens[index + 1].toIntOrNull() != null) index++
            }

            in Descending -> depth++

            in Holding -> index++
        }
        index++
    }
    return depth
}

private val Branches = setOf("anyOf", "oneOf")

/** The keywords that apply a schema to a member or an item named or placed after them. */
private val Named = setOf("properties", "patternProperties", "prefixItems")

/** The keywords that apply their schema to members or items not named. */
private val Descending = setOf("additionalProperties", "additionalItems", "contains", "unevaluatedProperties", "unevaluatedItems")

/** The keywords whose schemas stand under a name or a place that is not the value's. */
private val Holding = setOf("\$defs", "definitions", "dependentSchemas", "dependencies", "allOf", "anyOf", "oneOf")
