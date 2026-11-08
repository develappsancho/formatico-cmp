package com.developersancho.formatico.core.format.types

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
import com.developersancho.formatico.core.format.convert.isInteger
import com.developersancho.formatico.core.format.convert.numberParts
import com.developersancho.formatico.core.format.text.wholeRegex

/**
 * What the values met at one place of a document are, merged: how many of each kind, and for objects and arrays, the
 * shapes of their members and of their items. A shape is built a value at a time, by [add].
 */
internal class Shape {
    /** How many values were added. */
    var count = 0
        private set

    /** Whether a null was among them. */
    var nulls = false
        private set

    /** How many strings were among them, TOML's dates counted. */
    var strings = 0
        private set

    /** How many of the [strings] are an ISO 8601 date, time or date-time, TOML's dates counted. */
    var dates = 0
        private set

    /** The widest kind of number among them, or null where there was none. */
    var numeral: Numeral? = null
        private set

    /** Whether `true` or `false` was among them. */
    var booleans = false
        private set

    /** How many objects were among them. */
    var objects = 0
        private set

    /** The shape of each name the [objects] have, in the order the names were first met. */
    val members = LinkedHashMap<String, Shape>()

    /** How many arrays were among them. */
    var arrays = 0
        private set

    private var itemShape: Shape? = null

    /** The shape of the items of every one of the [arrays]; one of no values where every array was empty. */
    val items: Shape get() = itemShape ?: Shape().also { itemShape = it }

    /** How many kinds of value other than null were met: more than one, and the values are of no one type. */
    val kinds: Int get() = listOf(strings > 0, numeral != null, booleans, objects > 0, arrays > 0).count { it }

    /** Whether the values are objects, nulls among them or not, and nothing else. */
    val isRecords: Boolean get() = objects > 0 && kinds == 1

    /** Merges [value] in. */
    fun add(value: DataValue) {
        count++
        when (value) {
            is DataObject -> addObject(value)

            is DataArray -> {
                arrays++
                value.items.forEach(items::add)
            }

            is DataString -> addString(date = isIsoDate(value.value))

            is DataDate -> addString(date = true)

            is DataNumber -> {
                val kind = numeralOf(value.written)
                numeral = numeral?.let { maxOf(it, kind) } ?: kind
            }

            is DataBoolean -> booleans = true

            DataNull -> nulls = true
        }
    }

    /** Merges [value]'s members in, a name given twice by its first member alone. */
    private fun addObject(value: DataObject) {
        objects++
        val seen = HashSet<String>()
        value.members.forEach { member ->
            if (seen.add(member.name)) members.getOrPut(member.name) { Shape() }.add(member.value)
        }
    }

    private fun addString(date: Boolean) {
        strings++
        if (date) dates++
    }
}

/** The kinds of number, from the narrowest: a number of two kinds is of the wider. */
internal enum class Numeral(val type: ValueType) {
    /** A whole number 32 bits hold. */
    Whole(ValueType.Whole),

    /** A whole number 64 bits hold. */
    LongWhole(ValueType.LongWhole),

    /** Any other: a fraction, an exponent, an infinity, a whole number past 64 bits. */
    Decimal(ValueType.Decimal)
}

/** The kind of number [written] is, as its format spells it: `129.90`, `0x1F`, `1_000`, `.inf`, `1e3`. */
internal fun numeralOf(written: String): Numeral = when (val parts = numberParts(written)) {
    is NumberParts.Decimal -> if (parts.isInteger) wholeNumeral(if (parts.negative) "-${parts.whole}" else parts.whole) else Numeral.Decimal
    is NumberParts.Radix -> parts.inTens()?.let(::wholeNumeral) ?: Numeral.Decimal
    else -> Numeral.Decimal
}

/** The kind of the whole number [tens] writes in tens, signed. */
private fun wholeNumeral(tens: String): Numeral {
    val value = tens.toLongOrNull() ?: return Numeral.Decimal
    return if (value in Int.MIN_VALUE..Int.MAX_VALUE) Numeral.Whole else Numeral.LongWhole
}

private const val DATE = "[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])"
private const val TIME = "([01][0-9]|2[0-3]):[0-5][0-9](:([0-5][0-9]|60)([.,][0-9]+)?)?"
private const val ZONE = "([Zz]|[+-]([01][0-9]|2[0-3])(:?[0-5][0-9])?)"

/**
 * A date, a time or a date and a time, as ISO 8601 writes them in its extended form: `2026-11-04`, `14:32`,
 * `14:32:00Z`, `2026-10-28T14:32:00.5+03:00`; the `T` may be a space or in lower case, as RFC 3339 allows.
 */
private val IsoDate = wholeRegex("$DATE([Tt ]$TIME$ZONE?)?|$TIME$ZONE?")

/** Whether [text] is an ISO 8601 date, time or date-time. */
internal fun isIsoDate(text: String): Boolean = IsoDate.containsMatchIn(text)
