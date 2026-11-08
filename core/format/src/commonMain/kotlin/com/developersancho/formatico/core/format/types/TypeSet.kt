package com.developersancho.formatico.core.format.types

/** A type to write: its [name] and the [record] it declares. */
internal data class Declaration(val name: String, val record: ValueType.Record)

/**
 * The types written of a document, each record once, named, in the order they are written: breadth first from the
 * root, each type's nested types in the order of its members.
 */
internal class TypeSet private constructor(private val names: Map<ValueType.Record, String>) {
    /** Every type, in the order it is written. */
    val declarations: List<Declaration> = names.map { (record, name) -> Declaration(name, record) }

    /** The name of the type [record] is written as. */
    fun nameOf(record: ValueType.Record): String = names.getValue(record)

    companion object {
        /**
         * The types of [root], named [rootName], and of every record in it, named in [language] after the member
         * that holds it: a record met again is the type it was first named, and a name met again for another record
         * gets 2, 3, … after it. A name [language] already gives a type of its own (Kotlin's `String`, Swift's `Int`)
         * is one met again.
         */
        fun of(root: ValueType.Record, rootName: String, language: TypeLanguage): TypeSet {
            val capitalise = language.capitalise
            val names = LinkedHashMap<ValueType.Record, String>()
            val taken = language.reservedTypeNames.toHashSet()
            val queue = ArrayDeque<Pair<ValueType.Record, String>>()
            queue.addLast(root to rootName)
            while (queue.isNotEmpty()) {
                val (record, wanted) = queue.removeFirst()
                if (record in names) continue
                names[record] = uniqueName(wanted, taken)
                record.members.forEach { member ->
                    member.type.record?.let { nested -> queue.addLast(nested to nestedTypeName(member, capitalise)) }
                }
            }
            return TypeSet(names)
        }
    }
}

/** How [this] language writes a word of a type's name: Go writes its initialisms in capitals (`URL`, `ID`). */
private val TypeLanguage.capitalise: (String) -> String
    get() = if (this == TypeLanguage.Go) ::goCapitalised else ::capitalised

/** The names [this] language gives types of its own, which the types written in it must not take. */
private val TypeLanguage.reservedTypeNames: Set<String>
    get() = when (this) {
        TypeLanguage.Kotlin -> KotlinTypeNames
        TypeLanguage.Swift -> SwiftTypeNames
        TypeLanguage.TypeScript, TypeLanguage.Go -> emptySet()
    }

/**
 * The name the record [member] holds is wanted under: an object's, the member's name ("shipping_address" →
 * `ShippingAddress`); an array's items', the member's name singularised ("items" → `Item`), or with "Item" after it
 * where it is no plural ("status" → `StatusItem`).
 */
private fun nestedTypeName(member: Member, capitalise: (String) -> String): String {
    val words = wordsOf(member.name)
    if (member.type is ValueType.Record) return pascalName(words, capitalise) ?: VALUE
    val one = words.lastOrNull()?.let(::singular)
    return pascalName(if (one != null) words.dropLast(1) + one else words + ITEM, capitalise) ?: ITEM
}

/**
 * [wanted], or where [taken] already holds it, the first of it with 2, 3, … after it that [taken] does not hold; the
 * name is added to [taken].
 */
internal fun uniqueName(wanted: String, taken: MutableSet<String>): String {
    var name = wanted
    var suffix = 2
    while (!taken.add(name)) name = wanted + suffix++
    return name
}

/** [names], each one met again with 2, 3, … after it, as [uniqueName] makes it. */
internal fun uniqueNames(names: List<String>): List<String> {
    val taken = HashSet<String>()
    return names.map { uniqueName(it, taken) }
}

/**
 * The property name Kotlin and Swift give the member named [jsonName]: its words in lower camel case
 * ("first_name" → `firstName`, "Content-Type" → `contentType`), `_` before a name that starts with a digit
 * (`_2fa`), and `field` where it has no letters or digits.
 */
internal fun propertyName(jsonName: String): String {
    val name = camelName(wordsOf(jsonName))
    return when {
        name.isEmpty() -> FIELD
        name.first().isDigit() -> "_$name"
        else -> name
    }
}

/** The name of an object's type where its member's name has no letters or digits. */
private const val VALUE = "Value"

/** What goes after the name of a member that is no plural, for its items' type. */
private const val ITEM = "Item"

/** A property's name where its member's name has no letters or digits. */
private const val FIELD = "field"
