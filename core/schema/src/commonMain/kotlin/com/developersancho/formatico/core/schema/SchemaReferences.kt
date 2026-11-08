package com.developersancho.formatico.core.schema

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The schemas of [others] that [schema] names by their `$id` in its `$ref`s, and those they name in theirs: only they
 * are loaded with it, so that another open schema's own faults are not its.
 */
internal fun neededOf(schema: JsonElement, others: List<JsonElement>): List<JsonElement> {
    val ownId = idOf(schema)
    val candidates = others.filter { other -> idOf(other).let { it != null && it != ownId } }
    val needed = ArrayList<JsonElement>()
    val left = ArrayDeque(listOf(schema))
    while (left.isNotEmpty()) {
        val refs = refsOf(left.removeFirst(), base = ownId)
        candidates.filter { it !in needed && idOf(it) in refs }.forEach {
            needed += it
            left += it
        }
    }
    return needed
}

/** The addresses the `$ref`s in [schema] name, without their `#`: a relative one resolved against [base]. */
private fun refsOf(schema: JsonElement, base: String?): Set<String> {
    val refs = HashSet<String>()
    val left = ArrayDeque(listOf(schema))
    while (left.isNotEmpty()) {
        val here = left.removeLast()
        (here as? JsonObject)?.let { holder -> refOf(holder, base)?.let(refs::add) }
        when (here) {
            is JsonObject -> left.addAll(here.values)
            is JsonArray -> left.addAll(here)
            else -> Unit
        }
    }
    return refs - ""
}

/** The address [holder]'s `$ref` names, without its `#`, resolved against [base]; null where it has none. */
private fun refOf(holder: JsonObject, base: String?): String? {
    val ref = holder["\$ref"] as? JsonPrimitive ?: return null
    return ref.takeIf { it.isString }?.let { resolved(it.content.substringBefore('#'), base) }
}

/** [ref] resolved against [base] where it is relative: `customer.json` beside `https://example.com/order.json`. */
private fun resolved(ref: String, base: String?): String = when {
    ref.isEmpty() || "://" in ref || ref.startsWith("urn:") || base == null || "://" !in base -> ref
    ref.startsWith("/") -> base.substringBefore("://") + "://" + base.substringAfter("://").substringBefore('/') + ref
    else -> base.substringBeforeLast('/') + "/" + ref
}
