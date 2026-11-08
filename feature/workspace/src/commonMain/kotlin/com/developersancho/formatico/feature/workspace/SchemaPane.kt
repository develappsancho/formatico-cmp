package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import com.developersancho.formatico.feature.workspace.strings.schemaStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * A document validated against a schema in a wide window, beside its text, or on a tablet, in the whole room: the
 * schema chosen and the file to choose one from, what validating came to, and the problems, each a press away from its
 * place in the text. A wide window has the choice on a line of its own, as Schema-1440 draws it; a tablet says whose
 * schema it is and has its alert under the choice, as Schema-834 does.
 */
@Composable
internal fun SchemaPane(
    state: WorkspaceState,
    editor: CodeEditorState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    val check = state.schema
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    val name = document.name ?: workspaceStrings.untitled
    Column(modifier = modifier.fillMaxSize()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = if (wide) spacing.sm else spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            SchemaLabel(if (wide) schemaStrings.against else schemaStrings.validateAgainst(name))
            SchemaMenu(state, layout.buttons, onIntent)
            Spacer(modifier = Modifier.weight(1f))
            ChooseSchemaFile(layout.buttons, ButtonVariant.Text, onIntent)
        }
        if (wide) PantrixHorizontalDivider()
        if (check == null || !check.isShownInThePane) {
            SchemaMessage(state, Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        SchemaIssues(state, check, editor, layout, onIntent, Modifier.weight(1f))
    }
}

/** Whether the pane shows [this] check as an alert and a list: what the document is against a schema, or that none is chosen. */
internal val SchemaCheck.isShownInThePane: Boolean
    get() = this is SchemaCheck.Failed || this is SchemaCheck.Valid || this is SchemaCheck.NoSchema || this is SchemaCheck.Unusable

/**
 * What validating came to, as an alert, and the problems under it: a press on one selects its value in [editor]'s
 * text; on a tablet and a phone, which show the problems in the text's place, the text is shown there. The one the
 * caret is in stands out where the text is beside the list.
 */
@Composable
internal fun SchemaIssues(
    state: WorkspaceState,
    check: SchemaCheck,
    editor: CodeEditorState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val document = state.document ?: return
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    val caret = editor.selection.min
    val name = document.name ?: workspaceStrings.untitled
    val format = (state.reading as? Reading.Read)?.format?.displayName.orEmpty()
    val issues = check.issues.take(MOST_LISTED_ISSUES)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = spacing.sm, end = spacing.sm, bottom = spacing.sm)
    ) {
        item(key = "outcome") {
            SchemaAlert(
                check = check,
                documentName = name,
                format = format,
                modifier = Modifier.fillMaxWidth().padding(start = spacing.xs, end = spacing.xs, top = spacing.md, bottom = spacing.md)
            )
        }
        itemsIndexed(issues, key = { index, _ -> index }) { _, issue ->
            val range = issue.range
            SchemaIssueRow(
                issue = issue,
                current = wide && range != null && caret in range.start..range.end,
                onGoTo = {
                    if (range != null) editor.selection = TextRange(range.start, range.end)
                    if (!wide) onIntent(WorkspaceIntent.ShowPane(DocumentPane.Text))
                }
            )
        }
    }
}

/** Why the document is not validated: the tree's reason, the document's errors, which are repaired first, or its format. */
@Composable
internal fun SchemaMessage(state: WorkspaceState, modifier: Modifier = Modifier) {
    val strings = schemaStrings
    when (val check = state.schema) {
        is SchemaCheck.Invalid -> PaneMessage(
            icon = PantrixIcons.Warning,
            title = workspaceStrings.statusProblems(check.errors, check.errors),
            body = strings.invalid(check.errors, state.document?.name ?: workspaceStrings.untitled, check.errors),
            modifier = modifier
        )

        is SchemaCheck.Unsupported -> PaneMessage(
            icon = PantrixIcons.Info,
            title = strings.unsupportedTitle(check.format.displayName),
            body = strings.unsupported(check.format.displayName),
            modifier = modifier
        )

        else -> TreePaneMessage(state = state, modifier = modifier)
    }
}

/** A wide window's schema: the document's text, under its format, name and lines, and its validation beside it. */
@Composable
internal fun SchemaPanes(
    state: WorkspaceState,
    editor: CodeEditorState,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    BesideTextPanes(state, editor, onIntent, modifier) {
        SchemaPane(state = state, editor = editor, layout = WorkspaceLayout.Wide, onIntent = onIntent)
    }
}
