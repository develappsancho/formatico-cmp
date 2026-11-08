package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.Repairs
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.container.alert.AlertTone
import com.pantrixui.components.container.alert.PantrixAlert
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.testing.pantrixTestTag
import com.pantrixui.foundation.testing.rememberPantrixTestTag
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * The problems' pane in a wide window or on a tablet, as the Invalid boards draw it: the views' choice, what is wrong
 * with the document and what repairs it at once, and each problem with its place and its own repair. A press on a
 * problem goes to it in [editor]'s text; the one the caret is in stands out.
 */
@Composable
internal fun ProblemsPane(
    state: WorkspaceState,
    editor: CodeEditorState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(if (wide) 48.dp else 52.dp).padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewChoice(
                selected = DocumentPane.Problems,
                withText = false,
                onSelect = { onIntent(WorkspaceIntent.ShowPane(it)) },
                problems = state.problems.size
            )
        }
        ProblemsList(state = state, editor = editor, layout = layout, onIntent = onIntent, modifier = Modifier.weight(1f))
    }
}

/** What is wrong, what repairs it all, and the problems one under another: the pane's own, and a phone's. */
@Composable
internal fun ProblemsList(
    state: WorkspaceState,
    editor: CodeEditorState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PantrixTheme.spacing
    val document = state.document ?: return
    val format = document.format ?: return
    val caret = editor.selection.min
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = spacing.sm)) {
        item(key = "summary") {
            ProblemsSummary(state = state, format = format, layout = layout, onIntent = onIntent)
        }
        itemsIndexed(state.problems, key = { index, _ -> index }) { index, problem ->
            ProblemRow(
                problem = problem,
                text = document.text,
                format = format,
                current = caret in problem.range.start..problem.range.end,
                layout = layout,
                onGoTo = {
                    editor.selection = TextRange(problem.range.start, problem.range.end)
                    // A phone shows the list in the text's place: going to a problem shows the text.
                    if (layout == WorkspaceLayout.Phone) onIntent(WorkspaceIntent.ShowPane(DocumentPane.Text))
                },
                onRepair = { onIntent(WorkspaceIntent.Repair(problem)) },
                tagKey = "problem_$index",
                repairTagKey = "repair_$index"
            )
        }
    }
}

/** That the document is not valid, what Repair all does, and where JSON5 would allow some of it, reading it so. */
@Composable
private fun ProblemsSummary(state: WorkspaceState, format: Format, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit) {
    val spacing = PantrixTheme.spacing
    val errors = state.problems.count { it.severity == Severity.Error }
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = spacing.xs, end = spacing.xs, top = spacing.md, bottom = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        if (errors > 0) {
            PantrixAlert(
                message = workspaceStrings.invalidBody(state.repairable, state.repairable),
                tone = AlertTone.Danger,
                title = workspaceStrings.invalidTitle(format.displayName),
                announce = false,
                tagKey = "invalid"
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            PantrixButton(
                text = workspaceStrings.repairAll,
                onClick = { onIntent(WorkspaceIntent.RepairAll) },
                size = layout.buttons,
                enabled = state.repairable > 0,
                tagKey = "repair_all"
            )
            if (state.json5Allows > 0) {
                PantrixButton(
                    text = workspaceStrings.readAsJson5,
                    onClick = { onIntent(WorkspaceIntent.ChangeFormat(Format.Json5)) },
                    variant = ButtonVariant.Outline,
                    size = layout.buttons,
                    tagKey = "read_as_json5"
                )
                PantrixText(
                    text = workspaceStrings.json5Allows(state.json5Allows, state.json5Allows, errors),
                    style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                    color = PantrixTheme.colors.textSecondary
                )
            }
        }
    }
}

/**
 * A problem: its sign, what to do about it and where, a press on which goes to it ([onGoTo]), and its repair's button
 * where it has one. The [current] one, which the caret is in, stands on the brand's soft fill. The row is tagged
 * `ProblemRow_<tagKey>`, its button `Button_<repairTagKey>`.
 */
@Composable
private fun ProblemRow(
    problem: Problem,
    text: String,
    format: Format,
    current: Boolean,
    layout: WorkspaceLayout,
    onGoTo: () -> Unit,
    onRepair: () -> Unit,
    tagKey: String,
    repairTagKey: String
) {
    val spacing = PantrixTheme.spacing
    val colors = PantrixTheme.colors
    val ink = if (current) colors.primary.onContainer else colors.textPrimary
    val quiet = if (current) colors.primary.onContainer else colors.textSecondary
    val repair = Repairs.of(problem, text, format)
    val error = problem.severity == Severity.Error
    // A phone has no room for the button beside the words: it goes under them.
    val phone = layout == WorkspaceLayout.Phone
    val button: @Composable () -> Unit = {
        if (repair != null) {
            PantrixButton(
                text = repairLabel(repair.kind),
                onClick = onRepair,
                variant = ButtonVariant.Soft,
                size = layout.buttons,
                tagKey = repairTagKey
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (current) colors.primary.container else colors.surface, PantrixTheme.shapes.sm)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        PantrixIcon(
            imageVector = if (error) PantrixIcons.Error else PantrixIcons.Warning,
            contentDescription = null,
            tint = if (error) colors.danger.color else colors.warning.color
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onGoTo)
                .pantrixTestTag(rememberPantrixTestTag("ProblemRow", tagKey)),
            verticalArrangement = Arrangement.spacedBy(spacing.none)
        ) {
            PantrixText(
                text = problemMessage(problem, text, format),
                style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium),
                color = ink
            )
            PantrixText(
                text = workspaceStrings.problemWhere(problem.start.line, problem.start.column),
                style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                color = quiet
            )
            if (phone) button()
        }
        if (!phone) button()
    }
}
