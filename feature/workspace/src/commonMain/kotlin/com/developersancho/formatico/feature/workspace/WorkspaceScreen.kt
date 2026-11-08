package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.base.MviScreen
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.feature.workspace.editor.DocumentEditor
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.container.twopane.PaneSize
import com.pantrixui.components.container.twopane.PantrixTwoPane
import com.pantrixui.components.container.twopane.TwoPaneOrientation
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.divider.PantrixVerticalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The share of a wide window the text takes beside the tree: the design's 1.15 to 1. */
private const val TEXT_SHARE = 1.15f / 2.15f

/** The share of a wide window the text takes beside the table: the design's 0.72 to 1.28. */
private const val TABLE_TEXT_SHARE = 0.72f / 2f

/** How tall the text stands above the tree on a tablet until its handle is dragged: the board's 480, and its line. */
private val TextHeight = 481.dp

/** The strip the handle stands in between the text and the tree on a tablet: the board's 13, less the two lines. */
private val HandleGap = 11.dp

/** The workspace of the open document whose id is [documentId]: its toolbar, its text, its tree and its status. */
@Composable
fun WorkspaceRoute(documentId: Long, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<WorkspaceViewModel>(key = "workspace_$documentId") { parametersOf(documentId) }
    val commands = rememberDocumentCommands(koinInject())
    val editors = koinInject<Editors>()
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is WorkspaceEffect.Edited -> commands.report(effect.edit, effect.result)
            }
        }
    ) { state, dispatch ->
        WorkspaceScreen(
            state = state,
            onIntent = dispatch,
            commands = commands,
            editor = state.document?.let(editors::of),
            modifier = modifier
        )
    }
}

/**
 * A workspace drawn from [state]; what the user does goes to [onIntent], or to [commands]. Its text is [editor]'s, the
 * document's own editor, which outlives the workspace; a preview or a test that gives none has one made for it.
 */
@Composable
internal fun WorkspaceScreen(
    state: WorkspaceState,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    editor: CodeEditorState? = null,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    val text = editor ?: remember(document.id) { CodeEditorState(document.text) }
    val layout = workspaceLayout()
    ShareDocument(state = state, layout = layout, onIntent = onIntent, commands = commands)
    if (layout == WorkspaceLayout.Phone) {
        when (document.view.pane) {
            // The conversion and the comparison are screens of their own, their bars in the app bar's place, as
            // Convert-390 and Compare-390 draw them.
            DocumentPane.Convert -> PhoneConvert(state = state, onIntent = onIntent, commands = commands, modifier = modifier)

            DocumentPane.Types -> PhoneTypes(state = state, onIntent = onIntent, commands = commands, modifier = modifier)

            DocumentPane.Schema -> PhoneSchema(state = state, editor = text, onIntent = onIntent, modifier = modifier)

            DocumentPane.Compare -> PhoneCompare(
                state = state,
                onIntent = onIntent,
                more = { PhoneMoreMenu(state, document, onIntent) },
                modifier = modifier
            )

            else -> PhoneWorkspace(
                state = state,
                document = document,
                editor = text,
                onIntent = onIntent,
                commands = commands,
                modifier = modifier
            )
        }
        return
    }
    val wide = layout == WorkspaceLayout.Wide
    // The comparison takes the toolbar's room and the status bar's, as the Compare boards draw it.
    if (document.view.pane == DocumentPane.Compare) {
        CompareScreen(state = state, layout = layout, onIntent = onIntent, modifier = modifier)
        return
    }
    Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.surface)) {
        Toolbar(state = state, document = document, layout = layout, onIntent = onIntent, commands = commands)
        PantrixHorizontalDivider()
        if (document.view.pane == DocumentPane.Table && !wide) {
            // A tablet gives the table the whole room, as its board draws it.
            TablePane(state, layout, onIntent, commands, Modifier.weight(1f).fillMaxWidth())
            StatusBar(state = state, layout = layout, onIntent = onIntent)
            return@Column
        }
        if (document.view.pane == DocumentPane.Convert && !wide) {
            // A tablet gives the conversion the whole room as well: Convert-834 draws the new document alone.
            ConvertPane(state, layout, onIntent, commands, Modifier.weight(1f).fillMaxWidth())
            StatusBar(state = state, layout = layout, onIntent = onIntent)
            return@Column
        }
        if (document.view.pane == DocumentPane.Types && !wide) {
            // And its types, as Types-834 draws them.
            TypesPane(state, layout, onIntent, commands, Modifier.weight(1f).fillMaxWidth())
            StatusBar(state = state, layout = layout, onIntent = onIntent)
            return@Column
        }
        if (document.view.pane == DocumentPane.Schema && !wide) {
            // And its validation, as Schema-834 draws it.
            SchemaPane(state, text, layout, onIntent, Modifier.weight(1f).fillMaxWidth())
            StatusBar(state = state, layout = layout, onIntent = onIntent)
            return@Column
        }
        DocumentPanes(state, text, layout, onIntent, commands, Modifier.weight(1f).fillMaxWidth())
        StatusBar(state = state, layout = layout, editor = text, onIntent = onIntent)
    }
}

/**
 * The text and, beside it in a wide window, the tree, the table, the graph or the problems; on a tablet the tree or the problems
 * under it, past a strip of the page's background whose handle makes the text taller and shorter.
 */
@Composable
private fun DocumentPanes(
    state: WorkspaceState,
    editor: CodeEditorState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val wide = layout == WorkspaceLayout.Wide
    val pane = state.document?.view?.pane
    val table = pane == DocumentPane.Table
    if (wide && pane in MadeBesideTextPanes) {
        MadeBesideText(state, editor, onIntent, commands, modifier)
        return
    }
    // How tall the text stands on a tablet, as its handle leaves it.
    var textHeight by rememberSaveable { mutableFloatStateOf(TextHeight.value) }
    val second: @Composable () -> Unit = { PaneBesideText(state, editor, layout, onIntent, commands) }
    if (wide) {
        PantrixTwoPane(
            first = {
                Row(modifier = Modifier.fillMaxSize()) {
                    TextPane(state, editor, onIntent, Modifier.weight(1f).fillMaxHeight())
                    PantrixVerticalDivider()
                }
            },
            second = second,
            modifier = modifier,
            firstSize = PaneSize.Fraction(if (table) TABLE_TEXT_SHARE else TEXT_SHARE),
            gap = 0.dp,
            tagKey = "panes"
        )
        return
    }
    val surface = Modifier.fillMaxSize().background(PantrixTheme.colors.surface)
    PantrixTwoPane(
        first = {
            Column(modifier = surface) {
                TextPane(state, editor, onIntent, Modifier.weight(1f).fillMaxWidth())
                PantrixHorizontalDivider()
            }
        },
        second = {
            Column(modifier = surface) {
                PantrixHorizontalDivider()
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) { second() }
            }
        },
        modifier = modifier.background(PantrixTheme.colors.background),
        orientation = TwoPaneOrientation.Stacked,
        firstSize = PaneSize.Fixed(textHeight.dp),
        gap = HandleGap,
        tagKey = "panes",
        onFirstSizeChange = { size -> if (size is PaneSize.Fixed) textHeight = size.size.value }
    )
}

/** The panes a wide window shows beside the text, each a document of its own made of it. */
private val MadeBesideTextPanes = setOf(DocumentPane.Convert, DocumentPane.Types, DocumentPane.Schema)

/** The design's toolbar heights: 48 in a wide window, 56 on a tablet. */
private fun toolbarHeight(layout: WorkspaceLayout): Dp = if (layout == WorkspaceLayout.Wide) 48.dp else 56.dp

/** The actions on the whole document, over its text and its tree. */
@Composable
private fun Toolbar(
    state: WorkspaceState,
    document: OpenDocument,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    val wide = layout == WorkspaceLayout.Wide
    val size = layout.buttons
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(toolbarHeight(layout))
            .padding(horizontal = PantrixTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)
    ) {
        FormatMenu(format = document.format, size = size, onChange = { onIntent(WorkspaceIntent.ChangeFormat(it)) })
        if (wide) ToolbarDivider(wide)
        EditButtons(state = state, layout = layout, onIntent = onIntent)
        ToolbarDivider(wide)
        MadeButtons(document = document, size = size, onIntent = onIntent)
        if (wide) {
            PantrixButton(
                text = workspaceStrings.compare,
                onClick = { onIntent(WorkspaceIntent.ShowPane(DocumentPane.Compare)) },
                variant = ButtonVariant.Text,
                size = size,
                tagKey = "compare"
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        DocumentButtons(state = state, document = document, layout = layout, onIntent = onIntent, commands = commands)
    }
}

/** Repair where there is something to repair, Format, Minify, and in a wide window Sort keys: what changes the whole document. */
@Composable
private fun EditButtons(state: WorkspaceState, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit) {
    val size = layout.buttons
    val repairing = state.repairable > 0
    // An invalid document is repaired before it is formatted: the Invalid boards put Repair first.
    if (repairing) {
        PantrixButton(
            text = workspaceStrings.repair,
            onClick = { onIntent(WorkspaceIntent.RepairAll) },
            size = size,
            tagKey = "repair"
        )
    }
    PantrixButton(
        text = workspaceStrings.format,
        onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.Format)) },
        size = size,
        enabled = DocumentEdit.Format in state.edits,
        tagKey = "format"
    )
    // A tablet has no room for Minify beside Repair where it cannot be used: Repair takes its place, as Invalid-834
    // draws it.
    if (repairing && state.edits.isEmpty() && layout != WorkspaceLayout.Wide) return
    PantrixButton(
        text = workspaceStrings.minify,
        onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.Minify)) },
        variant = ButtonVariant.Outline,
        size = size,
        enabled = DocumentEdit.Minify in state.edits,
        tagKey = "minify"
    )
    if (layout == WorkspaceLayout.Wide) {
        PantrixButton(
            text = workspaceStrings.sortKeys,
            onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.SortKeys)) },
            variant = ButtonVariant.Subtle,
            size = size,
            enabled = DocumentEdit.SortKeys in state.edits,
            tagKey = "sort_keys"
        )
    }
}

/** Copy, Save as a file in a wide window, and the "more" menu, at the toolbar's end. */
@Composable
private fun DocumentButtons(
    state: WorkspaceState,
    document: OpenDocument,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    val wide = layout == WorkspaceLayout.Wide
    Row(verticalAlignment = Alignment.CenterVertically) {
        PantrixIconButton(
            imageVector = PantrixIcons.Copy,
            contentDescription = workspaceStrings.copyDocument,
            onClick = { commands.copy(document) },
            size = layout.buttons,
            tagKey = "copy"
        )
        if (wide) {
            PantrixIconButton(
                imageVector = PantrixIcons.Download,
                contentDescription = workspaceStrings.saveFile,
                onClick = { commands.save(document) },
                size = layout.buttons,
                tagKey = "save"
            )
        }
        DocumentMenu(
            items = if (wide) DocumentMenuItems.Close else DocumentMenuItems.SortCompareSaveClose,
            size = layout.buttons,
            canSort = DocumentEdit.SortKeys in state.edits,
            onSortKeys = { onIntent(WorkspaceIntent.Edit(DocumentEdit.SortKeys)) },
            onCompare = { onIntent(WorkspaceIntent.ShowPane(DocumentPane.Compare)) },
            onSave = { commands.save(document) },
            onShare = { onIntent(WorkspaceIntent.Share(open = true)) },
            onClose = { onIntent(WorkspaceIntent.Close) }
        )
    }
}

@Composable
private fun ToolbarDivider(wide: Boolean) {
    PantrixVerticalDivider(
        modifier = Modifier
            .height(if (wide) 24.dp else 28.dp)
            .padding(horizontal = if (wide) PantrixTheme.spacing.xs else PantrixTheme.spacing.none)
    )
}

/** The button that says the document's format and opens the menu of the formats it can be read as. */
@Composable
internal fun FormatMenu(format: Format?, size: ButtonSize, onChange: (Format?) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val name = format?.displayName ?: workspaceStrings.plainText
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            PantrixButton(
                text = name,
                onClick = { open = true },
                variant = ButtonVariant.Outline,
                size = size,
                trailingIcon = { PantrixIcon(imageVector = PantrixIcons.ChevronDown, contentDescription = null) },
                contentDescription = workspaceStrings.formatMenu(name),
                tagKey = "format_menu"
            )
        },
        tagKey = "formats"
    ) {
        FormatMenuItems(format = format, onChange = onChange)
    }
}

/** The document's text in its editor, kept with the document and with the tree's choice. */
@Composable
internal fun TextPane(
    state: WorkspaceState,
    editor: CodeEditorState,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    DocumentEditor(
        document = document,
        editor = editor,
        indentation = state.stats?.layout?.indentation,
        root = state.root,
        selected = state.selected,
        onWrite = { onIntent(WorkspaceIntent.Write(it)) },
        onSelect = { onIntent(WorkspaceIntent.Select(it)) },
        modifier = modifier,
        problems = state.problems,
        schemaIssues = state.schema.issues,
        matches = state.query.matches.map { TextRange(it.range.start, it.range.end) }
    )
}
