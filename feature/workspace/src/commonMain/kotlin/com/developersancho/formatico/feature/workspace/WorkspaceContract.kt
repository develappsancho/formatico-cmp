package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.Repairs
import com.developersancho.formatico.core.share.ShareKeep

/**
 * What a workspace draws: one open document, what was read of it, and how its tree is left.
 *
 * @property document the document, or null once it is closed.
 * @property reading what Formatico made of it.
 * @property stats how its text is laid out, once it is read.
 * @property expanded the keys of the tree's open branches.
 * @property trail the values from the root down to the chosen one, or nothing where none is chosen.
 * @property table the table of the array the choice is in, or of the document's first array.
 * @property json5Allows how many of a JSON document's errors JSON5 allows: what reading it as JSON5 would leave out.
 * @property summary what the document holds, while its Summary is shown.
 * @property sizes how large the document is, written out and squeezed, while its Summary is shown.
 * @property query what the document's query found, while its Query is shown, or null.
 * @property recentQueries the queries used last on this device, the latest first.
 * @property conversion what converting the document came to, while its Convert is shown, or null.
 * @property types what writing the document's types came to, while its Types is shown, or null.
 * @property comparison what comparing the document with another came to, while its Compare is shown, or null.
 * @property schema what validating the document against its schema came to, while its Schema is shown or a schema is
 *   chosen for it, or null.
 * @property schemas the other open documents that are JSON Schemas, which it can be validated against.
 * @property others the other open documents, which it can be compared with, in the order of their tabs.
 */
internal data class WorkspaceState(
    val document: OpenDocument? = null,
    val reading: Reading = Reading.Pending,
    val stats: TextStats? = null,
    val expanded: Set<String> = emptySet(),
    val trail: List<ValueNode> = emptyList(),
    val table: TableModel? = null,
    val json5Allows: Int = 0,
    val summary: DocumentSummary? = null,
    val sizes: SizeSummary? = null,
    val query: QueryOutcome? = null,
    val recentQueries: RecentQueries = RecentQueries(),
    val conversion: ConvertOutcome? = null,
    val types: TypesOutcome? = null,
    val comparison: CompareOutcome? = null,
    val schema: SchemaCheck? = null,
    val schemas: List<OfferedSchema> = emptyList(),
    val others: List<OtherDocument> = emptyList(),
    val share: DocumentShare? = null
) {
    /** What is wrong in the document, where it was read: its errors, and the warnings it is valid with. */
    val problems: List<Problem> get() = (reading as? Reading.Read)?.problems.orEmpty()

    /** How many of the [problems] Formatico repairs. */
    val repairable: Int
        get() {
            val document = document ?: return 0
            val format = document.format ?: return 0
            return problems.count { Repairs.of(it, document.text, format) != null }
        }

    /** The value chosen in the tree, or null. */
    val selected: ValueNode? get() = trail.lastOrNull()

    /** The document's value, where it was read. */
    val root: ValueNode? get() = (reading as? Reading.Read)?.root

    /**
     * What Format, Minify and Sort keys can make of the document: what its format allows, where it is read and has no
     * error.
     */
    val edits: Set<DocumentEdit>
        get() = if (reading is Reading.Read && reading.errors == 0 && reading.root != null) {
            reading.format.edits
        } else {
            emptySet()
        }

    /** Where the table stands in the tree's choice. */
    val tableChoice: TableChoice? get() = tableChoice(table, trail)

    /** The language the document is queried in, where it has a format. */
    val queryLanguage: QueryLanguage? get() = document?.format?.queryLanguage

    /** The queries of the document's language used last, the latest first, but the one it is queried with now. */
    val otherQueries: List<String>
        get() {
            val language = queryLanguage ?: return emptyList()
            val asked = document?.view?.query?.trim()
            return recentQueries.of(language).filter { it != asked }
        }

    /** The place of the chosen value among the query's matches, or null where it is none of them. */
    val currentMatch: Int? get() = query.matches.indexOfFirst { it.key != null && it.key == selected?.key }.takeIf { it >= 0 }
}

/** Another open document, by its [id] and its [name]: one a document can be compared with. */
internal data class OtherDocument(val id: Long, val name: String?)

/** What the user did, or asked for, in a workspace. */
internal sealed interface WorkspaceIntent {
    /** Show the document's link to share it, or leave it. */
    data class Share(val open: Boolean) : WorkspaceIntent

    /** Format the document in the link where [formatted], or leave its text as it is. */
    data class ShareFormatted(val formatted: Boolean) : WorkspaceIntent

    /** Share the document the [way] chosen. */
    data class ShareBy(val way: ShareWay) : WorkspaceIntent

    /** Keep the encrypted link for [keep]. */
    data class ShareKept(val keep: ShareKeep) : WorkspaceIntent

    /** Make the encrypted link: encrypt the document on this device and send it to Formatico's server. */
    data object CreateEncryptedLink : WorkspaceIntent

    /** Delete the encrypted link from Formatico's server, before it expires. */
    data object DeleteEncryptedLink : WorkspaceIntent

    /** Open the tree's branches whose keys are [keys], and close the others. */
    data class Expand(val keys: Set<String>) : WorkspaceIntent

    /** Choose the value whose key is [key], or none. */
    data class Select(val key: String?) : WorkspaceIntent

    /** Show [pane] of the document, on a phone. */
    data class ShowPane(val pane: DocumentPane) : WorkspaceIntent

    /** Open the tree's search bar, or close it where [open] is false. */
    data class Search(val open: Boolean) : WorkspaceIntent

    /** Read the document as [format], or as plain text. */
    data class ChangeFormat(val format: Format?) : WorkspaceIntent

    /** Make [edit] to the document. */
    data class Edit(val edit: DocumentEdit) : WorkspaceIntent

    /** Order the table's rows by [sort], or give them their own order back. */
    data class SortTable(val sort: TableSort?) : WorkspaceIntent

    /** Hide the table's columns named in [hidden], and show the others. */
    data class HideColumns(val hidden: Set<String>) : WorkspaceIntent

    /** Filter the table's rows by [filter], or show them all where it is empty. */
    data class FilterTable(val filter: String) : WorkspaceIntent

    /** Query the document with [query], in its language. */
    data class Query(val query: String) : WorkspaceIntent

    /** The query the document is queried with was used: it is offered again, as a recent one. */
    data object UseQuery : WorkspaceIntent

    /** Convert the document as [choice] says. */
    data class Convert(val choice: ConvertChoice) : WorkspaceIntent

    /** Write the document's types as [choice] says. */
    data class Types(val choice: TypesChoice) : WorkspaceIntent

    /** Compare the document with another as [choice] says. */
    data class Compare(val choice: CompareChoice) : WorkspaceIntent

    /** Validate the document against the schema [choice] names. */
    data class Schema(val choice: SchemaChoice) : WorkspaceIntent

    /** The document's text is now [text], as its editor wrote it. */
    data class Write(val text: String) : WorkspaceIntent

    /** Repair [problem] as Formatico repairs it. */
    data class Repair(val problem: Problem) : WorkspaceIntent

    /** Repair every problem Formatico repairs, until none is left. */
    data object RepairAll : WorkspaceIntent

    /** Close the document. */
    data object Close : WorkspaceIntent
}

/** What happens once in a workspace. */
internal sealed interface WorkspaceEffect {
    /** [edit] was asked for, and [result] came of it. */
    data class Edited(val edit: DocumentEdit, val result: EditResult) : WorkspaceEffect
}
