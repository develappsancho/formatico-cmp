package com.developersancho.formatico.feature.workspace.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Problem
import com.developersancho.formatico.core.format.Repairs
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.feature.workspace.OpenDocument
import com.developersancho.formatico.feature.workspace.SchemaIssue
import com.developersancho.formatico.feature.workspace.ValueNode
import com.developersancho.formatico.feature.workspace.nodeAt
import com.developersancho.formatico.feature.workspace.problemMessage
import com.developersancho.formatico.feature.workspace.repairLabel
import com.developersancho.formatico.feature.workspace.sentenceOf
import com.developersancho.formatico.feature.workspace.strings.schemaStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.codeblock.CodeMark
import com.pantrixui.components.data.codeblock.CodeMarkAction
import com.pantrixui.components.data.codeblock.CodeTone
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.codeeditor.CodeEditorStyle
import com.pantrixui.components.data.codeeditor.CodeRangeMark
import com.pantrixui.components.data.codeeditor.CodeRangeStyle
import com.pantrixui.components.data.codeeditor.PantrixCodeEditor
import com.pantrixui.foundation.token.shape.ShapeSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlin.time.Duration.Companion.milliseconds

/** How long the typing pauses before what was typed reaches the document, which is read again from it. */
private val WriteAfter = 300.milliseconds

/** How long the caret rests before the value it stands in is chosen in the tree. */
private val ChooseAfter = 150.milliseconds

/**
 * The editor as a pane: square, with no edge of its own, the pane's dividers being its edges, and its lines as the
 * boards draw them, 22 high.
 */
internal val PaneEditorStyle = CodeEditorStyle(
    shape = ShapeSpec.Fixed(RectangleShape),
    borderWidth = 0.dp,
    focusedBorderWidth = 0.dp,
    linePadding = 1.dp
)

/**
 * [document]'s text in [editor], coloured as its format is and indented by [indentation], kept with the document both
 * ways: what is typed reaches it once the typing pauses ([onWrite]), and a change made to it elsewhere, Format, Minify
 * or Sort keys, comes into the editor as a step of its own to undo.
 *
 * The text and the tree keep their choice in step: the value [selected] in the tree is selected in the text, and the
 * value of [root] the caret comes to rest in is chosen in the tree ([onSelect]). Its [problems] are marked, the
 * [schemaIssues] of a schema it is validated against, and the [matches] of the query asked of it, the one at [match]
 * framed. Where [onRepair] is given, a problem's note offers its repair and the next problem, as the Invalid boards' does;
 * a phone's card offers them in its place.
 */
@Composable
internal fun DocumentEditor(
    document: OpenDocument,
    editor: CodeEditorState,
    indentation: Indentation?,
    root: ValueNode?,
    selected: ValueNode?,
    onWrite: (String) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    problems: List<Problem> = emptyList(),
    schemaIssues: List<SchemaIssue> = emptyList(),
    matches: List<TextRange> = emptyList(),
    match: Int? = null,
    onRepair: ((Problem) -> Unit)? = null
) {
    val language = editorLanguage(document.format, document.firstRowHeader)
    // The text the editor last wrote to the document, or took from it: a document's text that is another came from
    // elsewhere. The editor outlives this, as a phone shows its tree in the text's place: what it holds is what it
    // knows, and a change made to the document meanwhile, a Minify run from the tree, comes into it.
    var known by remember(editor) { mutableStateOf(editor.text) }
    val write by rememberUpdatedState(onWrite)
    LaunchedEffect(editor) {
        snapshotFlow { editor.revision }.drop(1).collectLatest {
            delay(WriteAfter)
            val text = editor.text
            known = text
            write(text)
        }
    }
    // What was typed and not written yet, as the text leaves the screen before the typing pauses, is written then.
    DisposableEffect(editor) {
        onDispose {
            val text = editor.text
            if (text != known) write(text)
        }
    }
    LaunchedEffect(editor, document.text) {
        if (document.text != known) {
            known = document.text
            if (editor.text != document.text) editor.setText(document.text)
        }
    }
    FollowTheTree(editor = editor, root = root, selected = selected, onSelect = onSelect)
    val name = document.name ?: workspaceStrings.untitled
    PantrixCodeEditor(
        state = editor,
        modifier = modifier,
        highlighter = language.highlighter,
        rangeMarks = matchMarks(matches, match) + problemMarks(problems, document, editor, onRepair) + schemaMarks(schemaIssues),
        syntax = language.syntax,
        indent = editorIndent(indentation),
        style = PaneEditorStyle,
        colors = editorColors(),
        contentDescription = workspaceStrings.textOf(name),
        softWrap = LocalWrapLines.current,
        tagKey = "text"
    )
}

/**
 * Keeps [editor]'s selection and the tree's choice in step: the value [selected] in the tree is selected in the text,
 * and the value of [root] the caret rests in, once it was put there and not moved by typing, is chosen ([onSelect]).
 */
@Composable
private fun FollowTheTree(editor: CodeEditorState, root: ValueNode?, selected: ValueNode?, onSelect: (String) -> Unit) {
    val tree by rememberUpdatedState(root)
    val chosen by rememberUpdatedState(selected)
    val select by rememberUpdatedState(onSelect)
    // What the text chose, which the tree is not to select back in it.
    var fromText by remember(editor) { mutableStateOf<String?>(null) }
    LaunchedEffect(editor, selected?.key) {
        val node = selected ?: return@LaunchedEffect
        if (node.key == fromText) return@LaunchedEffect
        editor.selection = TextRange(node.range.start, node.range.end)
    }
    LaunchedEffect(editor) {
        var revision = editor.revision
        snapshotFlow { editor.selection to editor.revision }.drop(1).collectLatest { (selection, changed) ->
            // A caret moved by typing stands in a text the tree has not been read from yet.
            val typed = changed != revision
            revision = changed
            if (typed) return@collectLatest
            delay(ChooseAfter)
            val node = nodeAt(tree, selection.min) ?: return@collectLatest
            val current = chosen
            val asSelected = current != null && selection == TextRange(current.range.start, current.range.end)
            if (node.key != current?.key && !asSelected) {
                fromText = node.key
                select(node.key)
            }
        }
    }
}

/**
 * A query's [matches] as marks on the text: a fill behind each in the warning's tone, with no dot and no words, and a
 * frame round the one at [current] as well, the one chosen, as the Query boards mark them.
 */
internal fun matchMarks(matches: List<TextRange>, current: Int? = null): List<CodeRangeMark> =
    matches.mapIndexed { index, match ->
        CodeRangeMark(
            start = match.start,
            end = maxOf(match.end, match.start + 1),
            mark = CodeMark(tone = CodeTone.Warning),
            style = if (index == current) CodeRangeStyle.Current else CodeRangeStyle.Highlight
        )
    }

/** The [issues] against a schema as marks on the text: a wavy line under each, in an error's colour, and what is wrong. */
@Composable
private fun schemaMarks(issues: List<SchemaIssue>): List<CodeRangeMark> {
    if (issues.isEmpty()) return emptyList()
    val strings = schemaStrings
    return issues.mapNotNull { issue ->
        val range = issue.range ?: return@mapNotNull null
        CodeRangeMark(
            start = range.start,
            end = maxOf(range.end, range.start + 1),
            mark = CodeMark(tone = CodeTone.Danger, message = strings.sentenceOf(issue.problem))
        )
    }
}

/**
 * The [problems] as marks on the text: a wavy line under each, in the colour of an error or a warning, a dot before its
 * line, and what to do about it while the caret is there. A problem at a place, with no text of its own, marks the
 * character there. Where [onRepair] is given, the note offers the problem's repair, where it has one, and the next
 * problem, which [editor] goes to.
 */
@Composable
private fun problemMarks(
    problems: List<Problem>,
    document: OpenDocument,
    editor: CodeEditorState,
    onRepair: ((Problem) -> Unit)?
): List<CodeRangeMark> {
    val format = document.format ?: return emptyList()
    val next = workspaceStrings.nextProblem
    return problems.mapIndexed { index, problem ->
        val end = if (problem.range.end > problem.range.start) problem.range.end else minOf(problem.range.start + 1, document.text.length)
        val actions = if (onRepair == null) {
            emptyList()
        } else {
            listOfNotNull(
                Repairs.of(problem, document.text, format)?.let { repair ->
                    CodeMarkAction(repairLabel(repair.kind)) { onRepair(problem) }
                },
                problems.getOrNull(index + 1)?.let { after ->
                    CodeMarkAction(next) { editor.selection = TextRange(after.range.start, after.range.end) }
                }
            )
        }
        CodeRangeMark(
            start = problem.range.start,
            end = end,
            mark = CodeMark(
                tone = if (problem.severity == Severity.Error) CodeTone.Danger else CodeTone.Warning,
                message = problemMessage(problem, document.text, format),
                actions = actions
            )
        )
    }
}
