package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.Repairs

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
    val sizes: SizeSummary? = null
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
}

/** What the user did, or asked for, in a workspace. */
internal sealed interface WorkspaceIntent {
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
