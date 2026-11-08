package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.LineEnding
import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.feature.workspace.strings.convertStrings
import com.developersancho.formatico.feature.workspace.strings.queryStrings
import com.developersancho.formatico.feature.workspace.strings.schemaStrings
import com.developersancho.formatico.feature.workspace.strings.typesStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.container.statusbar.PantrixStatusBar
import com.pantrixui.components.container.statusbar.StatusBarStyle
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.data.tag.TagTone
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * The bar under the panes, Pantrix's status bar: whether the document is valid, how long it is, the line and column of
 * [editor]'s caret or of the chosen value, the chosen value's path and in a wide window the text's layout, or while the
 * query is shown which of its matches is chosen. How long it is shows its Summary, or hides it. While it is converted,
 * the bar says what is lost, where anything is, and how long the document is in each format; while its types are shown,
 * how many and how long. Where the bar has no room, the lowest of these go into its "…" menu first.
 */
@Composable
internal fun StatusBar(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    editor: CodeEditorState? = null,
    onIntent: (WorkspaceIntent) -> Unit = {}
) {
    val wide = layout == WorkspaceLayout.Wide
    val status = statusOf(state)
    val made = madeSizes(state)
    val size = sizeOf(state, onIntent)
    val match = state.currentMatch?.takeIf { state.document?.view?.pane == DocumentPane.Query }
        ?.let { queryStrings.matchOf(it + 1, state.query.matches.size) }
    val caret = caretOf(state, editor)?.let { (line, column) -> workspaceStrings.lineColumn(line, column) }
    val path = state.selected?.let { jsonPath(it.path) }
    // While the query is shown, the bar says which of its matches is chosen, where the text's layout stood.
    val textLayout = state.stats?.takeIf { wide && match == null }?.let { layoutOf(it) }
    val pathStyle = PantrixTheme.typography.footnote.let {
        PantrixTheme.typography.code.copy(fontSize = it.fontSize, lineHeight = it.lineHeight)
    }
    val quiet = PantrixTheme.colors.textSecondary
    PantrixStatusBar(
        start = {
            item(key = "status", label = status.text, priority = StatusPriority.STATUS) {
                PantrixTag(text = status.text, tone = status.tone, dot = true, tagKey = "status")
            }
            when {
                made != null -> text(key = "sizes", text = made, priority = StatusPriority.SIZE)

                size != null -> button(
                    key = "size",
                    text = size.text,
                    onClick = size.onClick,
                    selected = size.shown,
                    contentDescription = size.description,
                    priority = StatusPriority.SIZE
                )
            }
        },
        end = {
            if (made == null) {
                match?.let { text(key = "match", text = it, priority = StatusPriority.MATCH) }
                caret?.let { text(key = "caret", text = it, priority = StatusPriority.CARET) }
                path?.let { path ->
                    item(key = "path", label = path, priority = StatusPriority.PATH) {
                        PantrixText(text = path, style = pathStyle, color = quiet, maxLines = 1)
                    }
                }
            }
            textLayout?.let { text(key = "layout", text = it, priority = StatusPriority.LAYOUT) }
        },
        // As tall as the boards draw it on every platform: 28 in a wide window, 32 on a tablet, the size's press
        // reaching past it.
        style = StatusBarStyle(height = if (wide) 28.dp else 32.dp),
        tagKey = "document"
    )
}

/** How long each of the status bar's items stays as room runs short, the lowest going into its menu first. */
private object StatusPriority {
    const val STATUS = 10
    const val SIZE = 9
    const val MATCH = 8
    const val CARET = 7
    const val PATH = 5
    const val LAYOUT = 3
}

/** What the status tag says, and in which tone. */
internal data class Status(val text: String, val tone: TagTone)

/** How long the document is, as the bar's button says it, whether its Summary is shown, and what pressing it does. */
private class SizeButton(val text: String, val shown: Boolean, val description: String, val onClick: () -> Unit)

/**
 * How long the document is, as a button that shows its Summary in the tree's place, and while it is shown, in the
 * primary colour, gives the tree back: the table's, for a table's file. Null where it is not measured yet.
 */
@Composable
private fun sizeOf(state: WorkspaceState, onIntent: (WorkspaceIntent) -> Unit): SizeButton? {
    val document = state.document ?: return null
    val stats = state.stats ?: return null
    val shown = document.view.pane == DocumentPane.Summary
    val size = workspaceStrings.separated(
        workspaceStrings.lines(stats.lines, stats.lines),
        PantrixTheme.strings.fileSize(stats.bytes)
    )
    val back = openingPane(document.format)
    return SizeButton(
        text = size,
        shown = shown,
        description = if (shown) workspaceStrings.summaryShown(size) else workspaceStrings.showSummary(size),
        onClick = { onIntent(WorkspaceIntent.ShowPane(if (shown) back else DocumentPane.Summary)) }
    )
}

/**
 * The line and the column, from 1, that the bar says: of [editor]'s caret where the text is shown, or else of the start
 * of the value chosen in the tree or the table. Both count a line's characters as the editor does, in UTF-16 units.
 */
@Composable
private fun caretOf(state: WorkspaceState, editor: CodeEditorState?): Pair<Int, Int>? {
    if (editor != null) {
        val caret = editor.selection.end
        val line = editor.lineOf(caret)
        return line + 1 to caret - editor.lineStart(line) + 1
    }
    val text = state.document?.text ?: return null
    val selected = state.selected ?: return null
    val lines = remember(text) { LineMap(text) }
    return lines.position(selected.range.start).let { it.line to it.column }
}

/** `UTF-8 · LF · 2 spaces`: how the text is encoded, ends its lines and indents them, as far as it says. */
@Composable
private fun layoutOf(stats: TextStats): String {
    val parts = buildList {
        add(workspaceStrings.encoding)
        stats.layout.lineEnding?.let { add(lineEndingName(it)) }
        when (val indentation = stats.layout.indentation) {
            is Indentation.Spaces -> {
                add(workspaceStrings.indentSpaces(indentation.count, indentation.count))
            }

            Indentation.Tabs -> {
                add(workspaceStrings.indentTabs)
            }

            null -> {}
        }
    }
    return parts.joinToString(" · ")
}

private fun lineEndingName(lineEnding: LineEnding): String = when (lineEnding) {
    LineEnding.Lf -> "LF"
    LineEnding.Crlf -> "CRLF"
    LineEnding.Cr -> "CR"
}

/**
 * The document's status in words and a tone: what is lost in its conversion, while it is converted and something is;
 * else what its schema found, where it is validated against one; else whether it is valid, how many problems it has, or
 * that it is plain text.
 */
@Composable
internal fun statusOf(state: WorkspaceState): Status {
    val schema = state.schema
    val reading = state.reading
    val converted = state.conversion.converted?.takeIf { state.document?.view?.pane == DocumentPane.Convert }
    return when {
        converted != null && converted.losses.isNotEmpty() ->
            Status(lossesTag(converted.losses, converted.from, converted.to), TagTone.Warning)

        schema is SchemaCheck.Failed -> Status(schemaStrings.statusProblems(schema.total, schema.total), TagTone.Danger)

        schema is SchemaCheck.Valid -> Status(schemaStrings.statusValid, TagTone.Success)

        reading is Reading.Read && reading.errors == 0 ->
            Status(workspaceStrings.statusValid(reading.format.displayName), TagTone.Success)

        reading is Reading.Read -> Status(workspaceStrings.statusProblems(reading.errors, reading.errors), TagTone.Danger)

        reading == Reading.PlainText -> Status(workspaceStrings.plainText, TagTone.Neutral)

        else -> Status(workspaceStrings.statusReading, TagTone.Neutral)
    }
}

/**
 * While what is made of the document is shown, how long it is and what is made: `JSON 742 B → YAML 512 B` while it is
 * converted, `JSON 742 B → Kotlin: 6 types, 51 lines` while its types are shown. Null otherwise.
 */
@Composable
private fun madeSizes(state: WorkspaceState): String? {
    val pane = state.document?.view?.pane
    val converted = state.conversion.converted?.takeIf { pane == DocumentPane.Convert }
    val written = state.types.written?.takeIf { pane == DocumentPane.Types }
    val strings = PantrixTheme.strings
    val bytes = strings.fileSize(state.stats?.bytes ?: 0)
    return when {
        converted != null -> convertStrings.sizes(
            converted.from.displayName,
            bytes,
            converted.to.displayName,
            strings.fileSize(converted.bytes)
        )

        written != null -> typesStrings.sizes(written.types, written.from.displayName, bytes, written.language.displayName, written.lines)

        else -> null
    }
}
