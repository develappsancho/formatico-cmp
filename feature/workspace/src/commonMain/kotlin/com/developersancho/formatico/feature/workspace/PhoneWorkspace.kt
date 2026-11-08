package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.json.jsonPath
import com.pantrixui.components.container.card.CardVariant
import com.pantrixui.components.container.card.PantrixCard
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.scrollbar.pantrixVerticalScroll
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_copy_document
import formatico.feature.workspace.generated.resources.workspace_copy_value
import formatico.feature.workspace.generated.resources.workspace_format
import formatico.feature.workspace.generated.resources.workspace_minify
import formatico.feature.workspace.generated.resources.workspace_save_file
import formatico.feature.workspace.generated.resources.workspace_show_in_text
import org.jetbrains.compose.resources.stringResource

/**
 * A workspace on a phone: the view's choice, the trail to the chosen value and the status, the text or the tree, the
 * chosen value's card, and the actions at the bottom, within the thumb's reach. The bar over it is the app's.
 */
@Composable
internal fun PhoneWorkspace(
    state: WorkspaceState,
    document: OpenDocument,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val spacing = PantrixTheme.spacing
    val pane = document.view.pane
    Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.surface)) {
        ViewChoice(
            selected = pane,
            withText = true,
            onSelect = { onIntent(WorkspaceIntent.ShowPane(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.md, bottom = spacing.sm)
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            PathTrail(state = state, onIntent = onIntent, modifier = Modifier.weight(1f))
            StatusTag(state)
        }
        PantrixHorizontalDivider()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PhonePane(state = state, pane = pane, onIntent = onIntent)
        }
        val selected = state.selected
        if (pane == DocumentPane.Tree && selected != null) {
            SelectedValueCard(
                node = selected,
                document = document,
                onCopy = commands.copyValue,
                modifier = Modifier.fillMaxWidth().padding(start = spacing.sm, end = spacing.sm, top = spacing.sm)
            )
        }
        PhoneToolbar(state = state, document = document, onIntent = onIntent, commands = commands)
    }
}

/** The pane the phone shows: the editor's place, the tree, or what stands in for the tree. */
@Composable
private fun PhonePane(state: WorkspaceState, pane: DocumentPane, onIntent: (WorkspaceIntent) -> Unit) {
    val spacing = PantrixTheme.spacing
    if (pane == DocumentPane.Text) {
        EditorComing(readable = state.reading !is Reading.Unread, modifier = Modifier.fillMaxSize())
        return
    }
    if (state.root == null) {
        // The wide layouts' pane says why there is no tree; on a phone the same pane is the whole room.
        TreePaneMessage(state = state, modifier = Modifier.fillMaxSize())
        return
    }
    DocumentTree(
        state = state,
        layout = WorkspaceLayout.Phone,
        onIntent = onIntent,
        modifier = Modifier
            .fillMaxSize()
            .pantrixVerticalScroll(rememberScrollState())
            .padding(horizontal = spacing.sm, vertical = spacing.xs)
    )
}

/** Format, Minify, Copy and Save as a file, along the bottom. */
@Composable
private fun PhoneToolbar(
    state: WorkspaceState,
    document: OpenDocument,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    val spacing = PantrixTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PantrixTheme.colors.background)
            .height(72.dp)
            .padding(horizontal = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        PantrixButton(
            text = stringResource(Res.string.workspace_format),
            onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.Format)) },
            size = ButtonSize.Large,
            enabled = state.editable,
            tagKey = "format"
        )
        PantrixButton(
            text = stringResource(Res.string.workspace_minify),
            onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.Minify)) },
            variant = ButtonVariant.Outline,
            size = ButtonSize.Large,
            enabled = state.editable,
            tagKey = "minify"
        )
        Spacer(modifier = Modifier.weight(1f))
        PantrixIconButton(
            imageVector = PantrixIcons.Copy,
            contentDescription = stringResource(Res.string.workspace_copy_document),
            onClick = { commands.copy(document) },
            size = ButtonSize.Large,
            tagKey = "copy"
        )
        PantrixIconButton(
            imageVector = PantrixIcons.Download,
            contentDescription = stringResource(Res.string.workspace_save_file),
            onClick = { commands.save(document) },
            size = ButtonSize.Large,
            tagKey = "save"
        )
    }
}

/**
 * The chosen value, read out: its path and its kind, the value itself, what more there is to say of it, and its
 * actions. Showing it in the text waits for the editor.
 */
@Composable
private fun SelectedValueCard(
    node: ValueNode,
    document: OpenDocument,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PantrixTheme.spacing
    val reading = readValue(node)
    PantrixCard(
        modifier = modifier,
        variant = CardVariant.Elevated,
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
        tagKey = "selected_value"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                PantrixText(
                    text = jsonPath(node.path),
                    style = PantrixTheme.typography.code.copy(
                        fontSize = PantrixTheme.typography.footnote.fontSize,
                        lineHeight = PantrixTheme.typography.footnote.lineHeight
                    ),
                    color = PantrixTheme.colors.textSecondary,
                    maxLines = 1
                )
                PantrixTag(text = reading.kind, icon = reading.icon, tagKey = "kind")
            }
            PantrixText(
                text = reading.headline,
                style = PantrixTheme.typography.callout.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2
            )
            reading.detail?.let {
                PantrixText(
                    text = it,
                    style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                    color = PantrixTheme.colors.textSecondary
                )
            }
            ValueActions(
                onCopy = { onCopy(node.value ?: document.text.substring(node.range.start, node.range.end)) },
                modifier = Modifier.padding(top = spacing.xs)
            )
        }
    }
}

/** The chosen value's actions: showing it in the text, which waits for the editor, and copying it. */
@Composable
private fun ValueActions(onCopy: () -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)
    ) {
        PantrixButton(
            text = stringResource(Res.string.workspace_show_in_text),
            onClick = {},
            variant = ButtonVariant.Soft,
            enabled = false,
            tagKey = "show_in_text"
        )
        PantrixButton(
            text = stringResource(Res.string.workspace_copy_value),
            onClick = onCopy,
            variant = ButtonVariant.Text,
            leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Copy, contentDescription = null) },
            tagKey = "copy_value"
        )
    }
}
