package com.developersancho.formatico.core.format.types

/** What a value is typed as, in any language: what the writers write a type of. */
internal sealed interface ValueType {
    /** A string, TOML's dates among them. */
    data object Text : ValueType

    /** A whole number 32 bits hold. */
    data object Whole : ValueType

    /** A whole number 64 bits hold. */
    data object LongWhole : ValueType

    /** Any other number. */
    data object Decimal : ValueType

    /** `true` or `false`. */
    data object Truth : ValueType

    /** Values of more than one kind, or the items of arrays that were empty every time: the language's "anything". */
    data object Anything : ValueType

    /** Values that were null every time: their type cannot be told. */
    data object OnlyNull : ValueType

    /** An object, its [members] in the order they were first met: two records of the same members are one type. */
    data class Record(val members: List<Member>) : ValueType

    /** An array of [item]s, each of which may be null where [nullableItems] says so. */
    data class ListOf(val item: ValueType, val nullableItems: Boolean) : ValueType
}

/**
 * A member of a [ValueType.Record]: its JSON [name], its [type], whether some objects lack it ([optional]) and
 * whether it is null in some ([nullable]; always so where its type is [ValueType.OnlyNull]).
 */
internal data class Member(val name: String, val type: ValueType, val optional: Boolean, val nullable: Boolean)

/** Whether the type says nothing of its values: [ValueType.Anything] or [ValueType.OnlyNull]. */
internal val ValueType.isUnknown: Boolean get() = this == ValueType.Anything || this == ValueType.OnlyNull

/** The record this type is, or the one its lists hold, however deep; null where it is or holds none. */
internal val ValueType.record: ValueType.Record?
    get() = when (this) {
        is ValueType.Record -> this
        is ValueType.ListOf -> item.record
        else -> null
    }

/** What the values merged into this shape are typed as. */
internal fun Shape.type(): ValueType {
    val number = numeral
    return when {
        kinds > 1 -> ValueType.Anything
        strings > 0 -> ValueType.Text
        number != null -> number.type
        booleans -> ValueType.Truth
        objects > 0 -> record()
        arrays > 0 -> ValueType.ListOf(items.type(), nullableItems = items.nulls)
        nulls -> ValueType.OnlyNull
        else -> ValueType.Anything
    }
}

/** The record of the objects merged into this shape: a member that some of them lack is optional. */
internal fun Shape.record(): ValueType.Record = ValueType.Record(
    members.map { (name, member) -> Member(name, member.type(), optional = member.count < objects, nullable = member.nulls) }
)
