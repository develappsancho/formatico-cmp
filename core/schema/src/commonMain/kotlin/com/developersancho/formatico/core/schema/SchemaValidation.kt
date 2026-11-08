package com.developersancho.formatico.core.schema

import com.developersancho.formatico.core.format.convert.DataValue
import io.github.optimumcode.json.schema.ErrorCollector
import io.github.optimumcode.json.schema.FormatBehavior
import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.JsonSchemaLoader
import io.github.optimumcode.json.schema.SchemaOption
import io.github.optimumcode.json.schema.ValidationError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** How many problems a validation keeps: past them it counts. */
const val MOST_SCHEMA_PROBLEMS = 1000

/**
 * [value] validated against [schema]: by the draft its `$schema` names, 2020-12 where it names none, its `format`s
 * asserted as well as noted (a validator that let `"email": "nobody"` pass would surprise). A `$ref` to another schema is
 * looked for among [others], the open schemas, by their `$id`; no address is fetched. A schema that is not one of its
 * draft, names a draft Formatico does not know, or has a `$ref` to nothing cannot be used, and the outcome says why.
 */
fun validateAgainst(value: DataValue, schema: DataValue, others: List<DataValue> = emptyList()): SchemaOutcome {
    val schemaJson = jsonOf(schema)
    val needed = neededOf(schemaJson, others.map(::jsonOf))
    return when (val loaded = loaded(schemaJson, needed)) {
        is Loaded.Schema -> outcomeOf(jsonOf(value), loaded, SchemaDocuments(schemaJson, needed))
        is Loaded.Fault -> SchemaOutcome.Unusable(loaded.fault)
    }
}

/** [instance] validated against the schema [loaded], whose keywords [documents] hold. */
private fun outcomeOf(instance: JsonElement, loaded: Loaded.Schema, documents: SchemaDocuments): SchemaOutcome {
    val collected = Collected()
    loaded.schema.validate(instance, collected)
    if (collected.total == 0) return SchemaOutcome.Valid(loaded.draft)
    val problems = documents.problemsOf(collected.errors, instance)
    // The errors past those kept are counted as problems of their own: they are not grouped.
    return SchemaOutcome.Failed(loaded.draft, problems, problems.size + (collected.total - collected.errors.size))
}

/**
 * [schema] ready to validate with, by the draft its `$schema` names, 2020-12 where it names none, the [others] it names
 * known by their `$id`; or why it cannot be used.
 */
private fun loaded(schema: JsonElement, others: List<JsonElement>): Loaded {
    val named = (schema as? JsonObject)?.get("\$schema")
    val uri = (named as? JsonPrimitive)?.takeIf { it.isString }?.content
    val draft = if (named == null) SchemaDraft.Default else uri?.let(SchemaDraft::named)
    return when {
        draft == null -> Loaded.Fault(SchemaFault.UnknownDraft(uri ?: named.toString()))
        else -> notASchema(schema, draft)?.let { Loaded.Fault(it) } ?: load(schema, others, draft)
    }
}

/** Whether [value] looks like a JSON Schema: an object whose `$schema` names a draft, or that holds what schemas hold. */
fun looksLikeSchema(value: DataValue): Boolean {
    val schema = jsonOf(value) as? JsonObject ?: return false
    (schema["\$schema"] as? JsonPrimitive)?.let { named -> return named.isString && SchemaDraft.named(named.content) != null }
    val namesTypes = typesOf(schema["type"]).isNotEmpty()
    val holdsSchemas = listOf("properties", "\$defs", "definitions", "patternProperties").any { schema[it] is JsonObject }
    return holdsSchemas || (namesTypes && SchemaWords.any { it in schema })
}

/** The `$id` of [schema], without its `#`, by which the `$ref`s of other schemas name it; null where it has none. */
fun schemaIdOf(schema: DataValue): String? = idOf(jsonOf(schema))

/** The address a document's `$schema` names its schema by, without its `#`; null where it names none, or a draft. */
fun schemaNamedBy(document: DataValue): String? {
    val named = (jsonOf(document) as? JsonObject)?.get("\$schema") as? JsonPrimitive ?: return null
    return named.takeIf { it.isString && SchemaDraft.named(it.content) == null }?.content?.substringBefore('#')?.takeIf { it.isNotEmpty() }
}

/** What a schema that names its kinds holds beside its `type`. */
private val SchemaWords = listOf("required", "items", "enum", "const", "minimum", "maximum", "minLength", "maxLength", "pattern", "format")

/** What a validation collects: the first [MOST_SCHEMA_PROBLEMS] errors, and how many there were. */
private class Collected : ErrorCollector {
    val errors = ArrayList<ValidationError>()
    var total = 0
        private set

    override fun onError(error: ValidationError) {
        total++
        if (errors.size < MOST_SCHEMA_PROBLEMS) errors += error
    }
}

/** Why [schema] is not one of [draft], against the draft's own schema; null where it is one. */
private fun notASchema(schema: JsonElement, draft: SchemaDraft): SchemaFault.NotASchema? {
    val meta = JsonSchemaLoader.create().registerWellKnown(draft.type).fromDefinition("{\"\$ref\":\"${draft.uri}#\"}", draft.type)
    val collected = Collected()
    if (meta.validate(schema, collected)) return null
    val places = collected.errors.map { pointerTokens(it.objectPath.toString()) }.distinct()
    val deepest = places.maxBy { it.size }
    return SchemaFault.NotASchema(draft, stepsOf(deepest, schema), places.size)
}

/** What loading a schema came to. */
private sealed interface Loaded {
    data class Schema(val schema: JsonSchema, val draft: SchemaDraft) : Loaded

    data class Fault(val fault: SchemaFault) : Loaded
}

/** [schema] of [draft], ready to validate with, the [others] it names known by their `$id`. */
private fun load(schema: JsonElement, others: List<JsonElement>, draft: SchemaDraft): Loaded {
    val loader = JsonSchemaLoader.create().withSchemaOption(SchemaOption.FORMAT_BEHAVIOR_OPTION, FormatBehavior.ANNOTATION_AND_ASSERTION)
    others.forEach { other -> runCatching { loader.register(other, draft.type) } }
    return try {
        Loaded.Schema(loader.fromJsonElement(schema, draft.type), draft)
    } catch (unread: IllegalArgumentException) {
        Loaded.Fault(faultOf(unread.message.orEmpty(), schema))
    } catch (unread: IllegalStateException) {
        Loaded.Fault(SchemaFault.Unreadable(unread.message.orEmpty()))
    }
}

private val Unresolved = Regex("cannot resolve references: \\{\"([^\"]*)\": \\[\"([^\"]*)\"")

/** The fault the validator's [message] says loading [schema] failed of: a `$ref` to nothing, where it says so. */
private fun faultOf(message: String, schema: JsonElement): SchemaFault {
    val match = Unresolved.find(message) ?: return SchemaFault.Unreadable(message)
    val tokens = pointerTokens(match.groupValues[2]).let { if (it.lastOrNull() == "\$ref") it else it + "\$ref" }
    val written = (schema.at(tokens) as? JsonPrimitive)?.content ?: match.groupValues[1]
    return SchemaFault.UnresolvedReference(written, if (schema.at(tokens) != null) stepsOf(tokens, schema) else emptyList())
}
