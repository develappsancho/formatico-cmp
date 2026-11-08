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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.feature.workspace.editor.DocumentEditor
import com.pantrixui.components.container.card.CardVariant
import com.pantrixui.components.container.card.PantrixCard
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_copy_document
import formatico.feature.workspace.generated.resources.workspace_copy_value
import formatico.feature.workspace.generated.resources.workspace_format
import formatico.feature.workspace.generated.resources.workspace_go_to_anchor
import formatico.feature.workspace.generated.resources.workspace_minify
import formatico.feature.workspace.generated.resources.workspace_repair
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
    editor: CodeEditorState,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val spacing = PantrixTheme.spacing
    val pane = document.view.pane
    Column(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.surface)) {
        // A phone has room for three views: an invalid document's problems take the table's place, where it has none.
        ViewChoice(
            selected = pane,
            withText = true,
            onSelect = { onIntent(WorkspaceIntent.ShowPane(it)) },
            problems = state.problems.size,
            withTable = state.problems.isEmpty() || state.table != null,
            withQuery = pane == DocumentPane.Query,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.md, bottom = spacing.sm)
        )
        // The query's board gives it the room under the views, and its own actions at the bottom.
        if (pane == DocumentPane.Query) {
            PhoneQuery(state = state, onIntent = onIntent, commands = commands, modifier = Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        // The table's board and the Summary's give them the room the tree's card and the actions take.
        if (pane == DocumentPane.Table || pane == DocumentPane.Summary) {
            val room = Modifier.weight(1f).fillMaxWidth()
            if (pane == DocumentPane.Table) {
                TablePane(state, WorkspaceLayout.Phone, onIntent, commands, room)
            } else {
                SummaryContent(state, WorkspaceLayout.Phone, onIntent, room.background(PantrixTheme.colors.background))
            }
            return@Column
        }
        val problemsInText = pane == DocumentPane.Text && state.problems.any { it.severity == Severity.Error }
        val search = rememberTreeSearch(state, onIntent)
        UnderTheViews(state, pane, problemsInText, search, onIntent)
        PantrixHorizontalDivider()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PhonePane(state = state, editor = editor, pane = pane, search = search, onIntent = onIntent)
        }
        if (problemsInText) {
            ProblemCard(state, editor, onIntent, Modifier.fillMaxWidth().padding(start = spacing.sm, end = spacing.sm, top = spacing.sm))
        }
        val selected = state.selected
        if (pane == DocumentPane.Tree && selected != null) {
            SelectedValueCard(
                node = selected,
                document = document,
                onShowInText = { onIntent(WorkspaceIntent.ShowPane(DocumentPane.Text)) },
                onCopy = { commands.copyText(Copied.Value, it) },
                onCopyPath = { commands.copyText(Copied.Path, it) },
                onGoTo = { onIntent(WorkspaceIntent.Select(it)) },
                modifier = Modifier.fillMaxWidth().padding(start = spacing.sm, end = spacing.sm, top = spacing.sm),
                match = search.takeIf { it.open }?.matchOf(selected)
            )
        }
        PhoneToolbar(state = state, document = document, onIntent = onIntent, commands = commands)
    }
}

/**
 * The row under the views: an invalid document's text has the strip of the Invalid board, and its problem under it;
 * the tree being searched has the search's bar; and otherwise the trail to the chosen value and whether it is valid.
 */
@Composable
private fun UnderTheViews(
    state: WorkspaceState,
    pane: DocumentPane,
    problemsInText: Boolean,
    search: TreeSearch,
    onIntent: (WorkspaceIntent) -> Unit
) {
    val spacing = PantrixTheme.spacing
    when {
        problemsInText -> {
            InvalidStrip(state, onIntent, Modifier.fillMaxWidth().height(44.dp).padding(start = spacing.lg, end = spacing.sm))
        }

        pane == DocumentPane.Tree && search.open && state.root != null -> {
            val focus = remember { FocusRequester() }
            TreeSearchBar(
                query = search.query,
                position = search.position(),
                layout = WorkspaceLayout.Phone,
                onStep = search::step,
                onClose = search::close,
                focus = focus
            )
            // The caret goes to the field as the bar is opened.
            LaunchedEffect(focus) { focus.requestFocus() }
        }

        else -> {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                PathTrail(state = state, onIntent = onIntent, modifier = Modifier.weight(1f))
                StatusTag(state)
            }
        }
    }
}

/** The pane the phone shows: the editor's place, the tree with what [search] found marked, or what stands in for it. */
@Composable
private fun PhonePane(
    state: WorkspaceState,
    editor: CodeEditorState,
    pane: DocumentPane,
    search: TreeSearch,
    onIntent: (WorkspaceIntent) -> Unit
) {
    val spacing = PantrixTheme.spacing
    val document = state.document
    if (pane == DocumentPane.Text && document != null) {
        DocumentEditor(
            document = document,
            editor = editor,
            indentation = state.stats?.layout?.indentation,
            root = state.root,
            selected = state.selected,
            onWrite = { onIntent(WorkspaceIntent.Write(it)) },
            onSelect = { onIntent(WorkspaceIntent.Select(it)) },
            modifier = Modifier.fillMaxSize(),
            problems = state.problems
        )
        return
    }
    if (pane == DocumentPane.Problems && state.problems.isNotEmpty()) {
        ProblemsList(state = state, editor = editor, layout = WorkspaceLayout.Phone, onIntent = onIntent)
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
        modifier = Modifier.fillMaxSize().padding(horizontal = spacing.sm, vertical = spacing.xs),
        marks = search.marks()
    )
}

/** Repair where there is something to repair, Format, Minify, Copy and Save as a file, along the bottom. */
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
        if (state.repairable > 0) {
            PantrixButton(
                text = stringResource(Res.string.workspace_repair),
                onClick = { onIntent(WorkspaceIntent.RepairAll) },
                size = ButtonSize.Large,
                tagKey = "repair"
            )
        }
        // Where Repair leads, Format stands back beside it and Minify leaves the row, as the Invalid board draws it.
        val repairing = state.repairable > 0
        PantrixButton(
            text = stringResource(Res.string.workspace_format),
            onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.Format)) },
            variant = if (repairing) ButtonVariant.Outline else ButtonVariant.Solid,
            size = ButtonSize.Large,
            enabled = DocumentEdit.Format in state.edits,
            tagKey = "format"
        )
        if (!repairing) {
            PantrixButton(
                text = stringResource(Res.string.workspace_minify),
                onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.Minify)) },
                variant = ButtonVariant.Outline,
                size = ButtonSize.Large,
                enabled = DocumentEdit.Minify in state.edits,
                tagKey = "minify"
            )
        }
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
 * The chosen value, read out: its path and its kind, the value itself, what more there is to say of it, which [match]
 * of a search it is, and its
 * actions: copying its path ([onCopyPath]), showing it in the text ([onShowInText]), copying it, going to an alias's
 * anchor.
 */
@Composable
private fun SelectedValueCard(
    node: ValueNode,
    document: OpenDocument,
    onShowInText: () -> Unit,
    onCopy: (String) -> Unit,
    onCopyPath: (String) -> Unit,
    onGoTo: (String) -> Unit,
    modifier: Modifier = Modifier,
    match: String? = null
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A long path is cut short, so that its kind and the copying of it keep their places.
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    PantrixText(
                        text = jsonPath(node.path),
                        modifier = Modifier.weight(1f, fill = false),
                        style = PantrixTheme.typography.code.copy(
                            fontSize = PantrixTheme.typography.footnote.fontSize,
                            lineHeight = PantrixTheme.typography.footnote.lineHeight
                        ),
                        color = PantrixTheme.colors.textSecondary,
                        maxLines = 1
                    )
                    PantrixTag(text = reading.kind, icon = reading.icon, tagKey = "kind")
                }
                // The button stands into the card's padding, as the board draws it, and the row keeps its words' height.
                CopyPathMenu(
                    node = node,
                    layout = WorkspaceLayout.Phone,
                    onCopy = onCopyPath,
                    modifier = Modifier.outset(end = spacing.sm, vertical = spacing.sm)
                )
            }
            PantrixText(
                text = reading.headline,
                style = PantrixTheme.typography.callout.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2
            )
            listOfNotNull(reading.detail, match).joinToString(" · ").ifEmpty { null }?.let {
                PantrixText(
                    text = it,
                    style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                    color = PantrixTheme.colors.textSecondary
                )
            }
            ValueActions(
                target = node.target?.key,
                onShowInText = onShowInText,
                onCopy = { onCopy(node.value ?: document.text.substring(node.range.start, node.range.end)) },
                onGoTo = onGoTo,
                modifier = Modifier.padding(top = spacing.xs)
            )
        }
    }
}

/**
 * The chosen value's actions: showing it in the text ([onShowInText]), where it is selected, and copying it, or for an
 * alias, going to the value its anchor is given, whose key is [target].
 */
@Composable
private fun ValueActions(
    target: String?,
    onShowInText: () -> Unit,
    onCopy: () -> Unit,
    onGoTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)
    ) {
        PantrixButton(
            text = stringResource(Res.string.workspace_show_in_text),
            onClick = onShowInText,
            variant = ButtonVariant.Soft,
            tagKey = "show_in_text"
        )
        if (target != null) {
            PantrixButton(
                text = stringResource(Res.string.workspace_go_to_anchor),
                onClick = { onGoTo(target) },
                variant = ButtonVariant.Text,
                leadingIcon = { PantrixIcon(imageVector = PantrixIcons.ArrowForward, contentDescription = null) },
                tagKey = "go_to_anchor"
            )
        } else {
            PantrixButton(
                text = stringResource(Res.string.workspace_copy_value),
                onClick = onCopy,
                variant = ButtonVariant.Text,
                leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Copy, contentDescription = null) },
                tagKey = "copy_value"
            )
        }
    }
}

/** Lays this out [end] and [vertical] past the room it is given, as a negative margin does: what is around it keeps its size. */
private fun Modifier.outset(end: Dp, vertical: Dp): Modifier = layout { measurable, constraints ->
    val endPx = end.roundToPx()
    val verticalPx = vertical.roundToPx()
    val placeable = measurable.measure(constraints)
    layout((placeable.width - endPx).coerceAtLeast(0), (placeable.height - 2 * verticalPx).coerceAtLeast(0)) {
        placeable.place(0, -verticalPx)
    }
}

/** The document's "more" menu, as the app bar's has it, where a screen of the document's own stands in that bar's place. */
@Composable
internal fun PhoneMoreMenu(state: WorkspaceState, document: OpenDocument, onIntent: (WorkspaceIntent) -> Unit) {
    DocumentMenu(
        items = DocumentMenuItems.FindSummarySortCompareClose,
        size = ButtonSize.Large,
        read = document.format != null,
        onFind = {
            onIntent(WorkspaceIntent.ShowPane(DocumentPane.Tree))
            onIntent(WorkspaceIntent.Search(open = true))
        },
        onSummary = { onIntent(WorkspaceIntent.ShowPane(DocumentPane.Summary)) },
        onQuery = { onIntent(WorkspaceIntent.ShowPane(DocumentPane.Query)) },
        onConvert = { onIntent(WorkspaceIntent.ShowPane(DocumentPane.Convert)) },
        canSort = DocumentEdit.SortKeys in state.edits,
        onSortKeys = { onIntent(WorkspaceIntent.Edit(DocumentEdit.SortKeys)) },
        onSave = {},
        onClose = { onIntent(WorkspaceIntent.Close) }
    )
}
