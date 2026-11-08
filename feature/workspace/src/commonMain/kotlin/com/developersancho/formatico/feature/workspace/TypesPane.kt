package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.feature.workspace.editor.GeneratedTypesText
import com.developersancho.formatico.feature.workspace.strings.typesStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * A document's types in a wide window, beside its text, or on a tablet, in the whole room: the language they are
 * written in and the root type's name, what could not be told, and the code, to be copied or saved. A wide window has
 * the actions with the choices, as Types-1440 draws them; a tablet, along the bottom, as Types-834 does.
 */
@Composable
internal fun TypesPane(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    val written = state.types.written
    if (written == null) {
        TypesMessage(state = state, modifier = modifier.fillMaxSize())
        return
    }
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    val field = rememberRootField(document, onIntent)
    val choose = { language: TypeLanguage -> onIntent(WorkspaceIntent.Types(document.view.types.copy(language = language))) }
    val name = document.name ?: workspaceStrings.untitled
    Column(modifier = modifier.fillMaxSize()) {
        if (wide) {
            WideTypesChoices(written, field, choose, commands)
            PantrixHorizontalDivider()
            TypesAlert(
                written = written,
                documentName = name,
                withLibrary = true,
                modifier = Modifier.fillMaxWidth().padding(start = spacing.md, end = spacing.md, top = spacing.md)
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                TabletTypesChoices(written, field, layout, choose)
                TypesAlert(written = written, documentName = name, withLibrary = true, modifier = Modifier.fillMaxWidth())
            }
            PantrixHorizontalDivider()
        }
        GeneratedTypesText(
            text = written.text,
            language = written.language,
            name = written.fileName,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        if (!wide) TabletTypesActions(written, layout, commands)
    }
}

/** A wide window's choices and actions, on one line where they fit and on two where they do not, as Types-1440. */
@Composable
private fun WideTypesChoices(
    written: TypesOutcome.Written,
    field: TextFieldState,
    choose: (TypeLanguage) -> Unit,
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
        TypesLabel(typesStrings.language)
        LanguageMenu(written.language, size, choose)
        TypesLabel(typesStrings.rootType)
        RootField(field, Modifier.width(RootFieldWidth))
        Spacer(modifier = Modifier.weight(1f))
        CopyTypesButton(written, size, commands)
        SaveTypesButton(written, size, commands)
    }
}

/** A tablet's choices: "From JSON, in", the language, and the root type's name at the line's end, as Types-834. */
@Composable
private fun TabletTypesChoices(
    written: TypesOutcome.Written,
    field: TextFieldState,
    layout: WorkspaceLayout,
    choose: (TypeLanguage) -> Unit
) {
    val spacing = PantrixTheme.spacing
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        TypesLabel(typesStrings.fromIn(written.from.displayName))
        LanguageMenu(written.language, layout.buttons, choose)
        Spacer(modifier = Modifier.weight(1f))
        TypesLabel(typesStrings.rootType)
        RootField(field, Modifier.width(RootFieldWidth))
    }
}

/** A tablet's actions on the types, along the bottom: save them, copy them. */
@Composable
private fun TabletTypesActions(written: TypesOutcome.Written, layout: WorkspaceLayout, commands: DocumentCommands) {
    PantrixHorizontalDivider()
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = PantrixTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)
    ) {
        SaveTypesButton(written, layout.buttons, commands)
        Spacer(modifier = Modifier.weight(1f))
        CopyTypesButton(written, layout.buttons, commands)
    }
}

/** A wide window's types: the document's text, under its format, name and lines, and its types beside it, as Types-1440. */
@Composable
internal fun TypesPanes(
    state: WorkspaceState,
    editor: CodeEditorState,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    BesideTextPanes(state, editor, onIntent, modifier) {
        TypesPane(state = state, layout = WorkspaceLayout.Wide, onIntent = onIntent, commands = commands)
    }
}

/** A wide window's document beside what is made of it, as the pane shown says: its conversion, or its types. */
@Composable
internal fun MadeBesideText(
    state: WorkspaceState,
    editor: CodeEditorState,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    if (state.document?.view?.pane == DocumentPane.Types) {
        TypesPanes(state, editor, onIntent, commands, modifier)
    } else {
        ConvertPanes(state, editor, onIntent, commands, modifier)
    }
}

/** How wide the root type's field stands beside the other choices, as the boards draw it. */
private val RootFieldWidth = 160.dp
