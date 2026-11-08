package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.LineEnding
import com.developersancho.formatico.core.format.text.LineMap
import com.developersancho.formatico.feature.workspace.strings.convertStrings
import com.developersancho.formatico.feature.workspace.strings.queryStrings
import com.developersancho.formatico.feature.workspace.strings.typesStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.data.tag.TagTone
import com.pantrixui.components.elements.button.ButtonColors
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.button.colors
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * The bar under the panes: whether the document is valid, how long it is, the line and column of [editor]'s caret or
 * of the chosen value, the chosen value's path and the text's layout, or while the query is shown which of its matches
 * is chosen. How long it is shows its Summary, or hides it. While it is converted, the bar says what is lost, where
 * anything is, and how long the document is in each format; while its types are shown, how many and how long.
 */
@Composable
internal fun StatusBar(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    editor: CodeEditorState? = null,
    onIntent: (WorkspaceIntent) -> Unit = {}
) {
    val spacing = PantrixTheme.spacing
    val style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium)
    val quiet = PantrixTheme.colors.textSecondary
    Column(modifier = Modifier.fillMaxWidth().background(PantrixTheme.colors.background)) {
        PantrixHorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // As tall as the board's bar, or as a touch target where a button needs more: the size is pressed.
                .heightIn(min = if (layout == WorkspaceLayout.Wide) 28.dp else 32.dp)
                .padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            if (madeStatus(state)) {
                Spacer(modifier = Modifier.weight(1f))
                if (layout == WorkspaceLayout.Wide) state.stats?.let { PantrixText(text = layoutOf(it), style = style, color = quiet) }
                return@Row
            }
            StatusTag(state)
            state.stats?.let { stats -> SizeButton(state, stats, onIntent) }
            Spacer(modifier = Modifier.weight(1f))
            // While the query is shown, the bar says which of its matches is chosen, where the text's layout stood.
            val match = state.currentMatch?.takeIf { state.document?.view?.pane == DocumentPane.Query }
            match?.let {
                PantrixText(
                    text = queryStrings.matchOf(it + 1, state.query.matches.size),
                    style = style,
                    color = quiet
                )
            }
            caretOf(state, editor)?.let { (line, column) ->
                PantrixText(
                    text = workspaceStrings.lineColumn(line, column),
                    style = style,
                    color = quiet
                )
            }
            state.selected?.let { selected ->
                PantrixText(
                    text = jsonPath(selected.path),
                    style = PantrixTheme.typography.code.copy(fontSize = style.fontSize, lineHeight = style.lineHeight),
                    color = quiet,
                    maxLines = 1
                )
            }
            if (layout == WorkspaceLayout.Wide && match == null) {
                state.stats?.let { PantrixText(text = layoutOf(it), style = style, color = quiet) }
            }
        }
    }
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

/** Whether the document is valid, how many problems it has, or that it is plain text: a tag in its colour. */
@Composable
internal fun StatusTag(state: WorkspaceState) {
    val (text, tone) = when (val reading = state.reading) {
        Reading.Pending -> {
            workspaceStrings.statusReading to TagTone.Neutral
        }

        Reading.PlainText -> {
            workspaceStrings.plainText to TagTone.Neutral
        }

        is Reading.Read -> {
            if (reading.errors == 0) {
                workspaceStrings.statusValid(reading.format.displayName) to TagTone.Success
            } else {
                workspaceStrings.statusProblems(reading.errors, reading.errors) to
                    TagTone.Danger
            }
        }
    }
    PantrixTag(text = text, tone = tone, dot = true, tagKey = "status")
}

/**
 * How long the document is, as a button that shows its Summary in the tree's place, and while it is shown, in the
 * primary colour, gives the tree back: the table's, for a table's file.
 */
@Composable
private fun SizeButton(state: WorkspaceState, stats: TextStats, onIntent: (WorkspaceIntent) -> Unit) {
    val document = state.document ?: return
    val shown = document.view.pane == DocumentPane.Summary
    val size = workspaceStrings.separated(
        workspaceStrings.lines(stats.lines, stats.lines),
        PantrixTheme.strings.fileSize(stats.bytes)
    )
    val back = openingPane(document.format)
    PantrixButton(
        text = size,
        onClick = { onIntent(WorkspaceIntent.ShowPane(if (shown) back else DocumentPane.Summary)) },
        variant = ButtonVariant.Text,
        size = ButtonSize.ExtraSmall,
        colors = if (shown) {
            ButtonVariant.Text.colors()
        } else {
            ButtonColors(container = Color.Transparent, content = PantrixTheme.colors.textSecondary, border = Color.Transparent)
        },
        contentDescription = if (shown) workspaceStrings.summaryShown(size) else workspaceStrings.showSummary(size),
        tagKey = "size"
    )
}

/** The status of what is made of the document, while it is shown: its conversion's or its types'. Whether it has one. */
@Composable
private fun madeStatus(state: WorkspaceState): Boolean {
    val pane = state.document?.view?.pane
    val converted = state.conversion.converted?.takeIf { pane == DocumentPane.Convert }
    val written = state.types.written?.takeIf { pane == DocumentPane.Types }
    when {
        converted != null -> ConvertedStatus(state, converted)
        written != null -> TypesStatus(state, written)
        else -> return false
    }
    return true
}

/** Whether the document is valid, and `JSON 742 B → Kotlin: 6 types, 51 lines`, while its types are shown. */
@Composable
private fun TypesStatus(state: WorkspaceState, written: TypesOutcome.Written) {
    StatusTag(state)
    PantrixText(
        text = typesStrings.sizes(
            written.types,
            written.from.displayName,
            PantrixTheme.strings.fileSize(state.stats?.bytes ?: 0),
            written.language.displayName,
            written.lines
        ),
        style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
        color = PantrixTheme.colors.textSecondary,
        maxLines = 1
    )
}

/** What [converted] lost, a warning, or else whether the document is valid; and `JSON 742 B → YAML 512 B`. */
@Composable
private fun ConvertedStatus(state: WorkspaceState, converted: ConvertOutcome.Converted) {
    if (converted.losses.isEmpty()) {
        StatusTag(state)
    } else {
        PantrixTag(text = lossesTag(converted.losses, converted.from, converted.to), tone = TagTone.Warning, dot = true, tagKey = "status")
    }
    val strings = PantrixTheme.strings
    PantrixText(
        text = convertStrings.sizes(
            converted.from.displayName,
            strings.fileSize(state.stats?.bytes ?: 0),
            converted.to.displayName,
            strings.fileSize(converted.bytes)
        ),
        style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
        color = PantrixTheme.colors.textSecondary,
        maxLines = 1
    )
}
