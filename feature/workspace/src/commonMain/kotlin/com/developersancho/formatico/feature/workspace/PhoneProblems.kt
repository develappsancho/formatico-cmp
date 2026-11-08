package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.Repairs
import com.developersancho.formatico.core.format.Severity
import com.pantrixui.components.container.card.CardVariant
import com.pantrixui.components.container.card.PantrixCard
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.data.tag.TagTone
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_next_problem
import formatico.feature.workspace.generated.resources.workspace_not_valid_count
import formatico.feature.workspace.generated.resources.workspace_previous_problem
import formatico.feature.workspace.generated.resources.workspace_problem_error
import formatico.feature.workspace.generated.resources.workspace_problem_of
import formatico.feature.workspace.generated.resources.workspace_problem_warning
import formatico.feature.workspace.generated.resources.workspace_repair_all
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The strip a phone shows over an invalid document's text, in the trail's place: that it is not valid and how many
 * problems it has, and what repairs them all at once.
 */
@Composable
internal fun InvalidStrip(state: WorkspaceState, onIntent: (WorkspaceIntent) -> Unit, modifier: Modifier = Modifier) {
    val format = state.document?.format ?: return
    val problems = state.problems.size
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)
    ) {
        PantrixTag(
            text = pluralStringResource(Res.plurals.workspace_not_valid_count, problems, problems, format.displayName),
            tone = TagTone.Danger,
            dot = true,
            tagKey = "invalid"
        )
        Spacer(modifier = Modifier.weight(1f))
        PantrixButton(
            text = stringResource(Res.string.workspace_repair_all),
            onClick = { onIntent(WorkspaceIntent.RepairAll) },
            variant = ButtonVariant.Text,
            size = ButtonSize.Medium,
            enabled = state.repairable > 0,
            tagKey = "repair_all"
        )
    }
}

/**
 * The problem a phone's text is at, under the text: which of how many it is and where, the ones before and after it
 * a press away, what to do about it, and its repair where it has one. The problem is the one [editor]'s caret is in, or
 * the next after it; going to another selects it in the text.
 */
@Composable
internal fun ProblemCard(
    state: WorkspaceState,
    editor: CodeEditorState,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PantrixTheme.spacing
    val document = state.document ?: return
    val format = document.format ?: return
    val problems = state.problems
    if (problems.isEmpty()) return
    val index = currentProblem(problems, editor.selection.min)
    val problem = problems[index]
    val repair = Repairs.of(problem, document.text)
    val goTo = { to: Int -> editor.selection = TextRange(problems[to].range.start, problems[to].range.end) }
    PantrixCard(
        modifier = modifier,
        variant = CardVariant.Elevated,
        contentPadding = PaddingValues(start = spacing.lg, end = spacing.sm, top = spacing.sm, bottom = spacing.md),
        tagKey = "problem"
    ) {
        // The board sets the card's lines 6 apart, between two steps of the scale.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProblemCardHeader(problem = problem, index = index, count = problems.size, onGoTo = goTo)
            PantrixText(text = problemMessage(problem, document.text, format), style = PantrixTheme.typography.body)
            if (repair != null) {
                PantrixButton(
                    text = repairLabel(repair.kind),
                    onClick = { onIntent(WorkspaceIntent.Repair(problem)) },
                    variant = ButtonVariant.Soft,
                    size = ButtonSize.Medium,
                    modifier = Modifier.padding(top = spacing.xs),
                    tagKey = "repair_problem"
                )
            }
        }
    }
}

/** Which problem of [count] the card's is, from [index], and where; and the ones before and after it, which [onGoTo] goes to. */
@Composable
private fun ProblemCardHeader(problem: Problem, index: Int, count: Int, onGoTo: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)) {
        ProblemSign(problem)
        PantrixText(
            text = pluralStringResource(
                Res.plurals.workspace_problem_of,
                count,
                index + 1,
                count,
                problem.start.line,
                problem.start.column
            ),
            style = PantrixTheme.typography.footnote,
            color = PantrixTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        PantrixIconButton(
            imageVector = PantrixIcons.ChevronBack,
            contentDescription = stringResource(Res.string.workspace_previous_problem),
            onClick = { onGoTo(index - 1) },
            enabled = index > 0,
            size = ButtonSize.Medium,
            tagKey = "previous_problem"
        )
        PantrixIconButton(
            imageVector = PantrixIcons.ChevronForward,
            contentDescription = stringResource(Res.string.workspace_next_problem),
            onClick = { onGoTo(index + 1) },
            enabled = index < count - 1,
            size = ButtonSize.Medium,
            tagKey = "next_problem"
        )
    }
}

/** An error's sign, or a warning's, which says which it is. */
@Composable
private fun ProblemSign(problem: Problem) {
    val error = problem.severity == Severity.Error
    PantrixIcon(
        imageVector = if (error) PantrixIcons.Error else PantrixIcons.Warning,
        contentDescription = stringResource(if (error) Res.string.workspace_problem_error else Res.string.workspace_problem_warning),
        tint = if (error) PantrixTheme.colors.danger.color else PantrixTheme.colors.warning.color
    )
}

/**
 * Which of [problems], in the order of the text, the place [caret] is at: the one it is in, or the first after it, or
 * the last where it is past them all.
 */
internal fun currentProblem(problems: List<Problem>, caret: Int): Int =
    problems.indexOfFirst { caret <= it.range.end }.takeIf { it >= 0 } ?: problems.lastIndex
