package com.developersancho.formatico.feature.workspace.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.developersancho.formatico.core.format.Format
import com.pantrixui.components.data.codeblock.CodeMark
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.codeeditor.CodeRangeMark
import com.pantrixui.components.data.codeeditor.PantrixCodeEditor
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_text_of
import org.jetbrains.compose.resources.stringResource

/**
 * A compared document's [text], named [name], to be read and copied but not changed: coloured as [format] is, its lines
 * marked by [marks], by their places counted from nought, and the values that changed by [values]. The editor brings
 * the lines of [shown], the difference shown, into view, and leaves the caret where it is; where the difference has no
 * line here, it brings the line it would come before.
 */
@Composable
internal fun ComparedText(
    text: String,
    format: Format?,
    name: String,
    marks: Map<Int, CodeMark>,
    values: List<CodeRangeMark>,
    shown: IntRange?,
    tagKey: String,
    modifier: Modifier = Modifier
) {
    val language = editorLanguage(format)
    val editor = remember(text) { CodeEditorState(text) }
    LaunchedEffect(editor, shown) { shown?.let(editor::revealLines) }
    PantrixCodeEditor(
        state = editor,
        modifier = modifier,
        highlighter = language.highlighter,
        syntax = language.syntax,
        lineMarks = marks,
        rangeMarks = values,
        readOnly = true,
        style = PaneEditorStyle,
        colors = editorColors(),
        contentDescription = stringResource(Res.string.workspace_text_of, name),
        tagKey = tagKey
    )
}
