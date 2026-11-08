package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.schema.JsonType
import com.developersancho.formatico.core.schema.SchemaProblem
import com.developersancho.formatico.core.schema.SchemaRule
import com.developersancho.formatico.feature.workspace.strings.SchemaStrings

/** How many of the values an `enum` allows a sentence names: past them, "…". */
private const val MOST_NAMED_VALUES = 6

/**
 * What [problem] says: the value's name, what the schema asks of it, and what it is. A value that is no single one is
 * said to be an object or an array.
 */
internal fun SchemaStrings.sentenceOf(problem: SchemaProblem): String {
    val name = nameOf(problem.at)
    val actual = actualOf(problem)
    return when (val rule = problem.rule) {
        is SchemaRule.Type -> type(name, eitherOf(rule.expected.map(::kindOf)), actual)
        is SchemaRule.Required -> requiredOf(name, rule.missing)
        is SchemaRule.DependentRequired -> dependentRequired(name, rule.property, bothOf(rule.missing))
        is SchemaRule.Enum -> enumeration(name, eitherOf(namedValues(rule.allowed)), actual)
        is SchemaRule.Const -> constant(name, rule.expected, actual)
        is SchemaRule.Bound -> bound(name, boundOf(rule), actual)
        is SchemaRule.MultipleOf -> multipleOf(name, rule.factor, actual)
        is SchemaRule.Length -> lengthOf(name, rule, actual)
        is SchemaRule.Pattern -> pattern(name, rule.pattern, actual)
        is SchemaRule.Format -> format(name, rule.format, actual)
        is SchemaRule.ItemCount -> itemsOf(name, rule)
        SchemaRule.UniqueItems -> uniqueItems(name)
        SchemaRule.Contains -> contains(name)
        is SchemaRule.KeyCount -> keysOf(name, rule)
        SchemaRule.NotAllowed -> notAllowed(name)
        SchemaRule.Not -> not(name)
        is SchemaRule.NoChoice -> noChoice(name, rule.choices, rule.keyword)
        SchemaRule.ManyChoices -> manyChoices(name)
        is SchemaRule.Other -> other(name, rule.keyword)
    }
}

/** The name of the value at [path]: its key, an item by its array's name and its place, or the document itself. */
internal fun SchemaStrings.nameOf(path: List<PathStep>): String = when (val last = path.lastOrNull()) {
    null -> theDocument
    is PathStep.Key -> last.name
    is PathStep.Index -> ((path.getOrNull(path.size - 2) as? PathStep.Key)?.name.orEmpty()) + "[${last.index}]"
}

/** What [problem]'s value is: a single value as JSON writes it, or an object or an array. */
private fun SchemaStrings.actualOf(problem: SchemaProblem): String = problem.value ?: when (problem.type) {
    JsonType.Array -> anArray
    else -> anObject
}

/** A kind of value in words: "a string", "an integer". */
private fun SchemaStrings.kindOf(type: JsonType): String = when (type) {
    JsonType.Null -> nullValue
    JsonType.Boolean -> trueOrFalse
    JsonType.Integer -> anInteger
    JsonType.Number -> aNumber
    JsonType.String -> aString
    JsonType.Array -> anArray
    JsonType.Object -> anObject
}

/** [items] as one or another: "a", "a or b", "a, b or c". */
private fun SchemaStrings.eitherOf(items: List<String>): String =
    if (items.size < 2) items.firstOrNull().orEmpty() else either(items.dropLast(1).joinToString(", "), items.last())

/** [items] all together: "a", "a and b", "a, b and c". */
internal fun SchemaStrings.bothOf(items: List<String>): String =
    if (items.size < 2) items.firstOrNull().orEmpty() else both(items.dropLast(1).joinToString(", "), items.last())

/** The values an `enum` allows, the first [MOST_NAMED_VALUES] of them and "…" for the rest. */
private fun namedValues(values: List<String>): List<String> =
    if (values.size <= MOST_NAMED_VALUES) values else values.take(MOST_NAMED_VALUES) + "…"
