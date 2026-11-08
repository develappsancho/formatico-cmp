package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Immutable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.DataValue
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.core.format.text.TextRange
import com.developersancho.formatico.core.schema.SchemaDraft
import com.developersancho.formatico.core.schema.SchemaFault
import com.developersancho.formatico.core.schema.SchemaOutcome
import com.developersancho.formatico.core.schema.SchemaProblem
import com.developersancho.formatico.core.schema.looksLikeSchema
import com.developersancho.formatico.core.schema.schemaIdOf
import com.developersancho.formatico.core.schema.schemaNamedBy
import com.developersancho.formatico.core.schema.validateAgainst

/**
 * Which schema a document is validated against, as its view keeps it.
 *
 * @property with the id of the open document that is its schema, or null where none is chosen: then the open schema
 *   whose `$id` the document's `$schema` names, where there is one.
 */
data class SchemaChoice(val with: Long? = null)

/** An open document that is a JSON Schema, which the Against menu offers: its [id], its [name] and its `$id`. */
internal data class OfferedSchema(val id: Long, val name: String?, val schemaId: String?)

/** What validating a document against a schema came to. */
internal sealed interface SchemaCheck {
    /** It could not be read: it is plain text, or nothing of it could be read. */
    data object Unread : SchemaCheck

    /** It has [errors]: an invalid document is repaired before it is validated. */
    data class Invalid(val format: Format, val errors: Int) : SchemaCheck

    /** It is of a [format] whose values are all text, which a schema does not validate: CSV, properties, XML. */
    data class Unsupported(val format: Format) : SchemaCheck

    /** No schema is chosen. [named] is the address its `$schema` names a schema by, which no open schema has. */
    data class NoSchema(val named: String?) : SchemaCheck

    /** The [schema] chosen cannot be used, for [fault]; [line] is where in the schema, where it is known. */
    data class Unusable(val schema: String, val fault: SchemaFault, val line: Int?) : SchemaCheck

    /** It is what the [schema], of [draft], asks for. */
    data class Valid(val schema: String, val draft: SchemaDraft) : SchemaCheck

    /** It is not: [issues], in the order of the text, the first of [total]. */
    @Immutable
    data class Failed(val schema: String, val draft: SchemaDraft, val issues: List<SchemaIssue>, val total: Int) : SchemaCheck
}

/**
 * A problem of a document against its schema, in its text: the [problem]; the tree's [key] of its value; the [range]
 * its text marks, the key of an object or an array and a single value itself; and the [line] and [column] it starts at.
 */
internal data class SchemaIssue(val problem: SchemaProblem, val key: String?, val range: TextRange?, val line: Int?, val column: Int?)

/** The issues a check holds, where it failed; none otherwise. */
internal val SchemaCheck?.issues: List<SchemaIssue> get() = (this as? SchemaCheck.Failed)?.issues.orEmpty()

/** The formats a document is validated in: those whose values are a tree as JSON's are. */
internal val SchemaFormats = setOf(Format.Json, Format.Jsonc, Format.Json5, Format.Ndjson, Format.Yaml, Format.Toml)

/** How many issues the pane lists: past them it counts. */
internal const val MOST_LISTED_ISSUES = 200

/** The most a document may weigh to be looked at as a schema: a schema is small, and every open document is looked at. */
private const val MOST_SCHEMA_CHARS = 512 * 1024

/** An open document read as a schema: its [id], [name], [value], and the [root] and [text] its places are found in. */
internal class SchemaDocument(val id: Long, val name: String?, val value: DataValue, val root: ValueNode?, val text: String) {
    /** Its `$id`, by which a document's `$schema` and the `$ref`s of other schemas name it. */
    val schemaId: String? by lazy { schemaIdOf(value) }
}

/** [document], read as [reading], as a schema: where it is a JSON Schema, of a format a schema is written in. */
internal fun schemaDocumentOf(document: OpenDocument, reading: Reading): SchemaDocument? {
    if (document.text.length > MOST_SCHEMA_CHARS) return null
    val read = reading as? Reading.Read ?: return null
    val root = read.root ?: return null
    if (read.format !in SchemaFormats || read.errors > 0) return null
    val value = dataValueOf(root, read.format)
    return if (looksLikeSchema(value)) SchemaDocument(document.id, document.name, value, root, document.text) else null
}

/**
 * The schema a document whose value is [value] is validated against: the one its [choice] names among [schemas], or
 * where it names none, the one whose `$id` the document's `$schema` names.
 */
internal fun chosenSchema(choice: SchemaChoice, value: DataValue?, schemas: List<SchemaDocument>): SchemaDocument? {
    choice.with?.let { id -> return schemas.firstOrNull { it.id == id } }
    val named = value?.let(::schemaNamedBy) ?: return null
    return schemas.firstOrNull { it.schemaId == named }
}

/** What validating a document came to, and the schema it was validated against: the one chosen, or its `$schema`'s. */
internal data class SchemaValidation(val check: SchemaCheck, val schema: SchemaDocument?)

/**
 * [document], read as [reading], validated against the schema [choice] names among the open [schemas], the others known
 * by their `$id`: each line of NDJSON on its own, as each line is a value of its own.
 */
internal fun schemaCheckOf(
    document: OpenDocument,
    reading: Reading,
    choice: SchemaChoice,
    schemas: List<SchemaDocument>
): SchemaValidation {
    val read = reading as? Reading.Read ?: return SchemaValidation(SchemaCheck.Unread, null)
    val root = read.root ?: return SchemaValidation(SchemaCheck.Unread, null)
    if (read.errors > 0) return SchemaValidation(SchemaCheck.Invalid(read.format, read.errors), null)
    if (read.format !in SchemaFormats) return SchemaValidation(SchemaCheck.Unsupported(read.format), null)
    val value = dataValueOf(root, read.format)
    val schema = chosenSchema(choice, value, schemas)
    if (schema == null) {
        val named = schemaNamedBy(value)?.takeIf { address -> schemas.none { it.schemaId == address } }
        return SchemaValidation(SchemaCheck.NoSchema(named), null)
    }
    return SchemaValidation(validated(document, read, value, schema, schemas), schema)
}

/** [document], read as [read] to [value], validated against [schema], the [others] known by their `$id`. */
private fun validated(
    document: OpenDocument,
    read: Reading.Read,
    value: DataValue,
    schema: SchemaDocument,
    others: List<SchemaDocument>
): SchemaCheck {
    val root = checkNotNull(read.root)
    val name = schema.name ?: "—"
    val known = others.filter { it.id != schema.id }.map { it.value }
    val outcome = if (read.format == Format.Ndjson && value is DataArray) {
        eachLine(value, schema.value, known)
    } else {
        validateAgainst(value, schema.value, known)
    }
    return when (outcome) {
        is SchemaOutcome.Valid -> SchemaCheck.Valid(name, outcome.draft)
        is SchemaOutcome.Failed -> SchemaCheck.Failed(name, outcome.draft, issuesOf(outcome.problems, root, document.text), outcome.total)
        is SchemaOutcome.Unusable -> SchemaCheck.Unusable(name, outcome.fault, lineOf(outcome.fault, schema))
    }
}

/** Each line of an NDJSON document, [lines], validated against [schema]: a problem's path starts at its line. */
private fun eachLine(lines: DataArray, schema: DataValue, others: List<DataValue>): SchemaOutcome {
    val problems = ArrayList<SchemaProblem>()
    var total = 0
    var draft = SchemaDraft.Default
    lines.items.forEachIndexed { index, line ->
        when (val outcome = validateAgainst(line, schema, others)) {
            is SchemaOutcome.Unusable -> return outcome

            is SchemaOutcome.Valid -> draft = outcome.draft

            is SchemaOutcome.Failed -> {
                draft = outcome.draft
                total += outcome.total
                outcome.problems.forEach { problems += it.copy(at = listOf(PathStep.Index(index)) + it.at) }
            }
        }
    }
    return if (total == 0) SchemaOutcome.Valid(draft) else SchemaOutcome.Failed(draft, problems, total)
}

/** [problems] placed in the [text] [root] was read from, in the order of the text; those with no place last. */
private fun issuesOf(problems: List<SchemaProblem>, root: ValueNode, text: String): List<SchemaIssue> {
    val lines = LineMap(text)
    return problems.map { problem ->
        val node = nodeAtPath(root, problem.at)
        val range = node?.let { markOf(it, text) }
        val position = range?.let { lines.position(it.start) }
        SchemaIssue(problem, node?.key, range, position?.line, position?.column)
    }.sortedWith(compareBy(nullsLast()) { it.range?.start })
}

/** The line of the schema [fault] is at, in [schema]'s text, where it says a place. */
private fun lineOf(fault: SchemaFault, schema: SchemaDocument): Int? {
    val at = when (fault) {
        is SchemaFault.UnresolvedReference -> fault.at
        is SchemaFault.NotASchema -> fault.at
        else -> return null
    }
    val node = schema.root?.let { nodeAtPath(it, at) } ?: return null
    return LineMap(schema.text).position(markOf(node, schema.text).start).line
}
