package com.developersancho.formatico.feature.workspace.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import com.developersancho.formatico.core.format.Format
import com.pantrixui.components.data.codeblock.CodeMark
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.codeeditor.PantrixCodeEditor
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_text_of
import org.jetbrains.compose.resources.stringResource

/**
 * A compared document's [text], named [name], to be read and copied but not changed: coloured as [format] is, the
 * lines of its differences on the bands of [marks], by their places counted from nought, and the lines of [shown], the
 * difference shown, selected, which the editor brings into view; where the difference has no line here, the caret
 * stands at the start of the line it would follow.
 *
 * The difference shown is selected rather than banded in the primary colour, as the Compare boards draw it: the editor
 * bands the caret's line over any mark of its own, and brings a line into view only by moving the caret there
 * (PANTRIX_FINDINGS, "Kod editöründe imlecin satırı, satırın işaretini örtüyor").
 */
@Composable
internal fun ComparedText(
    text: String,
    format: Format?,
    name: String,
    marks: Map<Int, CodeMark>,
    shown: IntRange?,
    tagKey: String,
    modifier: Modifier = Modifier
) {
    val language = editorLanguage(format)
    val editor = remember(text) { CodeEditorState(text) }
    LaunchedEffect(editor, shown) {
        val lines = shown ?: return@LaunchedEffect
        val last = editor.lineCount - 1
        val start = editor.lineStart(lines.first.coerceIn(0, last))
        editor.selection = if (lines.isEmpty()) {
            TextRange(start)
        } else {
            val end = lines.last.coerceIn(0, last)
            TextRange(start, editor.lineStart(end) + editor.line(end).length)
        }
    }
    PantrixCodeEditor(
        state = editor,
        modifier = modifier,
        highlighter = language.highlighter,
        syntax = language.syntax,
        lineMarks = marks,
        readOnly = true,
        style = PaneEditorStyle,
        colors = editorColors(),
        contentDescription = stringResource(Res.string.workspace_text_of, name),
        tagKey = tagKey
    )
}
