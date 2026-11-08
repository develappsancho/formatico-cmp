package com.developersancho.formatico.core.schema

import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.DataBoolean
import com.developersancho.formatico.core.format.convert.DataDate
import com.developersancho.formatico.core.format.convert.DataNull
import com.developersancho.formatico.core.format.convert.DataNumber
import com.developersancho.formatico.core.format.convert.DataObject
import com.developersancho.formatico.core.format.convert.DataString
import com.developersancho.formatico.core.format.convert.DataValue
import com.developersancho.formatico.core.format.convert.jsonSpelled
import com.developersancho.formatico.core.format.json.PathStep
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.booleanOrNull

/**
 * [value] as kotlinx.serialization holds JSON, which the validator reads: a number spelled as JSON spells it, whatever
 * format it was read from, and a TOML date a string. A name given twice keeps its last value, as JSON's readers do.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun jsonOf(value: DataValue): JsonElement = when (value) {
    is DataObject -> JsonObject(LinkedHashMap<String, JsonElement>().apply { value.members.forEach { put(it.name, jsonOf(it.value)) } })
    is DataArray -> JsonArray(value.items.map(::jsonOf))
    is DataString -> JsonPrimitive(value.value)
    is DataNumber -> value.jsonSpelled?.let(::JsonUnquotedLiteral) ?: JsonPrimitive(value.written)
    is DataBoolean -> JsonPrimitive(value.value)
    DataNull -> JsonNull
    is DataDate -> JsonPrimitive(value.written)
}

private val WholeNumber = Regex("-?[0-9]+")

/** The kind of [element], as a schema's `type` names it: a number with no fraction an integer. */
internal fun typeOf(element: JsonElement): JsonType = when (element) {
    JsonNull -> JsonType.Null

    is JsonObject -> JsonType.Object

    is JsonArray -> JsonType.Array

    is JsonPrimitive -> when {
        element.isString -> JsonType.String
        element.booleanOrNull != null -> JsonType.Boolean
        WholeNumber.matches(element.content) -> JsonType.Integer
        else -> JsonType.Number
    }
}

/** The tokens of a JSON Pointer as RFC 6901 writes it, `/customer/phone`: `~1` read as `/` and `~0` as `~`. */
internal fun pointerTokens(pointer: String): List<String> =
    if (pointer.isEmpty()) emptyList() else pointer.removePrefix("/").split('/').map { it.replace("~1", "/").replace("~0", "~") }

/** The value [tokens] lead to from [root], or null where they lead to none. */
internal fun JsonElement.at(tokens: List<String>): JsonElement? =
    tokens.fold<String, JsonElement?>(this) { here, token ->
        when (here) {
            is JsonObject -> here[token]
            is JsonArray -> token.toIntOrNull()?.let(here::getOrNull)
            else -> null
        }
    }

/** [tokens] as the steps of a path from [root]: a token into an array its place, any other a key. */
internal fun stepsOf(tokens: List<String>, root: JsonElement): List<PathStep> {
    var here: JsonElement? = root
    return tokens.map { token ->
        val inArray = here as? JsonArray
        val index = inArray?.let { token.toIntOrNull() }
        here = here?.at(listOf(token))
        if (index != null) PathStep.Index(index) else PathStep.Key(token)
    }
}

/** [element] written as JSON, on one line, cut to [MOST_WRITTEN] characters with an ellipsis where it is longer. */
internal fun written(element: JsonElement): String {
    val text = element.toString()
    return if (text.length <= MOST_WRITTEN) text else text.take(MOST_WRITTEN - 1) + "…"
}

/** How long a value written in a problem's sentence may be. */
private const val MOST_WRITTEN = 40
