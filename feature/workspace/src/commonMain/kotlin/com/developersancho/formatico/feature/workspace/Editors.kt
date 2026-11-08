package com.developersancho.formatico.feature.workspace

import com.pantrixui.components.data.codeeditor.CodeEditorState

/**
 * The editors of the open documents of [workbench], kept while they are open: each holds its document's text as it is
 * typed, its selection, and its own history to undo. A workspace made again for a document, as one shown again after
 * Start, finds its editor as it was left, its undo with it.
 */
internal class Editors(private val workbench: Workbench) {
    private val kept = mutableMapOf<Long, CodeEditorState>()

    /** The editor of [document], made with its text the first time. */
    fun of(document: OpenDocument): CodeEditorState {
        val open = workbench.state.value.documents.mapTo(HashSet()) { it.id }
        kept.keys.retainAll(open)
        return kept.getOrPut(document.id) { CodeEditorState(document.text) }
    }
}
