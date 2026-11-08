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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.base.MviScreen
import com.developersancho.formatico.core.format.Format
import com.pantrixui.components.container.twopane.PaneSize
import com.pantrixui.components.container.twopane.PantrixTwoPane
import com.pantrixui.components.container.twopane.TwoPaneOrientation
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.divider.PantrixVerticalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.foundation.adaptive.WindowWidthClass
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_compare
import formatico.feature.workspace.generated.resources.workspace_convert
import formatico.feature.workspace.generated.resources.workspace_copy_document
import formatico.feature.workspace.generated.resources.workspace_format
import formatico.feature.workspace.generated.resources.workspace_format_menu
import formatico.feature.workspace.generated.resources.workspace_minify
import formatico.feature.workspace.generated.resources.workspace_plain_text
import formatico.feature.workspace.generated.resources.workspace_query
import formatico.feature.workspace.generated.resources.workspace_save_file
import formatico.feature.workspace.generated.resources.workspace_sort_keys
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The share of a wide window the text takes beside the tree: the design's 1.15 to 1. */
private const val TEXT_SHARE = 1.15f / 2.15f

/** How tall the text stands above the tree on a tablet. */
private val TextHeight = 480.dp

/** The workspace of the open document whose id is [documentId]: its toolbar, its text, its tree and its status. */
@Composable
fun WorkspaceRoute(documentId: Long, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<WorkspaceViewModel>(key = "workspace_$documentId") { parametersOf(documentId) }
    val commands = rememberDocumentCommands(koinInject())
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is WorkspaceEffect.Edited -> commands.report(effect.edit, effect.result)
            }
        }
    ) { state, dispatch ->
        WorkspaceScreen(state = state, onIntent = dispatch, commands = commands, modifier = modifier)
    }
}

/** A workspace drawn from [state]; what the user does goes to [onIntent], or to [commands]. */
@Composable
internal fun WorkspaceScreen(
    state: WorkspaceState,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    val layout = workspaceLayout()
    if (layout == WorkspaceLayout.Phone) {
        PhoneWorkspace(
            state = state,
            document = document,
            onIntent = onIntent,
            commands = commands,
            modifier = modifier
        )
        return
    }
    val wide = layout == WorkspaceLayout.Wide
    Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.surface)) {
        Toolbar(state = state, document = document, layout = layout, onIntent = onIntent, commands = commands)
        PantrixHorizontalDivider()
        PantrixTwoPane(
            first = {
                if (wide) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        TextPane(state = state, modifier = Modifier.weight(1f).fillMaxHeight())
                        PantrixVerticalDivider()
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TextPane(state = state, modifier = Modifier.weight(1f).fillMaxWidth())
                        PantrixHorizontalDivider()
                    }
                }
            },
            second = { TreePane(state = state, layout = layout, onIntent = onIntent) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            orientation = if (wide) TwoPaneOrientation.SideBySide else TwoPaneOrientation.Stacked,
            firstSize = if (wide) PaneSize.Fraction(TEXT_SHARE) else PaneSize.Fixed(TextHeight),
            gap = 0.dp,
            tagKey = "panes"
        )
        StatusBar(state = state, layout = layout)
    }
}

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
        PantrixButton(
            text = stringResource(Res.string.workspace_query),
            onClick = {},
            variant = ButtonVariant.Text,
            size = size,
            enabled = false,
            leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Search, contentDescription = null) },
            tagKey = "query"
        )
        PantrixButton(
            text = stringResource(Res.string.workspace_convert),
            onClick = {},
            variant = ButtonVariant.Text,
            size = size,
            enabled = false,
            leadingIcon = { PantrixIcon(imageVector = PantrixIcons.ArrowForward, contentDescription = null) },
            tagKey = "convert"
        )
        if (wide) {
            PantrixButton(
                text = stringResource(Res.string.workspace_compare),
                onClick = {},
                variant = ButtonVariant.Text,
                size = size,
                enabled = false,
                tagKey = "compare"
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        DocumentButtons(state = state, document = document, layout = layout, onIntent = onIntent, commands = commands)
    }
}

/** Format, Minify, and in a wide window Sort keys: what changes the whole document. */
@Composable
private fun EditButtons(state: WorkspaceState, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit) {
    val size = layout.buttons
    PantrixButton(
        text = stringResource(Res.string.workspace_format),
        onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.Format)) },
        size = size,
        enabled = state.editable,
        tagKey = "format"
    )
    PantrixButton(
        text = stringResource(Res.string.workspace_minify),
        onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.Minify)) },
        variant = ButtonVariant.Outline,
        size = size,
        enabled = state.editable,
        tagKey = "minify"
    )
    if (layout == WorkspaceLayout.Wide) {
        PantrixButton(
            text = stringResource(Res.string.workspace_sort_keys),
            onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.SortKeys)) },
            variant = ButtonVariant.Subtle,
            size = size,
            enabled = state.editable,
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
            contentDescription = stringResource(Res.string.workspace_copy_document),
            onClick = { commands.copy(document) },
            size = layout.buttons,
            tagKey = "copy"
        )
        if (wide) {
            PantrixIconButton(
                imageVector = PantrixIcons.Download,
                contentDescription = stringResource(Res.string.workspace_save_file),
                onClick = { commands.save(document) },
                size = layout.buttons,
                tagKey = "save"
            )
        }
        DocumentMenu(
            items = if (wide) DocumentMenuItems.Close else DocumentMenuItems.SortCompareSaveClose,
            size = layout.buttons,
            canSort = state.editable,
            onSortKeys = { onIntent(WorkspaceIntent.Edit(DocumentEdit.SortKeys)) },
            onSave = { commands.save(document) },
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
    val name = format?.displayName ?: stringResource(Res.string.workspace_plain_text)
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
                contentDescription = stringResource(Res.string.workspace_format_menu, name),
                tagKey = "format_menu"
            )
        },
        tagKey = "formats"
    ) {
        Format.entries.forEach { entry ->
            PantrixMenuItem(
                text = entry.displayName,
                onClick = { onChange(entry) },
                selected = entry == format,
                tagKey = entry.name.lowercase()
            )
        }
        PantrixMenuItem(
            text = stringResource(Res.string.workspace_plain_text),
            onClick = { onChange(null) },
            selected = format == null,
            tagKey = "plain"
        )
    }
}

/** What stands where the text editor will: a word on why it is not there yet. */
@Composable
private fun TextPane(state: WorkspaceState, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        EditorComing(readable = state.reading !is Reading.Unread, modifier = Modifier.fillMaxSize())
    }
}
