package com.developersancho.formatico.core.format.diff

import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.DataBoolean
import com.developersancho.formatico.core.format.convert.DataDate
import com.developersancho.formatico.core.format.convert.DataNull
import com.developersancho.formatico.core.format.convert.DataNumber
import com.developersancho.formatico.core.format.convert.DataObject
import com.developersancho.formatico.core.format.convert.DataString
import com.developersancho.formatico.core.format.convert.DataValue
import com.developersancho.formatico.core.format.convert.NumberParts
import com.developersancho.formatico.core.format.convert.inTens
import com.developersancho.formatico.core.format.convert.numberParts

/**
 * Whether [a] and [b] mean the same: objects with the same members, whatever their order, a name given twice matched
 * by its place among those of its name; arrays with the same items in the same order; strings with the same text, a
 * TOML date as a string; numbers that are the same number, however they are written.
 */
internal fun sameMeaning(a: DataValue, b: DataValue): Boolean = when {
    a is DataObject && b is DataObject -> a.members.size == b.members.size && sameMembers(a, b)
    a is DataArray && b is DataArray -> a.items.size == b.items.size && a.items.indices.all { sameMeaning(a.items[it], b.items[it]) }
    else -> sameScalar(a, b)
}

private fun sameMembers(a: DataObject, b: DataObject): Boolean {
    val byName = b.members.groupBy({ it.name }, { it.value })
    val seen = HashMap<String, Int>()
    return a.members.all { member ->
        val place = seen[member.name] ?: 0
        seen[member.name] = place + 1
        byName[member.name]?.getOrNull(place)?.let { sameMeaning(member.value, it) } == true
    }
}

/** Whether [a] and [b], neither an object nor an array, mean the same. */
internal fun sameScalar(a: DataValue, b: DataValue): Boolean = when {
    a is DataNumber && b is DataNumber -> canonicalNumber(a.written) == canonicalNumber(b.written)
    a is DataBoolean && b is DataBoolean -> a.value == b.value
    a == DataNull && b == DataNull -> true
    else -> textOf(a)?.let { it == textOf(b) } == true
}

private fun textOf(value: DataValue): String? = when (value) {
    is DataString -> value.value
    is DataDate -> value.written
    else -> null
}

/**
 * The number [written] stands for, written one way whatever way it was written: its digits with no nought at either
 * end, and the power of ten they are multiplied by, `12990e-2` for `129.90` and `129.9` alike; an integer in another
 * base in tens. A spelling that is no number is itself.
 */
internal fun canonicalNumber(written: String): String {
    val parts = numberParts(written) ?: return written
    return when (parts) {
        is NumberParts.Radix -> parts.inTens()?.let { canonicalNumber(it) } ?: written
        is NumberParts.Decimal -> decimal(parts) ?: written
        is NumberParts.Infinite -> if (parts.negative) "-inf" else "inf"
        NumberParts.NotANumber -> "nan"
    }
}

private fun decimal(parts: NumberParts.Decimal): String? {
    val fraction = parts.fraction.orEmpty()
    val power = parts.exponent?.let { it.toIntOrNull() ?: return null } ?: 0
    var exponent = power - fraction.length
    var digits = (parts.whole + fraction).trimStart('0')
    if (digits.isEmpty()) return "0"
    val trimmed = digits.trimEnd('0')
    exponent += digits.length - trimmed.length
    digits = trimmed
    return "${if (parts.negative) "-" else ""}${digits}e$exponent"
}
