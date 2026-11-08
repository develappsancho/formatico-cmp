package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.schema.SchemaFault
import com.developersancho.formatico.core.schema.SchemaRule
import com.developersancho.formatico.feature.workspace.strings.SchemaStrings

/** That [name] lacks the keys [missing], which the schema requires: one, or all of them together. */
internal fun SchemaStrings.requiredOf(name: String, missing: List<String>): String =
    if (missing.size == 1) requiredOne(name, missing.single()) else requiredMany(name, bothOf(missing))

/** A bound in words: "at least 1", "more than 0", "at most 9", "less than 9". */
internal fun SchemaStrings.boundOf(rule: SchemaRule.Bound): String = when {
    rule.lower && rule.exclusive -> moreThan(rule.limit)
    rule.lower -> atLeast(rule.limit)
    rule.exclusive -> lessThan(rule.limit)
    else -> atMost(rule.limit)
}

/** That [name] must be so many characters long, at least or at most, and is [actual]. */
internal fun SchemaStrings.lengthOf(name: String, rule: SchemaRule.Length, actual: String): String {
    val count = countOf(rule.limit)
    return if (rule.lower) minLength(count, name, rule.limit.toString(), actual) else maxLength(count, name, rule.limit.toString(), actual)
}

/** That [name] must hold so many items, at least or at most. */
internal fun SchemaStrings.itemsOf(name: String, rule: SchemaRule.ItemCount): String =
    if (rule.lower) {
        minItems(
            countOf(rule.limit),
            name,
            rule.limit.toString()
        )
    } else {
        maxItems(countOf(rule.limit), name, rule.limit.toString())
    }

/** That [name] must have so many keys, at least or at most. */
internal fun SchemaStrings.keysOf(name: String, rule: SchemaRule.KeyCount): String =
    if (rule.lower) minKeys(countOf(rule.limit), name, rule.limit.toString()) else maxKeys(countOf(rule.limit), name, rule.limit.toString())

/** [limit] as the number a sentence's form follows. */
private fun countOf(limit: Long): Int = limit.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

/** Why the schema [check] names cannot be used, for [document], as its alert says it. */
internal fun SchemaStrings.faultOf(check: SchemaCheck.Unusable, document: String): String = when (val fault = check.fault) {
    is SchemaFault.UnknownDraft -> unknownDraft(fault.uri)

    is SchemaFault.UnresolvedReference -> check.line?.let { unresolvedAt(it, fault.ref, document) } ?: unresolved(fault.ref, document)

    is SchemaFault.NotASchema -> check.line?.let { notASchemaAt(it, nameOf(fault.at), fault.draft.displayName, document) }
        ?: notASchema(fault.draft.displayName, document)

    is SchemaFault.Unreadable -> unreadable(fault.reason)
}
