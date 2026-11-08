package com.developersancho.formatico.feature.workspace.editor

import androidx.compose.runtime.Composable
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
import com.developersancho.formatico.core.format.Severity
import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.feature.workspace.OpenDocument
import com.developersancho.formatico.feature.workspace.ValueNode
import com.developersancho.formatico.feature.workspace.nodeAt
import com.developersancho.formatico.feature.workspace.problemMessage
import com.pantrixui.components.data.codeblock.CodeMark
import com.pantrixui.components.data.codeblock.CodeTone
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.codeeditor.CodeEditorStyle
import com.pantrixui.components.data.codeeditor.CodeRangeMark
import com.pantrixui.components.data.codeeditor.PantrixCodeEditor
import com.pantrixui.foundation.token.shape.ShapeSpec
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_text_of
import formatico.feature.workspace.generated.resources.workspace_untitled
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource
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
 * value of [root] the caret comes to rest in is chosen in the tree ([onSelect]).
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
    problems: List<Problem> = emptyList()
) {
    val language = editorLanguage(document.format)
    // The text the editor last wrote to the document, or took from it: a document's text that is another came from
    // elsewhere.
    var known by remember(editor) { mutableStateOf(document.text) }
    val write by rememberUpdatedState(onWrite)
    LaunchedEffect(editor) {
        snapshotFlow { editor.revision }.drop(1).collectLatest {
            delay(WriteAfter)
            val text = editor.text
            known = text
            write(text)
        }
    }
    LaunchedEffect(editor, document.text) {
        if (document.text != known) {
            known = document.text
            if (editor.text != document.text) editor.setText(document.text)
        }
    }
    FollowTheTree(editor = editor, root = root, selected = selected, onSelect = onSelect)
    val name = document.name ?: stringResource(Res.string.workspace_untitled)
    PantrixCodeEditor(
        state = editor,
        modifier = modifier,
        highlighter = language.highlighter,
        rangeMarks = problemMarks(problems, document),
        syntax = language.syntax,
        indent = editorIndent(indentation),
        style = PaneEditorStyle,
        colors = editorColors(),
        contentDescription = stringResource(Res.string.workspace_text_of, name),
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
 * The [problems] as marks on the text: a wavy line under each, in the colour of an error or a warning, a dot before its
 * line, and what to do about it while the caret is there. A problem at a place, with no text of its own, marks the
 * character there.
 */
@Composable
private fun problemMarks(problems: List<Problem>, document: OpenDocument): List<CodeRangeMark> {
    val format = document.format ?: return emptyList()
    return problems.map { problem ->
        val end = if (problem.range.end > problem.range.start) problem.range.end else minOf(problem.range.start + 1, document.text.length)
        CodeRangeMark(
            start = problem.range.start,
            end = end,
            mark = CodeMark(
                tone = if (problem.severity == Severity.Error) CodeTone.Danger else CodeTone.Warning,
                message = problemMessage(problem, document.text, format)
            )
        )
    }
}
