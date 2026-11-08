package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.feature.workspace.editor.ConvertedText
import com.pantrixui.components.container.twopane.PaneSize
import com.pantrixui.components.container.twopane.PantrixTwoPane
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.divider.PantrixVerticalDivider
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.convert_source
import formatico.feature.workspace.generated.resources.workspace_untitled
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The conversion of a document in a wide window, beside its text, or on a tablet, in the whole room: the format it is
 * converted to and how, what is lost, and the new document, to be copied, saved or opened in a tab of its own. A wide
 * window has the actions with the choices; a tablet, along the bottom.
 */
@Composable
internal fun ConvertPane(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val converted = state.conversion.converted
    val choice = state.document?.view?.convert ?: return
    if (converted == null) {
        ConvertMessage(state = state, modifier = modifier.fillMaxSize())
        return
    }
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    Column(modifier = modifier.fillMaxSize()) {
        if (wide) {
            WideChoices(converted, choice, onIntent, commands)
            PantrixHorizontalDivider()
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = spacing.md, end = spacing.md, top = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                ConvertAlerts(converted = converted, withNotes = true)
                if (converted.choosesNulls) ChoiceRow { NullChoice(choice, onIntent) }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                ChoiceRow {
                    ConvertToLabel(from = converted.from, layout = layout)
                    TargetMenu(converted.from, converted.to, layout.buttons) { onIntent(WorkspaceIntent.Convert(choice.copy(to = it))) }
                    if (converted.to.indents) IndentChoice(choice, onIntent)
                    Spacer(modifier = Modifier.weight(1f))
                    SortKeysSwitch(choice, onIntent)
                }
                ConvertAlerts(converted = converted, withNotes = true)
                if (converted.choosesNulls) ChoiceRow { NullChoice(choice, onIntent) }
            }
            PantrixHorizontalDivider()
        }
        ConvertedText(
            text = converted.text,
            format = converted.to,
            name = convertedFileName(converted),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            marked = converted.nullLines
        )
        if (!wide) TabletActions(converted, layout, commands)
    }
}

/** A wide window's choices and actions, on one line where they fit and on two where they do not. */
@Composable
private fun WideChoices(
    converted: ConvertOutcome.Converted,
    choice: ConvertChoice,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    val spacing = PantrixTheme.spacing
    val size = WorkspaceLayout.Wide.buttons
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        ConvertToLabel(from = converted.from, layout = WorkspaceLayout.Wide)
        TargetMenu(converted.from, converted.to, size) { onIntent(WorkspaceIntent.Convert(choice.copy(to = it))) }
        if (converted.to.indents) IndentChoice(choice, onIntent)
        SortKeysSwitch(choice, onIntent)
        Spacer(modifier = Modifier.weight(1f))
        CopyConvertedButton(converted, size, commands)
        SaveConvertedButton(converted, size, commands)
        OpenConvertedButton(converted, size, commands)
    }
}

/** A row of choices, which goes on to a second line where they do not fit. */
@Composable
private fun ChoiceRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically
    ) { content() }
}

/** A tablet's actions on the new document, along the bottom: open it, save it, copy it. */
@Composable
private fun TabletActions(converted: ConvertOutcome.Converted, layout: WorkspaceLayout, commands: DocumentCommands) {
    PantrixHorizontalDivider()
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = PantrixTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)
    ) {
        OpenConvertedButton(converted, layout.buttons, commands)
        SaveConvertedButton(converted, layout.buttons, commands)
        Spacer(modifier = Modifier.weight(1f))
        CopyConvertedButton(converted, layout.buttons, commands)
    }
}

/** Over the text of a document being converted in a wide window: its format, its name and how many lines it has. */
@Composable
internal fun ConvertSourceHeader(state: WorkspaceState) {
    val document = state.document ?: return
    val format = document.format ?: return
    val lines = state.stats?.lines ?: 0
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = PantrixTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)
        ) {
            PantrixTag(text = format.displayName, tagKey = "source_format")
            PantrixText(
                text = pluralStringResource(
                    Res.plurals.convert_source,
                    lines,
                    document.name ?: stringResource(Res.string.workspace_untitled),
                    lines
                ),
                style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                color = PantrixTheme.colors.textSecondary,
                maxLines = 1
            )
        }
        PantrixHorizontalDivider()
    }
}

/**
 * A wide window's conversion: the document's text, under its format, name and lines, and the new document beside it,
 * each half the room, as Convert-1440 draws them.
 */
@Composable
internal fun ConvertPanes(
    state: WorkspaceState,
    editor: CodeEditorState,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    PantrixTwoPane(
        first = {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ConvertSourceHeader(state)
                    TextPane(state, editor, onIntent, Modifier.weight(1f).fillMaxWidth())
                }
                PantrixVerticalDivider()
            }
        },
        second = { ConvertPane(state = state, layout = WorkspaceLayout.Wide, onIntent = onIntent, commands = commands) },
        modifier = modifier,
        firstSize = PaneSize.Fraction(CONVERT_TEXT_SHARE),
        gap = 0.dp,
        tagKey = "panes"
    )
}

/** The share of a wide window the text takes beside its conversion: half, as the board's two columns do. */
private const val CONVERT_TEXT_SHARE = 0.5f
