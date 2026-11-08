package com.developersancho.formatico.core.schema

import com.developersancho.formatico.core.format.json.PathStep

/**
 * A way a value fails its schema.
 *
 * @property at the path of the value, from the document's root.
 * @property rule what the schema asks of it, and it is not.
 * @property type the kind of the value.
 * @property value a single value as JSON writes it, `null`, `0`, `"EUR"`, cut where it is long; null for an object or
 *   an array.
 */
data class SchemaProblem(val at: List<PathStep>, val rule: SchemaRule, val type: JsonType, val value: String?)

/**
 * What a schema asks of a value, as a problem says it: one of JSON Schema's keywords, read with what it holds. A value
 * written in one (a bound, a constant, the values of an `enum`) is written as JSON writes it.
 */
sealed interface SchemaRule {
    /** It must be of one of [expected]: `type`. */
    data class Type(val expected: List<JsonType>) : SchemaRule

    /** It must have the keys [missing], which it has not: `required`. */
    data class Required(val missing: List<String>) : SchemaRule

    /** Having [property], it must have the keys [missing] too: `dependentRequired`, `dependencies`. */
    data class DependentRequired(val property: String, val missing: List<String>) : SchemaRule

    /** It must be one of [allowed]: `enum`. */
    data class Enum(val allowed: List<String>) : SchemaRule

    /** It must be [expected]: `const`. */
    data class Const(val expected: String) : SchemaRule

    /**
     * It must be at least [limit], where [lower] says so, or at most; past it where [exclusive] says so: `minimum`,
     * `maximum`, `exclusiveMinimum`, `exclusiveMaximum`.
     */
    data class Bound(val limit: String, val lower: Boolean, val exclusive: Boolean) : SchemaRule

    /** It must be a multiple of [factor]: `multipleOf`. */
    data class MultipleOf(val factor: String) : SchemaRule

    /** It must be at least [limit] characters long, where [lower] says so, or at most: `minLength`, `maxLength`. */
    data class Length(val limit: Long, val lower: Boolean) : SchemaRule

    /** It must match [pattern], a regular expression: `pattern`. */
    data class Pattern(val pattern: String) : SchemaRule

    /** It must be written as [format] says: `format`, `date-time`, `email`, `uri`. */
    data class Format(val format: String) : SchemaRule

    /** It must hold at least [limit] items, where [lower] says so, or at most: `minItems`, `maxItems`. */
    data class ItemCount(val limit: Long, val lower: Boolean) : SchemaRule

    /** Its items must differ from each other: `uniqueItems`. */
    data object UniqueItems : SchemaRule

    /** It must hold an item that matches a schema of its own: `contains`, `minContains`, `maxContains`. */
    data object Contains : SchemaRule

    /** It must have at least [limit] keys, where [lower] says so, or at most: `minProperties`, `maxProperties`. */
    data class KeyCount(val limit: Long, val lower: Boolean) : SchemaRule

    /** It must not be there: a `false` schema, as `additionalProperties: false` makes a key not named. */
    data object NotAllowed : SchemaRule

    /** It must not match a schema: `not`. */
    data object Not : SchemaRule

    /** It must match one of [choices] schemas, and matches none: `anyOf` or `oneOf`, the [keyword]. */
    data class NoChoice(val keyword: String, val choices: Int) : SchemaRule

    /** It must match exactly one of its schemas, and matches more: `oneOf`. */
    data object ManyChoices : SchemaRule

    /** It fails a [keyword] Formatico has no words of its own for. */
    data class Other(val keyword: String) : SchemaRule
}

/** Why a schema cannot be used. */
sealed interface SchemaFault {
    /** Its `$schema` names [uri], which is no draft Formatico knows. */
    data class UnknownDraft(val uri: String) : SchemaFault

    /** A `$ref` at [at] in the schema names [ref], which no schema holds: neither the schema nor another open one. */
    data class UnresolvedReference(val ref: String, val at: List<PathStep>) : SchemaFault

    /**
     * It is not a schema of its [draft]: what it holds [at] is not what the draft's own schema allows there, and so in
     * [places] places in all.
     */
    data class NotASchema(val draft: SchemaDraft, val at: List<PathStep>, val places: Int) : SchemaFault

    /** The validator could not read it, for [reason], in its own words. */
    data class Unreadable(val reason: String) : SchemaFault
}

/** What validating a value against a schema came to. */
sealed interface SchemaOutcome {
    /** The value is what the schema of [draft] asks for. */
    data class Valid(val draft: SchemaDraft) : SchemaOutcome

    /** It is not: [problems], the first of [total], in the order the schema finds them. */
    data class Failed(val draft: SchemaDraft, val problems: List<SchemaProblem>, val total: Int) : SchemaOutcome

    /** The schema cannot be used, for [fault]. */
    data class Unusable(val fault: SchemaFault) : SchemaOutcome
}
